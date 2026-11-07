#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include "src/harfa/harfa_proto.h"

// --- Stałe mocka ---
static const uint8_t FW_MAJOR = 0, FW_MINOR = 1, FW_PATCH = 0, HW_REV = 0;
static const uint16_t CAPS = HARFA_CAP_LEDS | HARFA_CAP_BATTERY | HARFA_CAP_LESSON;
static const uint8_t SEMITONE[HARFA_LEVER_COUNT] = {0, 2, 4, 5, 7, 9, 11};  // C D E F G A B
static const char NOTE_NAMES[] = "CDEFGAB";

// --- BLE ---
static BLEServer* server = nullptr;
static BLECharacteristic* txChar = nullptr;
static uint32_t deviceId = 0;
static char deviceName[16];

static volatile bool connected = false;
static volatile bool connectEvent = false;
static volatile bool disconnectEvent = false;
static volatile uint16_t connId = 0;

struct RxFrame {
  uint16_t len;                   
  uint8_t data[HARFA_MAX_FRAME_SIZE]; 
};
static QueueHandle_t rxQueue;

// --- Stan sesji i instrumentu ---
static bool sessionActive = false;    
static uint8_t txSeq = 0; 
static bool haveLast = false;    
static uint8_t lastSeq = 0, lastType = 0, lastStatus = 0;

static harfa_config_state_t config = {0, {0, 0, 0, 0, 0, 0, 0}, 2};
static uint8_t batteryPercent = 80;
static uint16_t batteryMillivolts = 3900;
static uint8_t batteryFlags = 0;

static bool lessonActive = false;
static uint16_t lessonTotalSteps = 0;
static uint16_t ledMask = 0;
static uint8_t ledMode = HARFA_LED_MODE_STEADY;

// ===================== Wysyłanie =====================

static void printFrame(const char* dir, const uint8_t* buf, size_t len) {
  Serial.print(dir);
  for (size_t i = 0; i < len; i++) {
    Serial.printf(" %02X", buf[i]);
  }
  if (len >= 2) {
    const char* name = harfa_msg_type_name(buf[1]);
    Serial.printf("  (%s)", name ? name : "?");
  }
  Serial.println();
}

static void sendMsg(harfa_msg_t& msg) {
  if (!connected) return;
  msg.seq = txSeq++;
  uint8_t buf[HARFA_MAX_FRAME_SIZE];
  size_t n = harfa_encode(&msg, buf, sizeof(buf));
  if (n == 0) {
    Serial.println("! harfa_encode: niepoprawna wiadomosc");
    return;
  }
  txChar->setValue(buf, n);
  txChar->notify();
  printFrame("TX", buf, n);
  delay(5);  // odstęp między notyfikacjami, żeby stos BLE ich nie zgubił
}

static void sendAck(uint8_t ackSeq, uint8_t status) {
  harfa_msg_t m{};
  m.type = HARFA_MSG_ACK;
  m.body.ack.ack_seq = ackSeq;
  m.body.ack.status = status;
  sendMsg(m);
}

static void sendError(uint8_t code, uint8_t refSeq, uint8_t refType) {
  harfa_msg_t m{};
  m.type = HARFA_MSG_ERROR;
  m.body.error.code = code;
  m.body.error.ref_seq = refSeq;
  m.body.error.ref_type = refType;
  sendMsg(m);
}

static void sendDeviceInfo() {
  harfa_msg_t m{};
  m.type = HARFA_MSG_DEVICE_INFO;
  m.body.device_info.proto_major = HARFA_PROTO_VERSION_MAJOR;
  m.body.device_info.proto_minor = HARFA_PROTO_VERSION_MINOR; 
  m.body.device_info.fw_major = FW_MAJOR;
  m.body.device_info.fw_minor = FW_MINOR;
  m.body.device_info.fw_patch = FW_PATCH;
  m.body.device_info.hw_rev = HW_REV;
  m.body.device_info.string_count = HARFA_STRING_COUNT;
  m.body.device_info.caps = CAPS;
  m.body.device_info.device_id = deviceId;
  sendMsg(m);
}

static void sendConfigState() {
  harfa_msg_t m{};
  m.type = HARFA_MSG_CONFIG_STATE;
  m.body.config_state = config;
  sendMsg(m);
}

static void sendBatteryState() {
  harfa_msg_t m{};
  m.type = HARFA_MSG_BATTERY_STATE;
  m.body.battery_state.percent = batteryPercent;
  m.body.battery_state.millivolts = batteryMillivolts;
  m.body.battery_state.flags = batteryFlags;
  sendMsg(m);
}

