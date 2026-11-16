# ADR 009: Model domenowy lekcji, nut i zdarzeń instrumentu w aplikacji

* **Stan:** Przyjęty
* **Data:** 2026-11-04
* **Osoby decyzyjne:** Emilia Bonk

## Kontekst
Aplikacja mobilna operuje na surowych napisach. Nuta w katalogu to `String` (`"G#4"`). Tryb nauki porównuje tylko **pierwszą literę** nuty oczekiwanej z literą odebraną z mocka SPP. Wynikają z tego problemy:
* `G4`, `G#4` i `G5` są nierozróżnialne, więc zagranie złego dźwięku zalicza krok.
* Pusta nuta w katalogu powodowała awarię aplikacji, bo pobranie pierwszego znaku z pustego napisu zgłaszało wyjątek. Po wydzieleniu warstw ([ADR 008](008-mobile-app-layered-architecture.md)) wyjątek już nie występuje, ale lekcja zatrzymuje się na takim kroku bez żadnej informacji dla użytkownika.
* Dane katalogu nie są sprawdzane. Utwór *Hallelujah* zawiera `D5`, `E5` i `F#5`, a te dźwięki leżą poza zakresem harfy (`B0..C#5`, [note-mapping §5](../note-mapping.md)).
* [ADR 007](007-ble-protocol.md) przyjmuje, że harfa raportuje `STRING_EVENT(string_id, midi)`, a logika nauki musi porównywać efektywne numery MIDI. Pojęcia „struna” i „dźwięk” trzeba więc rozdzielić.
* [ADR 008](008-mobile-app-layered-architecture.md) wydzielił warstwę `domain`, ale przeniósł do niej dotychczasową reprezentację napisową: zdarzenie `NotePlayed(note: String)` i przypadek użycia `EvaluateNoteUseCase`, który porównuje napisy.

Decyzja obejmuje trzy kwestie: reprezentację nuty i porównywanie, postępowanie z niepoprawnym katalogiem oraz stary format zdarzeń z mocka.

## Rozważane alternatywy

### 1. Reprezentacja nuty i porównywanie

#### Opcja A: Typ `Pitch` (nazwa + znak + oktawa), porównywanie po MIDI
* **Zalety:**
  * Zachowuje zapis z katalogu (`Ab4` lub `G#4`) do prezentacji.
  * Porównanie z `STRING_EVENT.midi` jest bezpośrednie i jednoznaczne.
  * Zgodne z note-mapping §4, które czyni efektywne MIDI identyfikatorem dźwięku.
* **Wady:**
  * Dwa pojęcia równości: równość pisowni i enharmonia (to samo MIDI).

#### Opcja B: Sam numer MIDI jako nuta
* **Zalety:**
  * Najprostsza implementacja.
* **Wady:**
  * Utrata pisowni z katalogu (nie odróżnimy `Ab4` od `G#4` w UI).
  * Brak walidacji „czy zapis jest poprawną nutą”, bo każda liczba wygląda poprawnie.

### 2. Niepoprawne dane katalogu

#### Opcja A: Blokada lekcji z informacją, utwór zostaje w katalogu
* **Zalety:**
  * Brak awarii i brak „cichych” błędów: użytkownik widzi, które nuty są złe.
  * Odsłuch MIDI dalej działa.
  * Dane źródłowe pozostają nienaruszone.
* **Wady:**
  * Utworu z błędem nie da się ćwiczyć do czasu poprawy danych.

#### Opcja B: Pominięcie niepoprawnych kroków
* **Zalety:**
  * Każdy utwór da się ćwiczyć.
* **Wady:**
  * Lekcja nie odpowiada utworowi, a błędy w danych pozostają niezauważone.

#### Opcja C: Automatyczna transpozycja do zakresu harfy
* **Zalety:**
  * Więcej utworów nadaje się do ćwiczenia.
* **Wady:**
  * Zmienia melodię bez wiedzy użytkownika i ukrywa błędy w danych.

### 3. Stary format zdarzeń (litera przez SPP)

#### Opcja A: Tymczasowy adapter litery na zdarzenie
* **Zalety:**
  * Tryb nauki działa ze starym mockiem do czasu przejścia na BLE.
* **Wady:**
  * Litera nie niesie oktawy ani struny, więc adapter musiałby zgadywać dźwięk. Wróciłaby niejednoznaczność, którą ta decyzja usuwa.

#### Opcja B: Rezygnacja z porównywania po literze
* **Zalety:**
  * Jedno źródło zdarzeń (`NoteEvent` z `STRING_EVENT`) i jednoznaczna ocena.
* **Wady:**
  * Do czasu przejścia aplikacji i mocka na BLE tryb nauki nie reaguje na mock.

