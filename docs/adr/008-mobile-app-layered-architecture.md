# ADR 008: Warstwowa architektura aplikacji mobilnej

* **Stan:** Przyjęty
* **Data:** 2026-11-04
* **Osoby decyzyjne:** Alicja Kowalska

## Kontekst

Aplikacja HARPApp obsługuje katalog utworów, tryb nauki, ustawienia, odtwarzanie
audio oraz komunikację z instrumentem. Wcześniej większość tej logiki znajdowała
się bezpośrednio w ViewModelach.

ViewModel wiedział jednocześnie:

* jak wygląda interfejs użytkownika,
* jak pobierać dane z Room,
* jak zapisywać ustawienia w DataStore,
* jak odtwarzać pliki audio,
* jak łączyć się z Bluetooth,
* jak interpretować komunikaty z instrumentu,
* jak oceniać poprawność zagranej nuty.

Takie rozwiązanie było wystarczające dla prototypu, ale utrudniało rozwój
i testowanie aplikacji. ViewModel miał zbyt wiele odpowiedzialności i znał
szczegóły techniczne źródeł danych. Na przykład wiedział, że utwory są
przechowywane w Room, zamiast korzystać tylko z operacji potrzebnej do pobrania
utworów.

Potrzebny był podział, w którym każda część aplikacji ma jasno określoną
odpowiedzialność, a logika domenowa nie zależy od Androida ani od konkretnej
technologii przechowywania danych.

## Rozważane alternatywy

### Opcja A: Dotychczasowa architektura oparta na ViewModelach

#### Organizacja

Wcześniej przepływ informacji wyglądał w uproszczeniu następująco:

```text
Compose
   ↓
ViewModel
   ├── Room / DAO
   ├── DataStore
   ├── MediaPlayer i assets
   ├── Android Bluetooth API
   └── logika aplikacji
```

ViewModel był punktem, w którym łączyły się interfejs użytkownika, dane,
komunikacja z urządzeniem i logika trybu nauki.

Przykładowo `SongViewModel` sam pobierał utwór, odtwarzał audio, odbierał
komunikaty Bluetooth i porównywał zagraną nutę z nutą oczekiwaną przez lekcję.
`HomeViewModel` korzystał bezpośrednio z Room, a `BluetoothViewModel` znał
Android Bluetooth API.

#### Zalety

* mała liczba plików i szybkie rozpoczęcie pracy nad prototypem,
* bezpośredni przepływ od ekranu do konkretnej implementacji,
* brak dodatkowych interfejsów i mapowania modeli.

#### Wady

* ViewModele miały zbyt wiele odpowiedzialności,
* logika była trudna do testowania bez Androida, Room i Bluetooth,
* zmiana sposobu przechowywania danych wymagała zmian w ViewModelach,
* warstwa prezentacji znała szczegóły infrastruktury,
* modele używane przez UI i Room były ze sobą połączone,
* kod obsługi urządzenia i kod aplikacji były trudniejsze do rozdzielenia,
* błędy w jednej odpowiedzialności mogły wpływać na pozostałą część ViewModelu.

### Opcja B: Podział na warstwy `presentation`, `domain`, `data` i `device`

W aktualnej architekturze aplikacja jest podzielona na cztery główne warstwy.
Warstwy komunikują się przez jasno określone interfejsy i modele.

```text
                    presentation
                 Compose i ViewModele
                          ↓
                 przypadki użycia
                          ↓
                       domain
          modele i interfejsy repozytoriów
                    ↙             ↘
                 data             device
          Room, DataStore,       Bluetooth
          assets, audio,         i kodek
          mapowanie
```

#### Warstwa `presentation`

Warstwa prezentacji zawiera:

* ekrany Compose,
* komponenty interfejsu,
* nawigację,
* ViewModele,
* stan prezentowany użytkownikowi,
* fabryki ViewModeli.

ViewModel zarządza stanem ekranu i reaguje na działania użytkownika. Nie zna
jednak szczegółów technicznych Room, DataStore ani Android Bluetooth API.

Zamiast pobierać dane bezpośrednio, ViewModel korzysta z przypadków użycia
albo z interfejsów domenowych. Dzięki temu interesuje go wyłącznie to,
**jaką operację wykonać**, a nie **skąd dane zostaną pobrane**.

Przykłady:

* `HomeViewModel` obserwuje utwory przez `ObserveSongsUseCase`,
* `SongViewModel` pobiera utwór przez `GetSongUseCase`,
* `SettingsViewModel` korzysta z `SettingsRepository`,
* `BluetoothViewModel` korzysta z `InstrumentRepository`.

#### Warstwa `domain`

Warstwa domenowa zawiera reguły i pojęcia ważne dla aplikacji, niezależnie od
Androida i sposobu przechowywania danych.

Znajdują się w niej:

* modele domenowe, na przykład `Song`, `NotePlayed` i `ThemeMode`,
* interfejsy repozytoriów,
* przypadki użycia, na przykład pobieranie, filtrowanie i oznaczanie utworów
  oraz ocena zagranej nuty.

Warstwa `domain` nie importuje klas Androida, Room, MediaPlayer ani Bluetooth.
Może być testowana jako zwykły kod Kotlin/JVM.

