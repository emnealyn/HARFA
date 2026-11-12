# Mock harfy (ESP32, BLE GATT)

Symulator instrumentu na ESP32 zgodny z protokołem BLE HARFA v2.0
([docs/protocol/ble-protocol.md](../../docs/protocol/ble-protocol.md)). Pozwala sprawdzić
komunikację aplikacji z „prawdziwym” urządzeniem BLE bez fizycznej harfy (HARFA-25).

Mock służy wyłącznie do rozwoju i testów. Nie jest firmware produkcyjnym: docelowy
firmware działa na STM32WB55 (ADR 003). Aplikacja nie zawiera żadnej logiki specyficznej
dla mocka.

## Co robi mock

- Reklamuje usługę `3CFB0001-6F66-44DE-A34A-0BCE651D326C` (pełny 128-bitowy UUID w pakiecie
  reklamowym), a w scan response podaje nazwę `HARFA-XXXX`.
- Udostępnia charakterystyki:
  - **RX** `3CFB0002-…`: Write i Write Without Response, przyjmuje komendy aplikacji,
  - **TX** `3CFB0003-…`: Notify z CCCD `0x2902`, wysyła odpowiedzi i zdarzenia.
- Ramki koduje i dekoduje **ten sam kodek C** co firmware (`firmware/protocol/harfa_proto.c`).
- Na `HELLO` odpowiada `DEVICE_INFO` (protokół 2.0, 15 strun, caps `LEDS | BATTERY | LESSON`),
  a potem wysyła `CONFIG_STATE` i `BATTERY_STATE`. Zapamiętuje `sid` z `HELLO` jako identyfikator sesji
  i wysyła go w nagłówku każdej ramki.
- Komendę z innym `sid` odrzuca statusem `STALE_SESSION` bez jej wykonania.
- Na `STATE_REQUEST`, `LED_SET` i `LESSON_*` zawsze odpowiada `ACK` ze statusem zgodnym ze spec §6.2.
  Retransmisja (ten sam `sid`, `seq` i `type`) nie jest wykonywana ponownie, tylko dostaje zapamiętany status.
- `LESSON_PAUSE` wstrzymuje lekcję (gasi diody, zachowuje krok) i ją wznawia.
- Stan diod i lekcji wypisuje w Serial Monitorze. Po zerwaniu połączenia kończy lekcję, gasi diody
  i ponownie zaczyna się reklamować.
- `device_id` pochodzi z eFuse układu, więc jest stały dla danej płytki. Aplikacja nie
  potrzebuje adresu MAC mocka: wyszukuje urządzenia po UUID usługi.

## Wymagania

- Arduino IDE 2.x albo `arduino-cli`.
- Core **esp32 by Espressif Systems** (Boards Manager), wersja 2.x lub 3.x. Biblioteka BLE jest
  częścią core, więc dodatkowe biblioteki nie są potrzebne.
- Płytka: **ESP32 Dev Module** (lub inny ESP32 z BLE; ESP32-S2 nie ma BLE).

## Kodek protokołu

Arduino kompiluje tylko pliki z folderu szkicu, dlatego `mock_harp/src/harfa/` zawiera **kopię**
`harfa_proto.h` i `harfa_proto.c`. Źródłem prawdy jest `firmware/protocol/`. Kopii nie edytuj
ręcznie. Po każdej zmianie kodeka odśwież ją (z katalogu głównego repo):

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File firmware/mock/sync-proto.ps1
```

i zacommituj zmienione pliki razem ze zmianą w `firmware/protocol/`.

## Wgrywanie

1. Otwórz `firmware/mock/mock_harp/mock_harp.ino` w Arduino IDE.
2. Wybierz płytkę ESP32 Dev Module i port COM, potem kliknij Upload.
3. Otwórz Serial Monitor: **115200**, zakończenie linii **New Line**. Pojawi się
   `Reklamuje sie jako HARFA-XXXX` i lista komend.

Z wiersza poleceń:

```bash
arduino-cli compile --fqbn esp32:esp32:esp32 firmware/mock/mock_harp
arduino-cli upload  --fqbn esp32:esp32:esp32 -p COM5 firmware/mock/mock_harp
```

## Komendy Serial Monitora

| Komenda | Działanie |
| :--- | :--- |
| `s <0-14>` | Zagranie struny: `STRING_EVENT` z MIDI liczonym z bieżącego strojenia (note-mapping §4). |
| `b <proc> [mV] [flagi]` | Stan baterii: `BATTERY_STATE`. Flagi: `1` ładowanie, `2` zasilacz, `4` niski poziom. `proc = 255` oznacza nieznany. |
| `l <C-B> <-1\|0\|1>` | Przełącznik nuty: bemol, naturalny, krzyżyk. Wysyła `CONFIG_STATE`. |
| `o <-1\|0\|1>` | Transpozycja oktawowa. Wysyła `CONFIG_STATE`. |
| `e <kod>` | Spontaniczny `ERROR(kod, 0xFF, 0xFF)`, np. `e 7` = `INTERNAL`. |
| `d` | Zerwanie połączenia BLE. |
| `i` | Status: połączenie, sesja, lekcja, bateria, strojenie, diody. |
| `h` | Pomoc. |

Zdarzenia (`s`, `b`, `l`, `o`, `e`) są wysyłane tylko w aktywnej sesji, czyli po `HELLO` od
aplikacji. Zmiany baterii i strojenia wpisane bez połączenia są zapamiętywane i trafiają do
aplikacji przy następnym handshake.

Każda ramka jest logowana szesnastkowo: `RX` to ramki od aplikacji, `TX` to ramki od mocka.

## Sprawdzenie

### Bez aplikacji (nRF Connect)

1. Skan pokazuje `HARFA-XXXX` z usługą `3CFB0001-…`.
2. Połącz się i włącz notyfikacje na TX (`3CFB0003-…`).
3. Zapisz na RX (`3CFB0002-…`) bajty `02010001020200` (`HELLO`, protokół 2.0, `sid = 1`).
   Przyjdą trzy notyfikacje: `DEVICE_INFO`, `CONFIG_STATE`, `BATTERY_STATE`.
4. Zapisz `0230010103010000` (`LED_SET`, struna 0, świecenie ciągłe). Przyjdzie `ACK` OK, a w Serialu pojawi się `LED: #..............`.
5. Zapisz `0230020203010000` (ta sama komenda z `sid = 2`). Przyjdzie `ACK` ze statusem `08` (`STALE_SESSION`), a diody się nie zmienią.
6. Zapisz kolejno `02310301020300` (`LESSON_START`, 3 kroki), `023204010400000008` (`LESSON_STEP`, struna 11)
   i `023505010101` (`LESSON_PAUSE`, wstrzymaj): diody gasną. `023506010100` (wznów) zapala je ponownie.

