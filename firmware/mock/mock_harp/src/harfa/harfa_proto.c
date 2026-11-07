#include "harfa_proto.h"

#include <string.h>

#define STRING_MASK_LIMIT (1ul << HARFA_STRING_COUNT)
#define CONFIG_RAW_MAX 2u

static uint16_t read_u16(const uint8_t *p)
{
    return (uint16_t)(p[0] | ((uint16_t)p[1] << 8));
}

static uint32_t read_u32(const uint8_t *p)
{
    return (uint32_t)read_u16(p) | ((uint32_t)read_u16(p + 2) << 16);
}

static uint8_t *write_u16(uint8_t *p, uint16_t value)
{
    p[0] = (uint8_t)(value & 0xFFu);
    p[1] = (uint8_t)(value >> 8);
    return p + 2;
}

static uint8_t *write_u32(uint8_t *p, uint32_t value)
{
    p = write_u16(p, (uint16_t)(value & 0xFFFFu));
    return write_u16(p, (uint16_t)(value >> 16));
}

/* Minimalna długość payloadu (sekcja 3.2) albo -1 dla nieznanego typu */
static int min_payload_size(uint8_t type)
{
    switch (type) {
    case HARFA_MSG_HELLO: return 2;
    case HARFA_MSG_DEVICE_INFO: return 13;
    case HARFA_MSG_STATE_REQUEST: return 1;
    case HARFA_MSG_ACK: return 2;
    case HARFA_MSG_ERROR: return 3;
    case HARFA_MSG_BATTERY_STATE: return 4;
    case HARFA_MSG_STRING_EVENT: return 4;
    case HARFA_MSG_CONFIG_STATE: return 9;
    case HARFA_MSG_LED_SET: return 3;
    case HARFA_MSG_LESSON_START: return 2;
    case HARFA_MSG_LESSON_STEP: return 4;
    case HARFA_MSG_LESSON_RESULT: return 1;
    case HARFA_MSG_LESSON_END: return 0;
    default: return -1;
    }
}

static int is_tristate(int8_t value)
{
    return value >= -1 && value <= 1;
}

static int has_valid_arguments(const harfa_msg_t *msg)
{
    unsigned i;

    switch (msg->type) {
    case HARFA_MSG_STRING_EVENT:
        return msg->body.string_event.string_id < HARFA_STRING_COUNT;
    case HARFA_MSG_CONFIG_STATE:
        if (!is_tristate(msg->body.config_state.octave_shift)) {
            return 0;
        }
        for (i = 0; i < HARFA_LEVER_COUNT; i++) {
            if (!is_tristate(msg->body.config_state.lever[i])) {
                return 0;
            }
        }
        return 1;
    case HARFA_MSG_BATTERY_STATE:
        return msg->body.battery_state.percent <= 100u ||
               msg->body.battery_state.percent == HARFA_BATTERY_PERCENT_UNKNOWN;
    case HARFA_MSG_LED_SET:
        return msg->body.led_set.mask < STRING_MASK_LIMIT &&
               msg->body.led_set.mode <= HARFA_LED_MODE_BLINK;
    case HARFA_MSG_LESSON_STEP:
        return msg->body.lesson_step.mask < STRING_MASK_LIMIT;
    case HARFA_MSG_LESSON_RESULT:
        return msg->body.lesson_result.result <= HARFA_LESSON_COMPLETE;
    default:
        return 1;
    }
}

