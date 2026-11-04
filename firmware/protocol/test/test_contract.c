/*
 * Test kontraktowy kodeka C: każdy wektor z docs/protocol/test-vectors.json
 * (sekcja 10 specyfikacji). Uruchomienie: test_contract <ścieżka do test-vectors.json>
 */
#include "harfa_proto.h"
#include "cJSON.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define DEFAULT_VECTORS_PATH "../../../docs/protocol/test-vectors.json"
#define MAX_TEST_FRAME 64

static char *read_file(const char *path)
{
    FILE *f = fopen(path, "rb");
    char *text;
    long size;

    if (f == NULL) {
        return NULL;
    }
    fseek(f, 0, SEEK_END);
    size = ftell(f);
    fseek(f, 0, SEEK_SET);
    text = malloc((size_t)size + 1);
    if (text != NULL && fread(text, 1, (size_t)size, f) != (size_t)size) {
        free(text);
        text = NULL;
    }
    if (text != NULL) {
        text[size] = '\0';
    }
    fclose(f);
    return text;
}

static size_t parse_hex(const char *hex, uint8_t *out, size_t cap)
{
    size_t n = 0;

    while (*hex != '\0' && n < cap) {
        unsigned value;
        if (*hex == ' ') {
            hex++;
            continue;
        }
        if (sscanf(hex, "%2x", &value) != 1) {
            break;
        }
        out[n++] = (uint8_t)value;
        hex += 2;
    }
    return n;
}

static long num(const cJSON *object, const char *key)
{
    const cJSON *v = cJSON_GetObjectItemCaseSensitive(object, key);
    return cJSON_IsNumber(v) ? (long)v->valuedouble : -1000;
}

static double num_u32(const cJSON *object, const char *key)
{
    const cJSON *v = cJSON_GetObjectItemCaseSensitive(object, key);
    return cJSON_IsNumber(v) ? v->valuedouble : -1.0;
}

static const char *str(const cJSON *object, const char *key)
{
    const char *s = cJSON_GetStringValue(cJSON_GetObjectItemCaseSensitive(object, key));
    return s != NULL ? s : "";
}

static int flag(const cJSON *object, const char *key)
{
    return cJSON_IsTrue(cJSON_GetObjectItemCaseSensitive(object, key));
}

static int type_from_name(const char *name, harfa_msg_type_t *out)
{
    unsigned code;

    for (code = 0; code <= 0xFFu; code++) {
        const char *candidate = harfa_msg_type_name((uint8_t)code);
        if (candidate != NULL && strcmp(candidate, name) == 0) {
            *out = (harfa_msg_type_t)code;
            return 1;
        }
    }
    return 0;
}

static int status_from_name(const char *name, harfa_status_t *out)
{
    unsigned code;

    for (code = 0; code <= 0xFFu; code++) {
        const char *candidate = harfa_status_name((uint8_t)code);
        if (candidate != NULL && strcmp(candidate, name) == 0) {
            *out = (harfa_status_t)code;
            return 1;
        }
    }
    return 0;
}

