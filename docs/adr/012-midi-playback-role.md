# ADR 012: Rola odtwarzania MIDI w aplikacji mobilnej

* **Stan:** Przyjęty
* **Data:** 2026-11-18
* **Osoby decyzyjne:** Alicja Kowalska

## Kontekst

System HARFA ma dwa niezależne środowiska audio:

* STM32WB55 wykrywa przerwanie wiązki i lokalnie generuje dźwięk harfy,
* aplikacja mobilna może odtwarzać plik MIDI przypisany do utworu.

Dotychczasowe określenie funkcji jako „odtwarzanie MIDI” nie wyjaśnia, czy telefon
ma symulować dźwięk instrumentu, prezentować utwór, czy odtwarzać dodatkową
ścieżkę podczas nauki. Takie niedoprecyzowanie mogłoby prowadzić do automatycznego
uruchamiania dźwięku w aplikacji i powstania dwóch niekontrolowanych ścieżek
audio.

## Relacja do wcześniejszych decyzji

Niniejszy ADR **rozszerza i uszczegóławia**:

* [ADR 001: Granice odpowiedzialności STM32WB55 i aplikacji mobilnej](001-system-responsibilities.md)
  o rolę lokalnego odtwarzania referencyjnego w aplikacji. Nie zmienia decyzji,
  że STM32WB55 jest właścicielem generowania dźwięku instrumentu w czasie
  rzeczywistym.
* [ADR 004: Wybór architektury toru audio](004-audio-architecture.md)
  o rozróżnienie pomiędzy sprzętowym torem audio harfy a audio odtwarzanym
  przez telefon. Nie zmienia wybranej architektury WAV/I2S + MAX98357A.

W razie niejednoznaczności niniejszy ADR jest źródłem prawdy wyłącznie dla roli
odtwarzania MIDI w aplikacji mobilnej. Nie nadpisuje odpowiedzialności STM32 ani
architektury sprzętowego toru audio określonych w powyższych ADR-ach.

## Rozważane alternatywy

### Opcja A: MIDI jako podgląd utworu

Telefon odtwarza utwór wyłącznie jako odsłuch przed rozpoczęciem lekcji.

**Zalety:**

* brak równoczesnego audio telefonu i instrumentu podczas lekcji,
* proste rozdzielenie podglądu od właściwej gry.

**Wady:**

* użytkownik nie może korzystać ze ścieżki jako odniesienia podczas nauki,
* funkcja jest mniej użyteczna przy ćwiczeniu rytmu i kolejności nut.

### Opcja B: MIDI jako ścieżka referencyjna

Telefon odtwarza utwór na żądanie użytkownika jako dodatkowe odniesienie.
Ścieżka może być odtwarzana także podczas aktywnej lekcji.

**Zalety:**

* użytkownik może świadomie korzystać z odniesienia podczas gry,
* dźwięk harfy nadal powstaje lokalnie w STM32,
* odtwarzanie pozostaje użyteczne bez połączenia z harfą.

**Wady:**

* podczas lekcji mogą występować dwa źródła dźwięku: telefon i harfa,
* UI i cykl życia odtwarzacza muszą jasno komunikować, że jest to opcjonalna
  ścieżka referencyjna, a nie dźwięk instrumentu.

### Opcja C: Usunięcie odtwarzania MIDI

Pliki MIDI i funkcja ich odtwarzania zostają usunięte z aplikacji.

**Zalety:**

* brak ryzyka pomylenia źródła dźwięku,
* prostszy cykl życia ekranu utworu.

**Wady:**

* użytkownik traci możliwość odsłuchu utworu i korzystania z odniesienia,
* aplikacja pozostaje bez lokalnego audio wspierającego naukę.

## Decyzja

Wybrano **Opcję B: MIDI jako ścieżka referencyjna**.

Ścieżka referencyjna:

1. jest uruchamiana wyłącznie po świadomym działaniu użytkownika,
2. może być odtwarzana podczas aktywnej lekcji,
3. nie generuje dźwięku pojedynczych strun i nie symuluje harfy czasu
   rzeczywistego,
4. nie steruje postępem lekcji i nie jest wysyłana do STM32 przez BLE,
5. może działać bez połączenia z harfą,
6. jest zatrzymywana i zwalniana przy zmianie utworu, opuszczeniu sesji utworu
   albo zniszczeniu odtwarzacza.

W UI funkcja jest nazywana **ścieżką referencyjną**, a nie „dźwiękiem harfy”.
Opis funkcji musi wyjaśniać, że dźwięk instrumentu generuje STM32.

Równoczesne odtwarzanie ścieżki referencyjnej przez telefon i dźwięku harfy
przez STM32 jest dozwolone tylko jako skutek jawnego uruchomienia ścieżki przez
użytkownika. Aplikacja nie uruchamia jej automatycznie po połączeniu BLE,
rozpoczęciu lekcji ani odebraniu zdarzenia zagrania nuty.

## Wymagania implementacyjne

* Przygotowanie odtwarzacza nie może blokować wątku UI.
* Odtwarzacz musi obsługiwać Android audio focus i zwalniać go, gdy ścieżka
  przestaje być odtwarzana.
* Utrata focusu audio nie może zmienić stanu lekcji ani zatrzymać generowania
  dźwięku przez STM32.
* W aplikacji może być aktywna najwyżej jedna ścieżka referencyjna dla bieżącego
  utworu.
* Błąd przygotowania lub odtwarzania musi być widoczny jako jawny stan funkcji,
  a nie jako ciche nieudane kliknięcie.

## Konsekwencje

* STM32WB55 pozostaje jedynym źródłem dźwięku harfy czasu rzeczywistego.
* Aplikacja ma opcjonalne, lokalne audio edukacyjne, ale nie znajduje się ono
  w krytycznej ścieżce działania instrumentu.
* Dwa dźwięki podczas lekcji są rozróżnialne koncepcyjnie: ścieżka referencyjna
  jest świadomie włączonym odniesieniem, a dźwięk instrumentu pochodzi z harfy.
* Nie są wymagane zmiany firmware'u ani protokołu BLE.
* Nazwa pola katalogu przechowującego ścieżkę może pozostać techniczna
  (`midiFilePath`), ponieważ opisuje format pliku; nazwa widoczna dla użytkownika
  musi opisywać jego rolę jako ścieżki referencyjnej.