harfa_status_t harfa_decode(const uint8_t *frame, size_t frame_len, harfa_msg_t *out)
{
    const uint8_t *p;
    uint8_t len;
    int min_len;
    unsigned i;

    if (frame == NULL || out == NULL || frame_len < HARFA_HEADER_SIZE) {
        return HARFA_STATUS_MALFORMED;
    }
    if (frame[0] != HARFA_PROTO_VERSION_MAJOR) {
        return HARFA_STATUS_UNSUPPORTED_VERSION;
    }
    len = frame[3];
    if (len > HARFA_MAX_PAYLOAD_SIZE || frame_len != (size_t)HARFA_HEADER_SIZE + len) {
        return HARFA_STATUS_MALFORMED;
    }
    min_len = min_payload_size(frame[1]);
    if (min_len < 0) {
        return HARFA_STATUS_UNKNOWN_TYPE;
    }
    if (len < (unsigned)min_len) {
        return HARFA_STATUS_MALFORMED;
    }

    memset(out, 0, sizeof(*out));
    out->type = (harfa_msg_type_t)frame[1];
    out->seq = frame[2];
    p = frame + HARFA_HEADER_SIZE;

    /* Nadmiarowe bajty na końcu payloadu są pomijane (zgodność z przyszłymi wersjami minor) */
    switch (out->type) {
    case HARFA_MSG_HELLO:
        out->body.hello.proto_major = p[0];
        out->body.hello.proto_minor = p[1];
        break;
    case HARFA_MSG_DEVICE_INFO:
        out->body.device_info.proto_major = p[0];
        out->body.device_info.proto_minor = p[1];
        out->body.device_info.fw_major = p[2];
        out->body.device_info.fw_minor = p[3];
        out->body.device_info.fw_patch = p[4];
        out->body.device_info.hw_rev = p[5];
        out->body.device_info.string_count = p[6];
        out->body.device_info.caps = (uint16_t)(read_u16(p + 7) & HARFA_CAP_KNOWN_MASK);
        out->body.device_info.device_id = read_u32(p + 9);
        break;
    case HARFA_MSG_STATE_REQUEST:
        out->body.state_request.what = (uint8_t)(p[0] & HARFA_STATE_KNOWN_MASK);
        break;
    case HARFA_MSG_ACK:
        out->body.ack.ack_seq = p[0];
        out->body.ack.status = p[1];
        break;
    case HARFA_MSG_ERROR:
        out->body.error.code = p[0];
        out->body.error.ref_seq = p[1];
        out->body.error.ref_type = p[2];
        break;
    case HARFA_MSG_BATTERY_STATE:
        out->body.battery_state.percent = p[0];
        out->body.battery_state.millivolts = read_u16(p + 1);
        out->body.battery_state.flags = (uint8_t)(p[3] & HARFA_BATTERY_KNOWN_MASK);
        break;
    case HARFA_MSG_STRING_EVENT:
        out->body.string_event.string_id = p[0];
        out->body.string_event.midi = p[1];
        out->body.string_event.t_ms = read_u16(p + 2);
        break;
    case HARFA_MSG_CONFIG_STATE:
        /* Zakres sprawdzany przed odjęciem offsetu, aby uniknąć przepełnienia int8_t */
        for (i = 0; i < 1u + HARFA_LEVER_COUNT; i++) {
            if (p[i] > CONFIG_RAW_MAX) {
                return HARFA_STATUS_INVALID_ARGUMENT;
            }
        }
        out->body.config_state.octave_shift = (int8_t)((int)p[0] - 1);
        for (i = 0; i < HARFA_LEVER_COUNT; i++) {
            out->body.config_state.lever[i] = (int8_t)((int)p[1 + i] - 1);
        }
        out->body.config_state.base_octave = p[8];
        break;
    case HARFA_MSG_LED_SET:
        out->body.led_set.mask = read_u16(p);
        out->body.led_set.mode = p[2];
        break;
    case HARFA_MSG_LESSON_START:
        out->body.lesson_start.total_steps = read_u16(p);
        break;
    case HARFA_MSG_LESSON_STEP:
        out->body.lesson_step.index = read_u16(p);
        out->body.lesson_step.mask = read_u16(p + 2);
        break;
    case HARFA_MSG_LESSON_RESULT:
        out->body.lesson_result.result = p[0];
        break;
    case HARFA_MSG_LESSON_END:
        break;
    }

    return has_valid_arguments(out) ? HARFA_STATUS_OK : HARFA_STATUS_INVALID_ARGUMENT;
}