### Z aplikacją

Użyj buildu debug z **wyłączonym** symulatorem (Ustawienia → Developer), żeby aplikacja używała
prawdziwego BLE.

1. Ekran Bluetooth: skan znajduje `HARFA-XXXX`. Połącz. W Serialu pojawi się `HELLO od aplikacji`,
   a aplikacja pokaże połączenie.
2. Ustaw strojenie pod utwór (np. `l F 1`) i włącz tryb nauki. W Serialu zobaczysz `Lekcja: start`
   oraz `LED:` z podświetloną struną.
3. Wpisz `s <numer podświetlonej struny>`. Lekcja w aplikacji przesuwa się o krok, a w Serialu
   pojawia się `wynik HIT` i kolejny krok.
4. Wpisz `s` z inną struną. Pojawi się `wynik MISS`.
5. Wpisz `b 10 3400 4`. Aplikacja dostaje niski stan baterii.
6. Wpisz `d`. Lekcja w aplikacji jest wstrzymana i pokazuje baner o braku połączenia, a mock znowu
   się reklamuje. Po ponownym połączeniu pojawia się nowe `HELLO`, `Lekcja: start` i bieżący krok
   (spec §8 pkt 9).

### Scenariusz akceptacyjny trybu nauki (HARFA-14)

Pełna lista do przejścia przed oddaniem zmian w trybie nauki. Kolumna „Serial” pokazuje, czego
szukać w logu mocka (pomijając linie `RX`/`TX`).

| # | Akcja | Aplikacja | Serial |
| :---: | :--- | :--- | :--- |
| 1 | Połącz się z `HARFA-XXXX`, włącz tryb nauki | krok 0 podświetlony w tekście | `Lekcja: start, krokow N`, `Lekcja: krok 0`, `LED:` z jedną struną |
| 2 | `s <podświetlona struna>` | lekcja przechodzi do kroku 1 | `wynik HIT`, `Lekcja: krok 1` |
| 3 | `s <inna struna>` | krok bez zmian | `wynik MISS` |
| 4 | `l <nuta bieżącego kroku> 1` | krok bez zmian | `Lekcja: krok 1` ponownie, `LED:` z nową maską (albo pustą, gdy dźwięku nie da się zagrać) |
| 5 | `l <ta sama nuta> 0` | krok bez zmian | `Lekcja: krok 1`, `LED:` jak w pkt 2 |
| 6 | „Wstrzymaj lekcję” | przycisk zmienia się na „Wznów lekcję” | `Lekcja: koniec`, `LED:` pusta |
| 7 | `s <dowolna struna>` | krok bez zmian (zagranie ignorowane) | brak `LESSON_RESULT` w `RX` |
| 8 | `d`, potem ponowne połączenie | lekcja nadal wstrzymana | `HELLO`, ale bez `Lekcja: start` |
| 9 | „Wznów lekcję” | lekcja trwa od tego samego kroku | `Lekcja: start`, `Lekcja: krok 1` |
| 10 | `d` | baner „Brak połączenia z harfą”, przycisk pauzy nieaktywny | rozłączenie, reklamowanie |
| 11 | Ponowne połączenie | baner znika, lekcja trwa od tego samego kroku | `HELLO`, `Lekcja: start`, `Lekcja: krok 1` |
| 12 | Graj podświetlone struny do końca | okno „Gratulacje!” | `wynik COMPLETE`, `LED:` pusta |
| 13 | „Zagraj ponownie” | krok 0 | `Lekcja: start`, `Lekcja: krok 0` |
| 14 | „Zatrzymaj tryb nauki” | tryb nauki wyłączony | `Lekcja: koniec` |
| 15 | Rozłącz, włącz tryb nauki, połącz | najpierw baner, po połączeniu krok 0 | po `HELLO`: `Lekcja: start`, `Lekcja: krok 0` |

Mock zawsze zgłasza `CAP_LESSON`, więc zachowania harfy z samymi LED-ami (`LED_SET` zamiast
`LESSON_*`) nie da się nim sprawdzić bez zmiany `CAPS` w `mock_harp.ino`. Obejmują je testy
jednostkowe `InstrumentLessonControllerTest`.

## Znane ograniczenia

- Mock nie odtwarza dźwięku i nie ma diod. Stan LED widać tylko w Serial Monitorze.
- `BATTERY_STATE` jest wysyłany tylko na żądanie (`b`, handshake, `STATE_REQUEST`). Mock nie
  symuluje rozładowywania baterii w czasie.