/* Buduje oczekiwaną wiadomość z pola "decoded" wektora */
static int expected_message(const cJSON *d, harfa_msg_t *m)
{
    const cJSON *lever;
    unsigned i;

    memset(m, 0, sizeof(*m));
    if (!type_from_name(str(d, "type"), &m->type)) {
        return 0;
    }
    m->seq = (uint8_t)num(d, "seq");

    switch (m->type) {
    case HARFA_MSG_HELLO:
        m->body.hello.proto_major = (uint8_t)num(d, "proto_major");
        m->body.hello.proto_minor = (uint8_t)num(d, "proto_minor");
        break;
    case HARFA_MSG_DEVICE_INFO:
        m->body.device_info.proto_major = (uint8_t)num(d, "proto_major");
        m->body.device_info.proto_minor = (uint8_t)num(d, "proto_minor");
        m->body.device_info.fw_major = (uint8_t)num(d, "fw_major");
        m->body.device_info.fw_minor = (uint8_t)num(d, "fw_minor");
        m->body.device_info.fw_patch = (uint8_t)num(d, "fw_patch");
        m->body.device_info.hw_rev = (uint8_t)num(d, "hw_rev");
        m->body.device_info.string_count = (uint8_t)num(d, "string_count");
        m->body.device_info.caps = (uint16_t)num(d, "caps");
        m->body.device_info.device_id = (uint32_t)num_u32(d, "device_id");
        break;
    case HARFA_MSG_STATE_REQUEST:
        m->body.state_request.what = (uint8_t)num(d, "what");
        break;
    case HARFA_MSG_ACK:
        m->body.ack.ack_seq = (uint8_t)num(d, "ack_seq");
        m->body.ack.status = (uint8_t)num(d, "status");
        break;
    case HARFA_MSG_ERROR:
        m->body.error.code = (uint8_t)num(d, "code");
        m->body.error.ref_seq = (uint8_t)num(d, "ref_seq");
        m->body.error.ref_type = (uint8_t)num(d, "ref_type");
        break;
    case HARFA_MSG_BATTERY_STATE:
        m->body.battery_state.percent = (uint8_t)num(d, "percent");
        m->body.battery_state.millivolts = (uint16_t)num(d, "millivolts");
        m->body.battery_state.flags = (uint8_t)num(d, "flags");
        break;
    case HARFA_MSG_STRING_EVENT:
        m->body.string_event.string_id = (uint8_t)num(d, "string_id");
        m->body.string_event.midi = (uint8_t)num(d, "midi");
        m->body.string_event.t_ms = (uint16_t)num(d, "t_ms");
        break;
    case HARFA_MSG_CONFIG_STATE:
        lever = cJSON_GetObjectItemCaseSensitive(d, "lever");
        if (!cJSON_IsArray(lever) || cJSON_GetArraySize(lever) != (int)HARFA_LEVER_COUNT) {
            return 0;
        }
        m->body.config_state.octave_shift = (int8_t)num(d, "octave_shift");
        for (i = 0; i < HARFA_LEVER_COUNT; i++) {
            m->body.config_state.lever[i] = (int8_t)cJSON_GetArrayItem(lever, (int)i)->valuedouble;
        }
        m->body.config_state.base_octave = (uint8_t)num(d, "base_octave");
        break;
    case HARFA_MSG_LED_SET:
        m->body.led_set.mask = (uint16_t)num(d, "mask");
        m->body.led_set.mode = (uint8_t)num(d, "mode");
        break;
    case HARFA_MSG_LESSON_START:
        m->body.lesson_start.total_steps = (uint16_t)num(d, "total_steps");
        break;
    case HARFA_MSG_LESSON_STEP:
        m->body.lesson_step.index = (uint16_t)num(d, "index");
        m->body.lesson_step.mask = (uint16_t)num(d, "mask");
        break;
    case HARFA_MSG_LESSON_RESULT:
        m->body.lesson_result.result = (uint8_t)num(d, "result");
        break;
    case HARFA_MSG_LESSON_END:
        break;
    }
    return 1;
}

static int messages_equal(const harfa_msg_t *a, const harfa_msg_t *b)
{
    if (a->type != b->type || a->seq != b->seq) {
        return 0;
    }
    switch (a->type) {
    case HARFA_MSG_HELLO:
        return a->body.hello.proto_major == b->body.hello.proto_major &&
               a->body.hello.proto_minor == b->body.hello.proto_minor;
    case HARFA_MSG_DEVICE_INFO:
        return a->body.device_info.proto_major == b->body.device_info.proto_major &&
               a->body.device_info.proto_minor == b->body.device_info.proto_minor &&
               a->body.device_info.fw_major == b->body.device_info.fw_major &&
               a->body.device_info.fw_minor == b->body.device_info.fw_minor &&
               a->body.device_info.fw_patch == b->body.device_info.fw_patch &&
               a->body.device_info.hw_rev == b->body.device_info.hw_rev &&
               a->body.device_info.string_count == b->body.device_info.string_count &&
               a->body.device_info.caps == b->body.device_info.caps &&
               a->body.device_info.device_id == b->body.device_info.device_id;
    case HARFA_MSG_STATE_REQUEST:
        return a->body.state_request.what == b->body.state_request.what;
    case HARFA_MSG_ACK:
        return a->body.ack.ack_seq == b->body.ack.ack_seq &&
               a->body.ack.status == b->body.ack.status;
    case HARFA_MSG_ERROR:
        return a->body.error.code == b->body.error.code &&
               a->body.error.ref_seq == b->body.error.ref_seq &&
               a->body.error.ref_type == b->body.error.ref_type;
    case HARFA_MSG_BATTERY_STATE:
        return a->body.battery_state.percent == b->body.battery_state.percent &&
               a->body.battery_state.millivolts == b->body.battery_state.millivolts &&
               a->body.battery_state.flags == b->body.battery_state.flags;
    case HARFA_MSG_STRING_EVENT:
        return a->body.string_event.string_id == b->body.string_event.string_id &&
               a->body.string_event.midi == b->body.string_event.midi &&
               a->body.string_event.t_ms == b->body.string_event.t_ms;
    case HARFA_MSG_CONFIG_STATE:
        return a->body.config_state.octave_shift == b->body.config_state.octave_shift &&
               memcmp(a->body.config_state.lever, b->body.config_state.lever,
                      sizeof(a->body.config_state.lever)) == 0 &&
               a->body.config_state.base_octave == b->body.config_state.base_octave;
    case HARFA_MSG_LED_SET:
        return a->body.led_set.mask == b->body.led_set.mask &&
               a->body.led_set.mode == b->body.led_set.mode;
    case HARFA_MSG_LESSON_START:
        return a->body.lesson_start.total_steps == b->body.lesson_start.total_steps;
    case HARFA_MSG_LESSON_STEP:
        return a->body.lesson_step.index == b->body.lesson_step.index &&
               a->body.lesson_step.mask == b->body.lesson_step.mask;
    case HARFA_MSG_LESSON_RESULT:
        return a->body.lesson_result.result == b->body.lesson_result.result;
    case HARFA_MSG_LESSON_END:
        return 1;
    }
    return 0;
}

