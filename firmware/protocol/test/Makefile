# Testy kontraktowe kodeka protokołu BLE na komputerze (gcc + curl, Linux/WSL/MinGW).
# Uruchomienie z katalogu firmware/protocol/test:  make test
#
# cJSON (MIT, https://github.com/DaveGamble/cJSON) jest pobierany przy pierwszym
# buildzie do build/cjson i weryfikowany sumą SHA-256. Nie trafia do firmware.

CC = gcc
CFLAGS = -std=c99 -Wall -Wextra -Wpedantic -Werror -O2 -I.. -I$(CJSON_DIR)
CJSON_CFLAGS = -std=c99 -O2
VECTORS = ../../../docs/protocol/test-vectors.json

CJSON_VERSION = v1.7.18
CJSON_URL = https://raw.githubusercontent.com/DaveGamble/cJSON/$(CJSON_VERSION)
CJSON_C_SHA256 = 75c51de8fa40ac9d7a99319c6330719bd692eb81c0a869265f3d4c682533f9b9
CJSON_H_SHA256 = 0578cc29132912edbc88f83207a8fc76e5db3db0605497e909a9384ef3cc474b

BUILD = build
CJSON_DIR = $(BUILD)/cjson
TARGET = $(BUILD)/test_contract

.PHONY: all test clean

all: $(TARGET)

$(CJSON_DIR)/cJSON.h $(CJSON_DIR)/cJSON.c &:
	mkdir -p $(CJSON_DIR)
	curl -fsSL -o $(CJSON_DIR)/cJSON.c $(CJSON_URL)/cJSON.c
	curl -fsSL -o $(CJSON_DIR)/cJSON.h $(CJSON_URL)/cJSON.h
	echo "$(CJSON_C_SHA256)  $(CJSON_DIR)/cJSON.c" | sha256sum -c - || { rm -rf $(CJSON_DIR); exit 1; }
	echo "$(CJSON_H_SHA256)  $(CJSON_DIR)/cJSON.h" | sha256sum -c - || { rm -rf $(CJSON_DIR); exit 1; }

$(BUILD)/cJSON.o: $(CJSON_DIR)/cJSON.c $(CJSON_DIR)/cJSON.h
	$(CC) $(CJSON_CFLAGS) -c -o $@ $<

$(TARGET): ../harfa_proto.c ../harfa_proto.h test_contract.c $(CJSON_DIR)/cJSON.h $(BUILD)/cJSON.o
	$(CC) $(CFLAGS) -o $@ ../harfa_proto.c test_contract.c $(BUILD)/cJSON.o

test: $(TARGET)
	./$(TARGET) $(VECTORS)

clean:
	rm -rf $(BUILD)
