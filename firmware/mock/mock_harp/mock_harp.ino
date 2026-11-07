#include "BluetoothSerial.h"

BluetoothSerial SerialBT;

const int buttonPins[7] = {13, 12, 14, 27, 26, 25, 33};

const char* notes[7] = {"F", "G", "A", "B", "C", "D", "E"};

bool lastButtonStates[7];

void setup() {
  Serial.begin(115200);

  SerialBT.begin("ESP32_BT");

  Serial.println("Bluetooth started!");

  for (int i = 0; i < 7; i++) {
    pinMode(buttonPins[i], INPUT_PULLUP);

    lastButtonStates[i] = digitalRead(buttonPins[i]);
  }
}

void loop() {

  for (int i = 0; i < 7; i++) {

    bool currentState = digitalRead(buttonPins[i]);

    if (lastButtonStates[i] == HIGH && currentState == LOW) {

      Serial.print("Button pressed: ");
      Serial.println(notes[i]);

      SerialBT.println(notes[i]);
    }

    lastButtonStates[i] = currentState;
  }

  delay(20); 
}