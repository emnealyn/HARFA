# ADR 010: Identyfikator sesji w nagłówku ramki BLE i komenda wstrzymania lekcji

* **Stan:** Przyjęty
* **Data:** 2026-11-12
* **Osoby decyzyjne:** Emilia Bonk, Alicja Kowalska

## Kontekst
[ADR 001](001-system-responsibilities.md) wymaga, aby komendy lekcji z poprzedniej sesji nie zmieniały stanu LED po ponownym połączeniu, i wymienia identyfikator sesji jako element kontraktu. Protokół 1.0 ([ADR 007](007-ble-protocol.md)) nie miał takiego pola: jego rolę miało pełnić samo połączenie BLE, bo ramka z poprzedniego połączenia nie może zostać dostarczona w nowym.

Code review HARFA-14 pokazało, że to założenie jest za słabe. Kontroler lekcji w aplikacji kolejkuje sygnały (`LESSON_STEP`, `LESSON_RESULT`, `LESSON_END`) i obsługuje je asynchronicznie. Jeśli połączenie zerwie się i wróci, zanim kolejka zostanie opróżniona, komenda przygotowana w poprzedniej sesji zostanie wysłana już w nowej. Ramka jest wtedy nowa, więc połączenie BLE niczego nie chroni.

Drugi problem dotyczy pauzy. Protokół 1.0 nie miał komendy wstrzymania, więc aplikacja wysyłała `LESSON_END`. Harfa traciła stan lekcji, a wznowienie wymagało ponownego `LESSON_START` i bieżącego kroku. Dla harfy pauza i zakończenie były nierozróżnialne.

Aplikacja ani firmware nie zostały jeszcze wydane, więc zmiana niezgodna wstecz nie dotyka żadnych użytkowników.

## Rozważane alternatywy

### 1. Miejsce identyfikatora sesji

#### Opcja A: `sid` w nagłówku ramki (protokół 2.0)
* **Zalety:**
  * Jedno miejsce dla wszystkich wiadomości, bez wyjątków dla poszczególnych typów.
  * Walidacja `sid` w warstwie sesji niezależnie od typu wiadomości, także dla retransmisji.
  * Proste dekodowanie: offset pola jest stały.
* **Wady:**
  * Zmiana niezgodna z 1.0 (zmiana `major`).
  * Payload maleje z 16 do 15 bajtów. Największy obecny payload (`DEVICE_INFO`) ma 13 bajtów.

#### Opcja B: `sid` jako ostatnie pole payloadu (protokół 1.1)
* **Zalety:**
  * Zmiana zgodna wstecz według reguł z sekcji 7 specyfikacji.
* **Wady:**
  * Offset pola zależy od typu wiadomości.
  * `LESSON_END` dostaje payload tylko dla tego pola.
  * Każde przyszłe pole dopisywane na końcu payloadu musi trafić za `sid`.
  * Zgodność wstecz nie ma wartości, bo nic nie zostało wydane.

### 2. Komenda wstrzymania

#### Opcja A: Jedna komenda `LESSON_PAUSE` z polem `paused`
* **Zalety:**
  * Ustawia stan absolutny, więc jest idempotentna (jak `LED_SET`).
  * Zajmuje jeden kod typu.
* **Wady:**
  * Nazwa komendy wznawiającej jest mniej oczywista.

#### Opcja B: Para `LESSON_PAUSE` / `LESSON_RESUME`
* **Zalety:**
  * Czytelne nazwy.
* **Wady:**
  * Dwa kody typu i dwie ścieżki walidacji dla jednego stanu.

## Decyzja
Wybrano **opcję 1A** i **opcję 2A**: protokół 2.0 z identyfikatorem sesji w nagłówku oraz komendę `LESSON_PAUSE`.

* Nagłówek ma 5 bajtów: `ver | type | seq | sid | len`, `ver = 0x02`, payload maks. 15 bajtów, ramka maks. 20 bajtów.
* `sid` nadaje aplikacja w `HELLO` (wartość różna od `0`). Odpowiedzi kopiują `sid` ramki, na którą odpowiadają, a zdarzenia niosą `sid` bieżącej sesji.
* Harfa odpowiada `ACK(STALE_SESSION)` (`0x08`) na komendę z innym `sid` i jej nie wykonuje. Aplikacja odrzuca ramki z innym `sid`.
* Bajt `ver` na offsecie 0 jest zamrożony we wszystkich wersjach. Od 2.0 zamrożony jest też format `HELLO` i `ERROR`.
* `LESSON_PAUSE` (`0x35`, `paused`: `1` = wstrzymaj, `0` = wznów) zachowuje lekcję i bieżący krok, gasi diody na czas pauzy i przywraca je po wznowieniu.
* `SessionId` w warstwie aplikacji jest lokalną, monotoniczną generacją połączenia i nie jest wartością 8-bitowego `sid`. Dzięki temu stara komenda pozostaje nieaktualna także po zawinięciu `sid`.

Szczegóły są w [specyfikacji protokołu](../protocol/ble-protocol.md) (sekcje 2, 4.10, 6, 8.1).

## Konsekwencje
* Wszystkie wektory testowe, kodeki (C i Kotlin), mock harfy i dokumentacja przechodzą na format 2.0. Buildy deweloperskie z 1.0 dostają `UNSUPPORTED_VERSION` zamiast błędnie parsować ramki.
* Sam bajt w ramce nie wystarczy: aplikacja musi przypisać identyfikator sesji do sygnału lekcji w chwili jego utworzenia i odrzucić go, jeśli sesja się zmieniła (warstwa domain, HARFA-42 etap B).
* Pauza nie przetrwa zerwania połączenia. Wznowienie w nowej sesji nadal wymaga `LESSON_START` i bieżącego `LESSON_STEP` (spec sekcja 8, pkt 9).
* Zasady kompatybilności `minor` z sekcji 7 specyfikacji obowiązują dalej, licząc od wersji 2.0.