size_t harfa_encode(const harfa_msg_t *msg, uint8_t *buf, size_t buf_size)
{
    uint8_t payload[HARFA_MAX_PAYLOAD_SIZE];
    uint8_t *p = payload;
    size_t len;
    unsigned i;

    if (msg == NULL || buf == NULL || min_payload_size((uint8_t)msg->type) < 0 ||
        !has_valid_arguments(msg)) {
        return 0;
    }

    switch (msg->type) {
    case HARFA_MSG_HELLO:
        *p++ = msg->body.hello.proto_major;
        *p++ = msg->body.hello.proto_minor;
        break;
    case HARFA_MSG_DEVICE_INFO:
        *p++ = msg->body.device_info.proto_major;
        *p++ = msg->body.device_info.proto_minor;
        *p++ = msg->body.device_info.fw_major;
        *p++ = msg->body.device_info.fw_minor;
        *p++ = msg->body.device_info.fw_patch;
        *p++ = msg->body.device_info.hw_rev;
        *p++ = msg->body.device_info.string_count;
        p = write_u16(p, msg->body.device_info.caps);
        p = write_u32(p, msg->body.device_info.device_id);
        break;
    case HARFA_MSG_STATE_REQUEST:
        *p++ = msg->body.state_request.what;
        break;
    case HARFA_MSG_ACK:
        *p++ = msg->body.ack.ack_seq;
        *p++ = msg->body.ack.status;
        break;
    case HARFA_MSG_ERROR:
        *p++ = msg->body.error.code;
        *p++ = msg->body.error.ref_seq;
        *p++ = msg->body.error.ref_type;
        break;
    case HARFA_MSG_BATTERY_STATE:
        *p++ = msg->body.battery_state.percent;
        p = write_u16(p, msg->body.battery_state.millivolts);
        *p++ = msg->body.battery_state.flags;
        break;
    case HARFA_MSG_STRING_EVENT:
        *p++ = msg->body.string_event.string_id;
        *p++ = msg->body.string_event.midi;
        p = write_u16(p, msg->body.string_event.t_ms);
        break;
    case HARFA_MSG_CONFIG_STATE:
        *p++ = (uint8_t)(msg->body.config_state.octave_shift + 1);
        for (i = 0; i < HARFA_LEVER_COUNT; i++) {
            *p++ = (uint8_t)(msg->body.config_state.lever[i] + 1);
        }
        *p++ = msg->body.config_state.base_octave;
        break;
    case HARFA_MSG_LED_SET:
        p = write_u16(p, msg->body.led_set.mask);
        *p++ = msg->body.led_set.mode;
        break;
    case HARFA_MSG_LESSON_START:
        p = write_u16(p, msg->body.lesson_start.total_steps);
        break;
    case HARFA_MSG_LESSON_STEP:
        p = write_u16(p, msg->body.lesson_step.index);
        p = write_u16(p, msg->body.lesson_step.mask);
        break;
    case HARFA_MSG_LESSON_RESULT:
        *p++ = msg->body.lesson_result.result;
        break;
    case HARFA_MSG_LESSON_END:
        break;
    }

    len = (size_t)(p - payload);
    if (buf_size < HARFA_HEADER_SIZE + len) {
        return 0;
    }

    buf[0] = HARFA_PROTO_VERSION_MAJOR;
    buf[1] = (uint8_t)msg->type;
    buf[2] = msg->seq;
    buf[3] = (uint8_t)len;
    memcpy(buf + HARFA_HEADER_SIZE, payload, len);
    return HARFA_HEADER_SIZE + len;
}

const char *harfa_msg_type_name(uint8_t type)
{
    switch (type) {
    case HARFA_MSG_HELLO: return "HELLO";
    case HARFA_MSG_DEVICE_INFO: return "DEVICE_INFO";
    case HARFA_MSG_STATE_REQUEST: return "STATE_REQUEST";
    case HARFA_MSG_ACK: return "ACK";
    case HARFA_MSG_ERROR: return "ERROR";
    case HARFA_MSG_BATTERY_STATE: return "BATTERY_STATE";
    case HARFA_MSG_STRING_EVENT: return "STRING_EVENT";
    case HARFA_MSG_CONFIG_STATE: return "CONFIG_STATE";
    case HARFA_MSG_LED_SET: return "LED_SET";
    case HARFA_MSG_LESSON_START: return "LESSON_START";
    case HARFA_MSG_LESSON_STEP: return "LESSON_STEP";
    case HARFA_MSG_LESSON_RESULT: return "LESSON_RESULT";
    case HARFA_MSG_LESSON_END: return "LESSON_END";
    default: return NULL;
    }
}

const char *harfa_status_name(uint8_t status)
{
    switch (status) {
    case HARFA_STATUS_OK: return "OK";
    case HARFA_STATUS_UNSUPPORTED_VERSION: return "UNSUPPORTED_VERSION";
    case HARFA_STATUS_UNKNOWN_TYPE: return "UNKNOWN_TYPE";
    case HARFA_STATUS_MALFORMED: return "MALFORMED";
    case HARFA_STATUS_INVALID_ARGUMENT: return "INVALID_ARGUMENT";
    case HARFA_STATUS_NOT_READY: return "NOT_READY";
    case HARFA_STATUS_BUSY: return "BUSY";
    case HARFA_STATUS_INTERNAL: return "INTERNAL";
    default: return NULL;
    }
}
