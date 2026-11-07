/*
 * Kodek ramek protokołu BLE HARFA w wersji 1.0 (docs/protocol/ble-protocol.md).
 *
 * Czysty C99: bez HAL, bez alokacji pamięci, bez stdio. Ten sam kod jest
 * kompilowany w firmware STM32WB55 oraz w testach kontraktowych na komputerze.
 */
#ifndef HARFA_PROTO_H
#define HARFA_PROTO_H

#include <stddef.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

#define HARFA_PROTO_VERSION_MAJOR 1u
#define HARFA_PROTO_VERSION_MINOR 0u

#define HARFA_HEADER_SIZE 4u
#define HARFA_MAX_PAYLOAD_SIZE 16u
#define HARFA_MAX_FRAME_SIZE (HARFA_HEADER_SIZE + HARFA_MAX_PAYLOAD_SIZE)

#define HARFA_STRING_COUNT 15u
#define HARFA_LEVER_COUNT 7u

#define HARFA_SERVICE_UUID "3CFB0001-6F66-44DE-A34A-0BCE651D326C"
#define HARFA_RX_CHAR_UUID "3CFB0002-6F66-44DE-A34A-0BCE651D326C"
#define HARFA_TX_CHAR_UUID "3CFB0003-6F66-44DE-A34A-0BCE651D326C"

typedef enum {
    HARFA_MSG_HELLO = 0x01,
    HARFA_MSG_DEVICE_INFO = 0x02,
    HARFA_MSG_STATE_REQUEST = 0x03,
    HARFA_MSG_ACK = 0x10,
    HARFA_MSG_ERROR = 0x11,
    HARFA_MSG_BATTERY_STATE = 0x12,
    HARFA_MSG_STRING_EVENT = 0x20,
    HARFA_MSG_CONFIG_STATE = 0x21,
    HARFA_MSG_LED_SET = 0x30,
    HARFA_MSG_LESSON_START = 0x31,
    HARFA_MSG_LESSON_STEP = 0x32,
    HARFA_MSG_LESSON_RESULT = 0x33,
    HARFA_MSG_LESSON_END = 0x34
} harfa_msg_type_t;

typedef enum {
    HARFA_STATUS_OK = 0x00,
    HARFA_STATUS_UNSUPPORTED_VERSION = 0x01,
    HARFA_STATUS_UNKNOWN_TYPE = 0x02,
    HARFA_STATUS_MALFORMED = 0x03,
    HARFA_STATUS_INVALID_ARGUMENT = 0x04,
    HARFA_STATUS_NOT_READY = 0x05,
    HARFA_STATUS_BUSY = 0x06,
    HARFA_STATUS_INTERNAL = 0x07
} harfa_status_t;

/* Maska caps w DEVICE_INFO */
#define HARFA_CAP_LEDS (1u << 0)
#define HARFA_CAP_BATTERY (1u << 1)
#define HARFA_CAP_LESSON (1u << 2)
#define HARFA_CAP_DISPLAY (1u << 3)
#define HARFA_CAP_KNOWN_MASK 0x000Fu

/* Pole what w STATE_REQUEST */
#define HARFA_STATE_CONFIG (1u << 0)
#define HARFA_STATE_BATTERY (1u << 1)
#define HARFA_STATE_KNOWN_MASK 0x03u

/* Pole flags w BATTERY_STATE */
#define HARFA_BATTERY_CHARGING (1u << 0)
#define HARFA_BATTERY_EXTERNAL_POWER (1u << 1)
#define HARFA_BATTERY_LOW (1u << 2)
#define HARFA_BATTERY_KNOWN_MASK 0x07u
#define HARFA_BATTERY_PERCENT_UNKNOWN 0xFFu

#define HARFA_LED_MODE_STEADY 0x00u
#define HARFA_LED_MODE_BLINK 0x01u

#define HARFA_LESSON_MISS 0x00u
#define HARFA_LESSON_HIT 0x01u
#define HARFA_LESSON_COMPLETE 0x02u

typedef struct {
    uint8_t proto_major;
    uint8_t proto_minor;
} harfa_hello_t;

typedef struct {
    uint8_t proto_major;
    uint8_t proto_minor;
    uint8_t fw_major;
    uint8_t fw_minor;
    uint8_t fw_patch;
    uint8_t hw_rev;
    uint8_t string_count;
    uint16_t caps;
    uint32_t device_id;
} harfa_device_info_t;

typedef struct {
    uint8_t what;
} harfa_state_request_t;

typedef struct {
    uint8_t ack_seq;
    uint8_t status; /* harfa_status_t lub kod z przyszłej wersji minor */
} harfa_ack_t;

typedef struct {
    uint8_t code;
    uint8_t ref_seq;
    uint8_t ref_type;
} harfa_error_t;

typedef struct {
    uint8_t percent;
    uint16_t millivolts;
    uint8_t flags;
} harfa_battery_state_t;

typedef struct {
    uint8_t string_id;
    uint8_t midi;
    uint16_t t_ms;
} harfa_string_event_t;

/* Wartości logiczne -1, 0, +1 (bez offsetu +1 stosowanego w ramce) */
typedef struct {
    int8_t octave_shift;
    int8_t lever[HARFA_LEVER_COUNT];
    uint8_t base_octave;
} harfa_config_state_t;

typedef struct {
    uint16_t mask;
    uint8_t mode;
} harfa_led_set_t;

typedef struct {
    uint16_t total_steps;
} harfa_lesson_start_t;

typedef struct {
    uint16_t index;
    uint16_t mask;
} harfa_lesson_step_t;

typedef struct {
    uint8_t result;
} harfa_lesson_result_t;

typedef struct {
    harfa_msg_type_t type;
    uint8_t seq;
    union {
        harfa_hello_t hello;
        harfa_device_info_t device_info;
        harfa_state_request_t state_request;
        harfa_ack_t ack;
        harfa_error_t error;
        harfa_battery_state_t battery_state;
        harfa_string_event_t string_event;
        harfa_config_state_t config_state;
        harfa_led_set_t led_set;
        harfa_lesson_start_t lesson_start;
        harfa_lesson_step_t lesson_step;
        harfa_lesson_result_t lesson_result;
    } body; /* LESSON_END nie ma payloadu */
} harfa_msg_t;

/*
 * Dekoduje ramkę. Zwraca HARFA_STATUS_OK i wypełnia *out albo kod błędu
 * (MALFORMED, UNSUPPORTED_VERSION, UNKNOWN_TYPE, INVALID_ARGUMENT).
 * Kolejność sprawdzeń zgodna z sekcją 10.1 specyfikacji.
 */
harfa_status_t harfa_decode(const uint8_t *frame, size_t frame_len, harfa_msg_t *out);

/*
 * Koduje wiadomość do buf. Zwraca liczbę zapisanych bajtów albo 0, gdy
 * wiadomość ma niepoprawne argumenty lub bufor jest za mały.
 * Bufor o rozmiarze HARFA_MAX_FRAME_SIZE zawsze wystarcza.
 */
size_t harfa_encode(const harfa_msg_t *msg, uint8_t *buf, size_t buf_size);

/* Nazwa typu wiadomości zgodna ze specyfikacją albo NULL dla nieznanego typu */
const char *harfa_msg_type_name(uint8_t type);

/* Nazwa kodu statusu zgodna ze specyfikacją albo NULL dla nieznanego kodu */
const char *harfa_status_name(uint8_t status);

#ifdef __cplusplus
}
#endif

#endif /* HARFA_PROTO_H */
