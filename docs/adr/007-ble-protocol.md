# ADR 007: Wersjonowany dwukierunkowy protokół komunikacji BLE

* **Stan:** Przyjęty, uzupełniony przez [ADR 010](010-ble-session-id-and-pause.md) (protokół 2.0: identyfikator sesji w nagłówku, `LESSON_PAUSE`)
* **Data:** 2026-11-03
* **Osoby decyzyjne:** Emilia Bonk

## Kontekst
Prototyp komunikuje się z aplikacją jednostronnie: mock harfy (ESP32, Bluetooth Classic SPP) wysyła pojedyncze litery nut (`"F\n"`), a aplikacja wybiera urządzenie po zahardkodowanym adresie MAC. Takie rozwiązanie nie wystarcza docelowemu systemowi:
* ADR 003 przyjmuje STM32WB55 z BLE 5.2 – profil SPP nie jest dostępny.
* Harfa ma 15 strun, 7 przełączników i transpozycję oktawową ([mapowanie strun](../note-mapping.md)), więc sama litera nie identyfikuje dźwięku.
* Aplikacja musi sterować diodami LED w trybie nauki, a harfa raportować baterię, błędy i aktualne strojenie ([specyfikacja](../specification.md)).
* Aplikacja i firmware będą rozwijane niezależnie, więc potrzebny jest kontrakt z jawną wersją, zasadami kompatybilności i testami po obu stronach.

Decyzja obejmuje trzy powiązane kwestie: format wiadomości, organizację usługi GATT oraz podział odpowiedzialności za logikę lekcji.

## Rozważane alternatywy

### 1. Format wiadomości

#### Opcja A: Binarne ramki ze stałym nagłówkiem
* **Zalety:**
  * Małe ramki (zdarzenie struny: 9 bajtów), wszystkie wiadomości mieszczą się w 20 bajtach domyślnego ATT_MTU – brak fragmentacji.
  * Trywialne, deterministyczne parsowanie w C bez alokacji pamięci na mikrokontrolerze.
  * Jednoznaczna reprezentacja – ramkę można zapisać jako wektor testowy w hex i porównywać bajt po bajcie.
* **Wady:**
  * Ramki nie są czytelne „na oko” w narzędziach typu nRF Connect.
  * Ewolucję schematu trzeba zapewnić własnymi regułami.

#### Opcja B: Tekst / JSON
* **Zalety:**
  * Czytelne logi i łatwe ręczne testowanie.
* **Wady:**
  * Ramki kilkukrotnie większe, część wiadomości przekracza 20 bajtów.
  * Parser JSON w firmware i niejednoznaczności zapisu (spacje, kolejność pól, liczby) utrudniają testy kontraktowe.


### 2. Organizacja GATT

#### Opcja A: Jedna dedykowana usługa z charakterystykami RX i TX
* **Zalety:**
  * Jeden kanał, jeden format, jeden kodek i jeden zestaw testów kontraktowych.
  * UUID usługi w advertisingu pozwala rozpoznać harfę bez adresu MAC.
* **Wady:**
  * Bateria i wersja firmware nie są widoczne w standardowych narzędziach BLE.

#### Opcja B: Dedykowana usługa + standardowe Battery Service i Device Information Service
* **Zalety:**
  * Standardowe informacje dostępne „od ręki” w każdej aplikacji BLE.
* **Wady:**
  * Część kontraktu poza wspólnym formatem – osobna obsługa i osobne testy.
  * Brak wspólnej numeracji sekwencyjnej i kolejności względem pozostałych zdarzeń.

## Decyzja
Wybrano:
* **Format:** **Opcja 1A** – binarne ramki `ver | type | seq | len | payload`, little-endian, maksymalnie 20 bajtów.
* **GATT:** **Opcja 2A** – jedna usługa HARFA z charakterystykami RX (zapis) i TX (notyfikacje). Identyfikacja urządzenia przez UUID usługi zamiast adresu MAC.

Dodatkowo przyjęto:
* Wersjonowanie `major.minor`: `major` w każdej ramce, `minor` negocjowany w handshake. Zmiany zgodne wstecz polegają wyłącznie na dodawaniu typów wiadomości i dopisywaniu pól na końcu payloadu.
* Potwierdzenia `ACK` dla komend aplikacji (stop-and-wait, retransmisja z tym samym `seq`), zdarzenia harfy bez potwierdzeń.
* Brak parowania w wersji 1.0.
* Wspólny plik wektorów testowych jako normatywna część kontraktu, testowany przez obie implementacje.

Pełna definicja protokołu znajduje się w [docs/protocol/ble-protocol.md](../protocol/ble-protocol.md).

## Konsekwencje
* Specyfikacja `docs/protocol/ble-protocol.md` staje się jedynym źródłem prawdy dla komunikacji. Definicja ramki konfiguracyjnej z note-mapping §6 jest częścią wiadomości `CONFIG_STATE`.
* Należy przygotować `docs/protocol/test-vectors.json` oraz kodeki z testami kontraktowymi w aplikacji (Kotlin) i firmware (C, testy uruchamiane na komputerze).
* Aplikacja musi przejść z Bluetooth Classic (SPP, adres MAC) na BLE GATT ze skanowaniem po UUID usługi – osobne zadanie.
* Mock harfy (ESP32) musi zostać przepisany na BLE i nowy protokół – osobne zadanie.
* Logika nauki w aplikacji musi porównywać efektywne numery MIDI zamiast liter nut.
* Wszystkie wiadomości wersji 1 mieszczą się w 20 bajtach. Większe wiadomości (np. przesyłanie utworów) wymagają mechanizmu fragmentacji w przyszłej wersji.
* Połączenie jest nieszyfrowane. Wprowadzenie bondingu wymaga nowej wersji protokołu.