## Decyzja
Wybrano:
* **Reprezentacja:** **Opcja 1A**. Osobne typy `StringId` (struna `0..14`) i `Pitch` (`NoteName` + `Accidental` + oktawa, z wyliczanym MIDI). Krok lekcji jest trafiony, gdy `NoteEvent.midi == step.pitch.midi`.
* **Katalog:** **Opcja 2A**. Walidacja przed rozpoczęciem lekcji zwraca poprawną `Lesson` albo listę wszystkich problemów (`NO_STEPS`, `EMPTY`, `MALFORMED`, `OUT_OF_RANGE`). Utwór z problemami zostaje w katalogu bez trybu nauki. `songs.json` nie jest automatycznie zmieniany.
* **Stary format:** **Opcja 3B**. Tryb nauki przyjmuje wyłącznie `NoteEvent`.

Dodatkowo przyjęto:
* Model domenowy (`StringId`, `NoteName`, `Accidental`, `Pitch`, `InstrumentState`, `NoteEvent`, `Lesson`, `LessonStep`, `LessonProgress`) jest niezależny od Androida, Rooma i BLE, więc testują go testy jednostkowe.
* Parsowanie, walidacja i odwzorowanie ramek BLE na model nie zgłaszają wyjątków. Niepoprawne dane dają wynik błędu albo „brak wartości”.
* Struktura katalogu (`songs.json`, encja Room `SongEntity` i model `Song` z listą `LyricNote`) pozostaje bez zmian i pełni rolę danych wejściowych. Nie jest wymagana migracja bazy.
* Elementy rozmieszczono zgodnie z warstwami z [ADR 008](008-mobile-app-layered-architecture.md):
  * `domain`: model, walidator katalogu, ocena kroku (`LessonProgress`) i przypadek użycia przygotowujący lekcję z utworu;
  * `device`: odwzorowanie ramek protokołu (`STRING_EVENT`, `CONFIG_STATE`) na `NoteEvent` i `InstrumentState`. `InstrumentRepository` udostępnia strumienie tych typów;
  * `presentation`: ViewModel przechowuje bieżący `LessonProgress` i prezentuje błędy walidacji.
* Katalog **nie przechowuje danych wyliczanych** z nuty (numer struny, stopień gamy, MIDI). Struna zależy od bieżącego strojenia harfy, więc zapisana wartość byłaby poprawna tylko dla jednego ustawienia. Do tego powstałyby dwa źródła prawdy. Wyliczenie odbywa się raz, przy walidacji.
* Odłożono na osobne zadanie opcjonalne pole na poziomie utworu, `harpSetup` (zalecana transpozycja i pozycje przełączników). Pole opisuje decyzję autora lekcji, a nie dane wyliczane. Wymaga migracji bazy.

Pełna definicja modelu znajduje się w [docs/domain-model.md](../domain-model.md).

## Konsekwencje
* `docs/domain-model.md` jest źródłem prawdy dla pojęć domenowych aplikacji. Wartości liczbowe (tabela strun, zakres, wzór MIDI) pozostają w [note-mapping](../note-mapping.md).
* Ocena zagrania w aplikacji jest zgodna z kontraktem BLE: porównywane jest autorytatywne `midi` harfy.
* `NotePlayed`, `EvaluateNoteUseCase` i kodek liter `NoteEventCodec` z [ADR 008](008-mobile-app-layered-architecture.md) zostają usunięte. Zastępują je `NoteEvent`, `LessonProgress` i odwzorowanie ramek protokołu.
* *Hallelujah* nie będzie dostępne w trybie nauki, dopóki jego dane nie zostaną poprawione (osobne zadanie).
* Tryb nauki nie reaguje na stary mock SPP. Zdarzenia dostarcza klient BLE GATT (ADR 007), a mock harfy musi wysyłać ramki protokołu przez BLE.
* `LessonProgress` ze stanem `PAUSED` realizuje zachowanie po utracie BLE z [ADR 001](001-system-responsibilities.md). Wynik kroku odpowiada `LESSON_RESULT` z protokołu.

## Aktualizacja (HARFA-39)

* Odłożone pole `harpSetup` zrealizowano jako katalog strojeń `assets/tunings.json` (`Tuning`: nazwa, typ i pozycje przełączników) oraz pole `Song.tuningId`. Strojenie nie zawiera oktawy, bo transpozycję dobiera analiza na podstawie nut utworu. Zgodnie z [ADR 010](010-catalog-versioning-and-user-state.md) katalog jest tylko do odczytu, więc migracja bazy nie była potrzebna.
* Bieżące strojenie harfy jest porównywane z utworem przez `AnalyzeTuningUseCase` ([domain-model §3.4](../domain-model.md)). Walidacja katalogu (§6) nadal nie sprawdza strojenia.
* Dane *Hallelujah* poprawiono: utwór jest w tonacji pliku MIDI, obniżony o oktawę, i jest dostępny w trybie nauki.
