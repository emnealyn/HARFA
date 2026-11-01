# ADR 003: Wybór Mikrokontrolera (MCU) oraz integracja Bluetooth (BLE)

* **Stan:** Przyjęty
* **Data:** 2026-10-31
* **Osoby decyzyjne:** Emilia Bonk

## Kontekst
Projekt wymaga mikrokontrolera do sterowania 15 detektorami wiązki, 15 diodami LED, syntezą dźwięku i odtwarzaniem audio oraz komunikacją bezprzewodową z aplikacją mobilną przez Bluetooth Low Energy.

## Rozważane alternatywy

### Opcja A: STM32WB55 (Zintegrowane BLE) ~260 zł
* **Zalety:**
  * Wbudowany kontroler BLE (Bluetooth 5.2, dwurdzeniowa architektura ARM Cortex-M4 + Cortex-M0+).
  * Prostszy projekt układu (brakuje konieczności osobnego modułu BLE).
  * Niższy całkowity pobór mocy.
* **Wady:**
  * Mniejsza moc obliczeniowa w porównaniu do serii H7.
  * Znacząco wyższa cena w porównaniu do ESP-32-S3.

### Opcja B: STM32H7 + Zewnętrzny moduł BLE (np. ESP32 / NRF52) ~180zł + ~30zł
* **Zalety:**
  * Bardzo wysoka wydajność obliczeniowa (Cortex-M7 @ 480 MHz), bardzo dużo pamięci RAM/Flash.
  * Łatwość realizacji złożonej syntezy dźwięku cyfrowego.
* **Wady:**
  * Konieczność projektowania i obsługi szyny komunikacyjnej (UART/SPI) między STM32H7 a modułem BLE.
  * Bardziej skomplikowany firmware.

### Opcja C: ESP32-S3-DEV-KIT-N8R8 (Zintegrowane BLE) ~50zł
* **Zalety:**
  * Bardzo wysoka moc obliczeniowa (Dual-Core 240 MHz LX7 z rozszerzeniami DSP do przetwarzania audio).
  * Duży zapas pamięci (8 MB Flash + 8 MB PSRAM) na próbki WAV i bufory.
  * Wbudowane Wi-Fi oraz Bluetooth 5 (LE).
  * Niska cena modułu deweloperskiego i łatwy rozwój oprogramowania.
* **Analiza problemu ADC:**
  * Możliwe że podobnie jak w ESP32-WROOM (płytce prototypowej), w ESP32-S3 blok **ADC2** jest współdzielony z Wi-Fi/BLE i podatny na zakłócenia.
  * **Rozwiązanie:** ESP32-S3 udostępnia **ADC1 (10 kanałów)**, który działa w pełni niezależnie od BLE. Zastosowanie cyfrowych czujników lub ekspandera/multipleksera podłączonego wyłącznie do magistrali I2C lub pinów ADC1 eliminuje zakłócenia od transmisji radiowej.

## Decyzja
Wybrano **Opcję A (STM32WB55)** do docelowej produkcji, ze względu na przemysłowy standard i gwarantowaną stabilność magistral analogowo-cyfrowych bez konieczności omijania ograniczeń bloku ADC2.

Moc obliczeniowa Cortex-M4 jest wystarczająca dla wymaganych zadań. Zintegrowany rdzeń BLE znacznie upraszcza architekturę sprzętową i oprogramowanie układowe.

## Konsekwencje
* Docelowym mikrokontrolerem zostaje układ STM32WB55, należy uwzględnić jego wymiary w projekcie 3D oraz projekcie układu elektroniki.
* Protokół BLE realizowany jest bezpośrednio w oprogramowaniu układowym.