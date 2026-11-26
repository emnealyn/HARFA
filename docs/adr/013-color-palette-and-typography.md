# ADR 013: Paleta kolorów, typografia i motyw aplikacji mobilnej

* **Stan:** Przyjęty
* **Data:** 2026-11-26
* **Osoby decyzyjne:** Emilia Bonk

## Kontekst

Przed HARFA-36 motyw aplikacji definiował tylko kilka ról Material 3 (`primary`, `secondary`, `background`, `surface` i ich kolory treści). Pozostałe role, m.in. `surfaceContainer*`, `surfaceVariant`, `errorContainer` i `primaryContainer`, przyjmowały domyślne, fioletowe wartości Material 3. Było to widać w rozwijanych menu, oknach dialogowych i komunikatach ostrzegawczych. Poza motywem istniały też kolory zaszyte w komponentach (`Color.Green` w `TopBar`, literały w `DifficultyBadge` i `BluetoothScreen`, `outlineVariant` wpisany w `Theme.kt`) oraz półprzezroczysty `DarkLibraryHeader`. W trybie ciemnym `DarkPrimary` (`#851F1F`) miał na tle kontrast ok. 1,9:1.

Celem było przygotowanie spójnej, stonowanej palety w stylu vintage z luksusową czerwienią, która jest jedynym źródłem kolorów w aplikacji, działa w obu motywach i pasuje do charakteru instrumentu.

## Rozważane alternatywy

### Opcja A: Pozostawienie oryginalnego wyglądu
Paleta `#5E0006` / `#80050A` na prawie białym tle (`#F9F6F6`) oraz neutralny, grafitowy tryb ciemny (`#151515`), z górnym i dolnym paskiem w pełnym kolorze `primary`.
* **Zalety:**
  * Brak pracy i ryzyka regresji.
* **Wady:**
  * Brakujące role Material 3 dawały fioletowe tła menu, dialogów i komunikatów.
  * Kolory zaszyte poza motywem i półprzezroczysty nagłówek w trybie ciemnym.
  * `DarkPrimary` z kontrastem ok. 1,9:1 na tle.
  * Pełne bordowe paski odcinały się od treści i dominowały ekran.

### Opcja B: Bordo z akcentem antycznego złota
Pergaminowe tło, oksblodowe `primary` i złoty `secondary` (`#9C7A3C` / `#C9A66B`), z którego m.in. rysowana jest rama harfy.
* **Zalety:**
  * Wyraźny charakter „luksusowy”, harfa wyglądająca na złoconą.
* **Wady:**
  * Złoto nie pasowało do reszty interfejsu i było zbyt krzykliwe przy czerwieni.
  * Pierwsza wersja trybu ciemnego (szarawe brązy, jaskrawoczerwony nagłówek) wyglądała nieestetycznie, a niezdefiniowane role nadal dawały fioletowe tła.

### Opcja C: Bordo ze stonowanymi brązami, z pastelowym trybem ciemnym
Złoto zastąpione brązami (orzech, umbra), tryb ciemny w ciepłych, ale jasnych i mało nasyconych tonach.
* **Zalety:**
  * Spójny, spokojny tryb jasny.
* **Wady:**
  * Tryb ciemny był zbyt pastelowy, a kolory statusu (zielony, żółty, czerwony) w trybie jasnym zbyt przygaszone.
  * Kolory ♯/♭ liczone z koloru błędu dawały niespójną gamę przycisków strojenia (pastelowy róż obok błotnistej czerwieni).

### Opcja D: Oksblod i bordo, stonowane brązy, pergamin / espresso, nasycony tryb ciemny (wybrana)
* **Tryb jasny:** `primary` `#6B0F1A` (oksblod), nagłówek `#7A1F2B` (bordo), tło `#EFE6D8` (len/pergamin), powierzchnie `#F7F0E4`, tekst `#2B1D16` (sepia), `secondary` `#7A5C4A` (orzech).
* **Tryb ciemny:** tło `#1C1613` (espresso), `primary` `#B32D3A` (karmazyn), nagłówek `#5A1D24` (głębokie wino, w pełni kryjące), `secondary` `#8C6C51`, tekst `#EADFD0` (kość słoniowa).
* **Zalety:**
  * Jedna gama barw w obu motywach, bez fioletu i bez złota.
  * Kolory statusu i strojenia dobrane osobno dla każdego motywu.
* **Wady:**
  * Część kolorów akcentu nie spełnia progów kontrastu (patrz „Konsekwencje”).

### Typografia tytułów
* **Domyślna czcionka bezszeryfowa:** czytelna, ale nie wyróżnia się na tle aplikacji.
* **Playfair Display Black:** wyraźna, ale zbyt ozdobna (duży kontrast grubości kresek) i za ciężka w pasku aplikacji.
* **Systemowa czcionka szeryfowa:** bez zależności, ale wygląd zależy od producenta telefonu.
* **Lora Bold (wybrana):** spokojny, książkowy krój szeryfowy, pobierany przez Google Fonts (`ui-text-google-fonts`, `font_certs.xml`), więc nie zwiększa rozmiaru APK. Do czasu pobrania lub bez usług Google Play używana jest systemowa czcionka szeryfowa.

### Tekstury
Rozważano tła z teksturą papieru. Nie wprowadzono ich, aby nie obniżać czytelności tekstu i nut oraz nie zwiększać rozmiaru APK (por. HARFA-31). Efekt „papieru” uzyskano samym kolorem tła.

## Decyzja

Wybrano **Opcję D** oraz czcionkę **Lora Bold** dla tytułów. Ustalenia:

* `Color.kt` i `Theme.kt` definiują pełny zestaw ról Material 3 dla obu motywów: kontenery `primary`/`secondary`/`tertiary`, `surfaceVariant`, wszystkie `surfaceContainer*`, `outline`, `outlineVariant`, `inverse*`, `error*`, a `surfaceTint` jest brązowy, nie fioletowy.
* Kolory spoza Material 3 są w `ExtendedColors`, dostępnym jako `MaterialTheme.extendedColors` i dobieranym osobno dla trybu jasnego i ciemnego: stan połączenia, trudność utworu, kolory ♭/♮/♯ strun i przycisków strojenia, kolor nadchodzącej nuty, tytuł górnego paska i zaznaczona pozycja w dolnym pasku.
* Poza pakietem `theme` nie ma literałów `Color(0x…)` ani `Color.Green` itp.
* `errorContainer` jest półprzezroczysty i w stonowanej, ceglastej czerwieni, aby ostrzeżenia (np. niezgodne strojenie, utrata połączenia, niska bateria) nie były pastelowo różowe w trybie jasnym ani jaskrawe w ciemnym.
* Górny pasek ma kolor tła, tytuł wyrównany do lewej (Lora Bold) i status Bluetooth/baterii w zaokrąglonej pigułce. Dolny pasek jest stonowany i półprzezroczysty, a wyraźny kolor ma tylko zaznaczona ikona.
* Paski systemowe obsługuje `enableEdgeToEdge` ze stylem jasnym lub ciemnym zgodnym z motywem wybranym w aplikacji (nie z motywem systemu), bez przestarzałych `window.statusBarColor` / `navigationBarColor`.
* Ekrany stanu utworu (brak wybranego utworu, nie znaleziono, błąd ładowania) korzystają ze wspólnego `SongStatePlaceholder` w stylu palety.

## Konsekwencje

* Kolory zmienia się w jednym miejscu (`theme/`), a nowe komponenty powinny korzystać z ról Material 3 lub `extendedColors`, nigdy z literałów.
* Tytuł wymaga dostępu do usług Google Play przy pierwszym pobraniu czcionki. Na urządzeniach bez nich zostaje systemowy krój szeryfowy.
* Na Androidzie 15+ edge-to-edge jest wymuszane przez system (`targetSdk 36`). Dzięki `enableEdgeToEdge` aplikacja wygląda tak samo na starszych wersjach, a odstępy od pasków zapewniają `TopAppBar`, `NavigationBar` i `innerPadding` ze `Scaffold`.

### Elementy niespełniające wymaganych proporcji kontrastu

Kryteria HARFA-36 wymagają co najmniej 4,5:1 dla tekstu i 3:1 dla ikon i elementów interfejsu. Poniższe elementy ich nie spełniają. Jest to świadoma decyzja, a nie przeoczenie:

| Element | Motyw | Kontrast | Wymóg |
|---|---|---|---|
| Etykiety trudności: łatwy / średni / trudny (tekst na tle etykiety) | jasny | 2,89 / 2,02 / 3,70 | 4,5 |
| Tekst „połączono” (`connected`, ekran Harfa) | jasny | 3,48 | 4,5 |
| `DarkPrimary` jako ikona/obramowanie na tle (obramowania przycisków strojenia, linia zakładek, kropka aktywnej nuty) | ciemny | 2,86 | 3 |
| Struna ♭ na ilustracji harfy | ciemny | 2,68 | 3 |
| `outlineVariant` (obramowania kart) | oba | 1,75 / 1,68 | 3 |
| Nuta aktywna przy najsłabszej fazie pulsowania oraz nuta ukończona | oba | poniżej 4,5 | 4,5 |

Uzasadnienie:

* **Etykiety trudności i kolor „połączono”.** Zielony, bursztynowy i czerwony tekst na jasnym tle nie osiąga 4,5:1 bez wyraźnego przyciemnienia. Sprawdzono dwa warianty: przyciemniony tekst, który wyglądał na „martwy” i zgaszony, oraz etykiety wypełnione żywym kolorem z ciemnym tekstem, które wyglądały nieestetycznie i odstawały od stonowanej palety. Oba odrzucono na rzecz żywych kolorów, które od razu kojarzą się z poziomem trudności. Tekst etykiet jest krótki i pogrubiony, a w trybie ciemnym te same kolory spełniają wymogi.
* **`DarkPrimary`.** Rozjaśnienie go do 3:1 (`#BC3442`) zmieniało charakter karmazynu i obniżało kontrast jasnego tekstu na przyciskach. Na tle jest używany wyłącznie w elementach pomocniczych (obramowania, linie, kropka), a jako tło przycisków z jasnym tekstem ma 5,61:1.
* **Struna ♭.** To element ilustracji, a nie jedyny nośnik informacji: stan ♭/♮/♯ jest też pokazany symbolem na przycisku strojenia, który ma odpowiedni kontrast.
* **`outlineVariant`.** W Material 3 rola ta służy do ozdobnych obramowań. Karty odróżniają się od tła również kolorem wypełnienia, więc obramowanie nie jest jedyną granicą komponentu. Kontrast 3:1 dałby ciężkie, ciemne ramki niepasujące do stylu.
* **Stany nut.** Pulsowanie aktywnej nuty to animowana informacja zwrotna, która w szczytowej fazie spełnia wymogi (10,49 / 5,61). Ukończone nuty są celowo przygaszone, aby uwaga ucznia skupiała się na nucie aktywnej i nadchodzących, które spełniają wymogi w obu motywach (9,94 / 6,11). Wersja z pełnym kryciem wszystkich stanów była przygotowana, ale odrzucona razem z poprawkami powyżej.