static int effectiveMidi(int stringId) {
  int step = stringId % HARFA_LEVER_COUNT;
  int octave = config.base_octave + stringId / HARFA_LEVER_COUNT;
  return (octave + 1) * 12 + SEMITONE[step] + config.lever[step] + config.octave_shift * 12;
}

static void sendStringEvent(int stringId) {
  harfa_msg_t m{};
  m.type = HARFA_MSG_STRING_EVENT;
  m.body.string_event.string_id = (uint8_t)stringId;
  m.body.string_event.midi = (uint8_t)effectiveMidi(stringId);
  m.body.string_event.t_ms = (uint16_t)(millis() & 0xFFFF);
  sendMsg(m);
}

// ===================== Stan lekcji i diod =====================

static void printLeds() {
  Serial.print("LED: ");
  for (int i = 0; i < HARFA_STRING_COUNT; i++) {
    Serial.print((ledMask >> i) & 1 ? '#' : '.');
  }
  Serial.printf("  mask=0x%04X %s\n", ledMask, ledMode == HARFA_LED_MODE_BLINK ? "(miganie)" : "");
}

static void endLessonAndLedsOff() {
  lessonActive = false;
  lessonTotalSteps = 0;
  ledMask = 0;
}

// ===================== Obsługa ramek (spec §6.2) =====================

/** Wykonuje poprawnie zdekodowaną komendę. Zwraca status do ACK. */
static uint8_t executeCommand(const harfa_msg_t& msg, uint8_t& stateRequestWhat) {
  if (!sessionActive) return HARFA_STATUS_NOT_READY;

  switch (msg.type) {
    case HARFA_MSG_STATE_REQUEST:
      stateRequestWhat = msg.body.state_request.what;
      return HARFA_STATUS_OK;

    case HARFA_MSG_LED_SET:
      ledMask = msg.body.led_set.mask;
      ledMode = msg.body.led_set.mode;
      printLeds();
      return HARFA_STATUS_OK;

    case HARFA_MSG_LESSON_START:
      lessonActive = true;
      lessonTotalSteps = msg.body.lesson_start.total_steps;
      ledMask = 0;
      Serial.printf("Lekcja: start, krokow %u\n", lessonTotalSteps);
      return HARFA_STATUS_OK;

    case HARFA_MSG_LESSON_STEP:
      if (!lessonActive) return HARFA_STATUS_NOT_READY;
      if (msg.body.lesson_step.index >= lessonTotalSteps) return HARFA_STATUS_INVALID_ARGUMENT;
      ledMask = msg.body.lesson_step.mask;
      ledMode = HARFA_LED_MODE_STEADY;
      Serial.printf("Lekcja: krok %u\n", msg.body.lesson_step.index);
      printLeds();
      return HARFA_STATUS_OK;

    case HARFA_MSG_LESSON_RESULT: {
      if (!lessonActive) return HARFA_STATUS_NOT_READY;
      uint8_t r = msg.body.lesson_result.result;
      Serial.printf("Lekcja: wynik %s\n",
                    r == HARFA_LESSON_HIT ? "HIT" : r == HARFA_LESSON_MISS ? "MISS" : "COMPLETE");
      if (r == HARFA_LESSON_COMPLETE) {
        ledMask = 0;  // lekcja trwa do LESSON_END
        printLeds();
      }
      return HARFA_STATUS_OK;
    }

    case HARFA_MSG_LESSON_END:
      endLessonAndLedsOff();
      Serial.println("Lekcja: koniec");
      printLeds();
      return HARFA_STATUS_OK;

    default:
      return HARFA_STATUS_UNKNOWN_TYPE;
  }
}

static void handleHello(const harfa_msg_t& msg) {
  txSeq = 0;
  haveLast = false;
  endLessonAndLedsOff();
  sessionActive = true;
  Serial.printf("HELLO od aplikacji (v%u.%u) -> nowa sesja\n",
                msg.body.hello.proto_major, msg.body.hello.proto_minor);
  sendDeviceInfo();
  sendConfigState();
  sendBatteryState();
}

