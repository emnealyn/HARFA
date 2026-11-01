# ADR 006: Wybór sposobu zasilania, ogniwa Li-Ion oraz modułu zarządzania energią

* **Stan:** Przyjęty
* **Data:** 2026-10-31
* **Osoby decyzyjne:** Joanna Walczak, Emilia Bonk

## Kontekst
Urządzenie HARFA wymaga mobilnego zasilania akumulatorowego do obsługi mikrokontrolera STM32WB55, czujników optycznych (dioda laserowa + fototranzystor), wyświetlacza OLED, diod LED oraz toru audio z głośnikiem Visaton 4 Ohm.

## Rozważane alternatywy
### Opcja A: Zasilanie wyłącznie z portu USB-C (Brak akumulatora)
* **Zalety:**
  * Prosty sprawdzony już w prototypie schemat.
  * Brak konieczności stosowania układu BMS (Battery Management System) i ogniw Li-Ion.
  * Mniejsza waga i gabaryty instrumentu.
* **Wady:**
  * Brak mobilności – konieczność stałego podłączenia do powerbanku lub ładowarki sieciowej.

### Opcja B: Dedykowane ogniwo Samsung INR18650-35E (3400mAh) + Moduł zarządzania Waveshare 18306 ~120zł
* **Zalety:**
  * **Pełna mobilność i wysoka pojemność:** Sprawdzone, markowe ogniwo 3.7V o dużej pojemności 3400mAh zapewnia długi czas pracy instrumentu.
  * **Kompletne zarządzanie zasilaniem:** Dedykowany moduł Waveshare 18306 integruje w sobie ładowarkę USB, zabezpieczenia ogniwa (przed głębokim rozładowaniem/przeładowaniem) oraz regulację napięć zasilających układ.
* **Wady:**
  * Ogniwa oraz moduł Waveshare kosztują znacząco więcej niż pozostałe opcje.

### Opcja C: Zasilanie tradycyjnymi bateriami / akumulatorkami AA (4x AA)
* **Zalety:** 
  * Łatwa wymiana baterii przez użytkownika w przypadku rozładowania.
* **Wady:** 
  * Niestabilne napięcie w miarę rozładowywania baterii (spadki napięcia powodujące reset MCU lub zakłócenia w dźwięku) wymagany dodatkowo PowrPath i układy zabezpieczające.
  * Szybkie zużycie przy poborze prądu przez głośnik i diody laserowe/LED.

## Decyzja
Wybrano **Opcję B (ogniwo Samsung INR18650-35E (3400mAh) + Moduł zarządzania Waveshare 18306)**.

Rozwiązanie to zapewnia pełną bezprzewodową mobilność instrumentu, bezpieczne ładowanie przez USB-C oraz ochronę ogniwa Li-Ion bez konieczności skomplikowanego projektowania toru zasilania.

## Konsekwencje
* W oficjalnym zestawieniu komponentów umieszczone zostają ognioSamsung INR18650-35E 3400mAh oraz moduł Waveshare 18306.
* Rezygnujemy z jakichkolwiek osobnych zewnętrznych przetwornic Step-Up/LDO, polegając na sekcji zasilania zapewnianej przez Waveshare 18306.