Repozytorium w domenie jest kontraktem. Przykładowo `SongRepository` mówi,
że aplikacja potrafi obserwować utwory, pobrać utwór albo zmienić jego stan.
Nie mówi, czy dane znajdują się w Room, pliku JSON czy innym źródle.

#### Warstwa `data`

Warstwa danych zawiera techniczne sposoby przechowywania i odczytywania danych.

Znajdują się w niej:

* baza Room,
* encje i DAO,
* repozytoria implementujące interfejsy domenowe,
* mapowanie pomiędzy encjami Room a modelami domenowymi,
* DataStore i repozytorium ustawień,
* odczyt danych z assets,
* implementacja odtwarzania audio.

Przykładowy przepływ odczytu utworów wygląda tak:

```text
HomeViewModel
    ↓
ObserveSongsUseCase
    ↓
SongRepository
    ↓
RoomSongRepository
    ↓
SongDao → SongEntity → SongMappers → Song
```

Room jest więc szczegółem implementacyjnym warstwy `data`. Pozostałe warstwy
otrzymują model domenowy `Song`, a nie encję Room.

#### Warstwa `device`

Warstwa urządzenia odpowiada za komunikację z fizycznym instrumentem.

Znajdują się w niej:

* implementacja komunikacji Bluetooth,
* obsługa połączenia i jego stanu,
* odczyt komunikatów z urządzenia,
* kodek tłumaczący surowe komunikaty na zdarzenia domenowe,
* zamykanie połączenia i obsługa błędów transportu.

Pozostałe warstwy korzystają z interfejsu `InstrumentRepository`, a nie
bezpośrednio z Android Bluetooth API. Dzięki temu szczegóły transportu są
ukryte w jednym miejscu.

Wybór konkretnego protokołu i transportu BLE jest opisany w [ADR 007](007-ble-protocol.md).
Niezależnie od tego, czy używany jest aktualny transport prototypowy, czy
docelowy BLE GATT, jego implementacja należy do warstwy `device`.

#### Składanie zależności

Zależności aplikacji są składane w `AppContainer`, który pełni funkcję
composition root:

* tworzy implementacje repozytoriów,
* tworzy przypadki użycia,
* tworzy odtwarzacz audio,
* przekazuje zależności do fabryk ViewModeli.

Zastosowano constructor injection bez dodatkowego frameworka DI. Dla małej
aplikacji jest to wystarczające i pozwala jasno zobaczyć, z czego składa się
aplikacja.

#### Aktualny przepływ informacji

Dla działania użytkownika przepływ wygląda teraz następująco:

```text
ekran Compose
    ↓
ViewModel
    ↓
przypadek użycia lub interfejs domenowy
    ↓
implementacja w data albo device
    ↓
model domenowy lub zdarzenie
    ↓
stan ViewModelu
    ↓
ekran Compose
```

Przykładowo podczas nauki:

1. Instrument przekazuje zdarzenie do warstwy `device`.
2. Kodek tłumaczy komunikat na zdarzenie domenowe.
3. `SongViewModel` odbiera zdarzenie przez `InstrumentRepository`.
4. `EvaluateNoteUseCase` ocenia nutę.
5. ViewModel aktualizuje stan lekcji.
6. Compose wyświetla wynik użytkownikowi.

## Decyzja

Wybrano **Opcję B: podział na warstwy `presentation`, `domain`, `data` i
`device`**.

Decyzja została podjęta, ponieważ:

* każda warstwa ma jedną główną odpowiedzialność,
* ViewModel nie musi wiedzieć, skąd pochodzą dane,
* logika domenowa może być testowana jako czysty Kotlin,
* Room, DataStore, audio i Bluetooth są szczegółami implementacyjnymi,
* zmiana źródła danych nie wymaga przebudowy interfejsu użytkownika,
* komunikacja z instrumentem jest odizolowana od reszty aplikacji,
* ręczny `AppContainer` jest prostszy i wystarczający dla tego projektu.

Podział nie ma na celu stworzenia rozbudowanej architektury produkcyjnej.
Jest minimalnym podziałem potrzebnym do usunięcia bezpośrednich zależności
ViewModeli od infrastruktury i umożliwienia testowania logiki domenowej.

## Konsekwencje

### Korzyści

* ViewModele są prostsze i skupiają się na stanie oraz zachowaniu ekranu.
* Logika domenowa jest niezależna od Androida.
* Repozytoria i przypadki użycia można testować bez uruchamiania interfejsu.
* Room może zostać zmieniony bez zmiany kodu prezentacji.
* Szczegóły Bluetooth są zamknięte w warstwie `device`.
* Mapowanie modeli odbywa się na granicach warstw.
* Kod łatwiej analizować i rozwijać w ramach pracy inżynierskiej.

### Koszty

* Pojawia się więcej plików, interfejsów i mapowań.
* Trzeba jawnie składać zależności w `AppContainer`.
* Prosta funkcja może przechodzić przez kilka warstw.
* Każda zmiana kontraktu domenowego może wymagać aktualizacji implementacji
  repozytorium i ViewModelu.

Koszty te są ograniczone przez brak frameworka DI, brak klas bazowych,
brak event busa i brak dodatkowych menedżerów. Architektura pozostaje
prosta, ale zachowuje wyraźne granice odpowiedzialności.