static void handleFrame(const RxFrame& f) {
  size_t len = f.len > HARFA_MAX_FRAME_SIZE ? HARFA_MAX_FRAME_SIZE : f.len;
  printFrame("RX", f.data, len);

  if (f.len < HARFA_HEADER_SIZE) {
    sendError(HARFA_STATUS_MALFORMED, 0xFF, 0xFF);
    return;
  }
  uint8_t type = f.data[1];
  uint8_t seq = f.data[2];

  harfa_msg_t msg{};
  harfa_status_t st = (f.len > HARFA_MAX_FRAME_SIZE)
                          ? HARFA_STATUS_MALFORMED
                          : harfa_decode(f.data, len, &msg);

  if (st == HARFA_STATUS_UNSUPPORTED_VERSION) {
    sendError(st, seq, type);
    return;
  }

  if (type == HARFA_MSG_HELLO) {
    if (st != HARFA_STATUS_OK) {
      sendError(HARFA_STATUS_MALFORMED, seq, HARFA_MSG_HELLO);
    } else {
      handleHello(msg);
    }
    return;
  }

  if (haveLast && seq == lastSeq && type == lastType) {
    Serial.println("(retransmisja - odsylam zapamietany status)");
    sendAck(seq, lastStatus);
    return;
  }

  uint8_t stateRequestWhat = 0;
  uint8_t status = (st == HARFA_STATUS_OK) ? executeCommand(msg, stateRequestWhat) : (uint8_t)st;

  haveLast = true;
  lastSeq = seq;
  lastType = type;
  lastStatus = status;
  sendAck(seq, status);

  if (status == HARFA_STATUS_OK && type == HARFA_MSG_STATE_REQUEST) {
    if (stateRequestWhat & HARFA_STATE_CONFIG) sendConfigState();
    if (stateRequestWhat & HARFA_STATE_BATTERY) sendBatteryState();
  }
}

// ===================== Callbacki BLE (wątek stosu BT) =====================

class ServerCallbacks : public BLEServerCallbacks {
  void onConnect(BLEServer* s, esp_ble_gatts_cb_param_t* param) override {
    connId = param->connect.conn_id;
    connected = true;
    connectEvent = true;
  }
  void onDisconnect(BLEServer* s) override {
    connected = false;
    disconnectEvent = true;
  }
};

class RxCallbacks : public BLECharacteristicCallbacks {
  void onWrite(BLECharacteristic* c) override {
    auto value = c->getValue();
    RxFrame f{};
    f.len = value.length();
    size_t n = f.len > HARFA_MAX_FRAME_SIZE ? HARFA_MAX_FRAME_SIZE : f.len;
    memcpy(f.data, value.c_str(), n);
    xQueueSend(rxQueue, &f, 0);
  }
};

// ===================== Komendy z Serial Monitora =====================

static void printHelp() {
  Serial.println(F(
      "Komendy:\n"
      "  s <0-14>             zagranie struny (STRING_EVENT, MIDI wg strojenia)\n"
      "  b <proc> [mV] [fl]   stan baterii (fl: 1=ladowanie 2=zasilacz 4=niski)\n"
      "  l <C-B> <-1|0|1>     przelacznik (bemol / naturalny / krzyzyk) -> CONFIG_STATE\n"
      "  o <-1|0|1>           transpozycja oktawowa -> CONFIG_STATE\n"
      "  e <kod>              spontaniczny ERROR (np. 7 = INTERNAL)\n"
      "  d                    zerwanie polaczenia\n"
      "  i                    status\n"
      "  h                    pomoc"));
}

static void printStatus() {
  Serial.printf("Nazwa: %s  device_id: 0x%08X\n", deviceName, deviceId);
  Serial.printf("Polaczenie: %s  sesja: %s  lekcja: %s (%u krokow)\n",
                connected ? "tak" : "nie", sessionActive ? "aktywna" : "brak",
                lessonActive ? "trwa" : "brak", lessonTotalSteps);
  Serial.printf("Bateria: %u%%  %u mV  flagi 0x%02X\n", batteryPercent, batteryMillivolts, batteryFlags);
  Serial.print("Strojenie:");
  for (int i = 0; i < HARFA_LEVER_COUNT; i++) {
    Serial.printf(" %c%s", NOTE_NAMES[i], config.lever[i] < 0 ? "b" : config.lever[i] > 0 ? "#" : "");
  }
  Serial.printf("  oktawa %+d\n", config.octave_shift);
  printLeds();
}

static bool requireSession() {
  if (!sessionActive) {
    Serial.println("Brak sesji - polacz aplikacje (harfa nie wysyla zdarzen przed HELLO).");
    return false;
  }
  return true;
}