static int check_vector(const cJSON *v)
{
    const char *name = str(v, "name");
    uint8_t frame[MAX_TEST_FRAME];
    size_t frame_len = parse_hex(str(v, "hex"), frame, sizeof(frame));
    harfa_msg_t decoded;
    harfa_status_t status = harfa_decode(frame, frame_len, &decoded);

    if (!flag(v, "valid")) {
        harfa_status_t expected;
        if (!status_from_name(str(v, "error"), &expected)) {
            printf("FAIL %s: unknown error name in vector\n", name);
            return 0;
        }
        if (status != expected) {
            printf("FAIL %s: expected %s, got %s\n", name,
                   harfa_status_name(expected), harfa_status_name(status));
            return 0;
        }
        return 1;
    } else {
        const cJSON *d = cJSON_GetObjectItemCaseSensitive(v, "decoded");
        harfa_msg_t expected;
        uint8_t encoded[HARFA_MAX_FRAME_SIZE];
        size_t encoded_len;

        if (num(d, "ver") != HARFA_PROTO_VERSION_MAJOR || !expected_message(d, &expected)) {
            printf("FAIL %s: cannot build expected message\n", name);
            return 0;
        }
        if (status != HARFA_STATUS_OK) {
            printf("FAIL %s: decode returned %s\n", name, harfa_status_name(status));
            return 0;
        }
        if (!messages_equal(&decoded, &expected)) {
            printf("FAIL %s: decoded message differs from vector\n", name);
            return 0;
        }
        if (flag(v, "roundtrip")) {
            encoded_len = harfa_encode(&expected, encoded, sizeof(encoded));
            if (encoded_len != frame_len || memcmp(encoded, frame, frame_len) != 0) {
                printf("FAIL %s: encoded frame differs from vector\n", name);
                return 0;
            }
        }
        return 1;
    }
}

/* Testy spoza wektorów: odrzucanie niepoprawnych argumentów i za małego bufora */
static int check_encode_guards(void)
{
    harfa_msg_t m;
    uint8_t buf[HARFA_MAX_FRAME_SIZE];
    int ok = 1;

    memset(&m, 0, sizeof(m));
    m.type = HARFA_MSG_STRING_EVENT;
    m.body.string_event.string_id = HARFA_STRING_COUNT;
    if (harfa_encode(&m, buf, sizeof(buf)) != 0) {
        printf("FAIL encode_guard_string_15\n");
        ok = 0;
    }

    memset(&m, 0, sizeof(m));
    m.type = HARFA_MSG_LED_SET;
    m.body.led_set.mask = 0x8000u;
    if (harfa_encode(&m, buf, sizeof(buf)) != 0) {
        printf("FAIL encode_guard_led_mask\n");
        ok = 0;
    }

    memset(&m, 0, sizeof(m));
    m.type = HARFA_MSG_DEVICE_INFO;
    if (harfa_encode(&m, buf, HARFA_HEADER_SIZE + 12u) != 0) {
        printf("FAIL encode_guard_small_buffer\n");
        ok = 0;
    }
    return ok;
}

int main(int argc, char **argv)
{
    const char *path = argc > 1 ? argv[1] : DEFAULT_VECTORS_PATH;
    char *text = read_file(path);
    cJSON *root;
    const cJSON *vectors;
    const cJSON *vector;
    int total, passed = 0;
    int guards_ok;

    if (text == NULL) {
        printf("Cannot read %s\n", path);
        return 2;
    }
    root = cJSON_Parse(text);
    free(text);
    vectors = cJSON_GetObjectItemCaseSensitive(root, "vectors");
    if (!cJSON_IsArray(vectors)) {
        printf("Invalid vectors file %s\n", path);
        cJSON_Delete(root);
        return 2;
    }

    total = cJSON_GetArraySize(vectors);
    cJSON_ArrayForEach(vector, vectors) {
        passed += check_vector(vector);
    }
    guards_ok = check_encode_guards();

    printf("%d/%d vectors passed, encode guards: %s\n", passed, total,
           guards_ok ? "OK" : "FAIL");
    cJSON_Delete(root);
    return (passed == total && guards_ok) ? 0 : 1;
}