static void handleCommand(const char* line) {
  int a = 0, b = 0, c = 0;
  char note = 0;

  switch (line[0]) {
    case 's':
      if (sscanf(line, "s %d", &a) == 1 && a >= 0 && a < (int)HARFA_STRING_COUNT) {
        if (requireSession()) sendStringEvent(a);
      } else {
        Serial.println("Uzycie: s <0-14>");
      }
      break;

    case 'b': {
      int n = sscanf(line, "b %d %d %d", &a, &b, &c);
      if (n >= 1 && ((a >= 0 && a <= 100) || a == HARFA_BATTERY_PERCENT_UNKNOWN)) {
        batteryPercent = (uint8_t)a;
        if (n >= 2) batteryMillivolts = (uint16_t)b;
        if (n >= 3) batteryFlags = (uint8_t)(c & HARFA_BATTERY_KNOWN_MASK);
        if (requireSession()) sendBatteryState();
      } else {
        Serial.println("Uzycie: b <0-100|255> [mV] [flagi]");
      }
      break;
    }

    case 'l': {
      const char* pos = nullptr;
      if (sscanf(line, "l %c %d", &note, &a) == 2 && a >= -1 && a <= 1 &&
          (pos = strchr(NOTE_NAMES, toupper(note))) != nullptr && *pos != '\0') {
        config.lever[pos - NOTE_NAMES] = (int8_t)a;
        if (requireSession()) sendConfigState();
      } else {
        Serial.println("Uzycie: l <C|D|E|F|G|A|B> <-1|0|1>");
      }
      break;
    }

    case 'o':
      if (sscanf(line, "o %d", &a) == 1 && a >= -1 && a <= 1) {
        config.octave_shift = (int8_t)a;
        if (requireSession()) sendConfigState();
      } else {
        Serial.println("Uzycie: o <-1|0|1>");
      }
      break;

    case 'e':
      if (sscanf(line, "e %d", &a) == 1 && a >= 0 && a <= 255) {
        if (requireSession()) sendError((uint8_t)a, 0xFF, 0xFF);
      } else {
        Serial.println("Uzycie: e <kod>");
      }
      break;

    case 'd':
      if (connected) {
        Serial.println("Zrywam polaczenie...");
        server->disconnect(connId);
      } else {
        Serial.println("Brak polaczenia.");
      }
      break;

    case 'i':
      printStatus();
      break;

    case 'h':
    case '?':
      printHelp();
      break;

    case '\0':
      break;

    default:
      Serial.println("Nieznana komenda, wpisz h");
  }
}

static void pollSerial() {
  static char line[64];
  static size_t lineLen = 0;
  while (Serial.available()) {
    char ch = (char)Serial.read();
    if (ch == '\r') continue;
    if (ch == '\n') {
      line[lineLen] = '\0';
      handleCommand(line);
      lineLen = 0;
    } else if (lineLen < sizeof(line) - 1) {
      line[lineLen++] = ch;
    }
  }
}

// ===================== setup / loop =====================

static void startAdvertising() {
  BLEAdvertising* adv = BLEDevice::getAdvertising();

  BLEAdvertisementData advData;
  advData.setFlags(0x06);  
  advData.setCompleteServices(BLEUUID(HARFA_SERVICE_UUID));
  adv->setAdvertisementData(advData);

  BLEAdvertisementData scanData;
  scanData.setName(deviceName);
  adv->setScanResponseData(scanData);

  BLEDevice::startAdvertising();
  Serial.printf("Reklamuje sie jako %s\n", deviceName);
}

void setup() {
  Serial.begin(115200);
  delay(200);

  uint64_t efuse = ESP.getEfuseMac();
  deviceId = (uint32_t)(efuse >> 16) ^ (uint32_t)efuse;
  snprintf(deviceName, sizeof(deviceName), "HARFA-%04X", (unsigned)(deviceId & 0xFFFF));

  rxQueue = xQueueCreate(16, sizeof(RxFrame));

  BLEDevice::init(deviceName);
  server = BLEDevice::createServer();
  server->setCallbacks(new ServerCallbacks());

  BLEService* service = server->createService(HARFA_SERVICE_UUID);

  BLECharacteristic* rxChar = service->createCharacteristic(
      HARFA_RX_CHAR_UUID,
      BLECharacteristic::PROPERTY_WRITE | BLECharacteristic::PROPERTY_WRITE_NR);
  rxChar->setCallbacks(new RxCallbacks());

  txChar = service->createCharacteristic(HARFA_TX_CHAR_UUID, BLECharacteristic::PROPERTY_NOTIFY);
  txChar->addDescriptor(new BLE2902()); 

  service->start();
  startAdvertising();
  printHelp();
}

void loop() {
  if (connectEvent) {
    connectEvent = false;
    Serial.println("Polaczono - czekam na HELLO");
  }

  if (disconnectEvent) {
    disconnectEvent = false;
    sessionActive = false;
    haveLast = false;
    endLessonAndLedsOff();
    Serial.println("Rozlaczono - lekcja zakonczona, diody zgaszone");
    delay(100);
    startAdvertising();
  }

  RxFrame f;
  while (xQueueReceive(rxQueue, &f, 0) == pdTRUE) {
    handleFrame(f);
  }

  pollSerial();
  delay(2);
}