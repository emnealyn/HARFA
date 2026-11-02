# Mapowanie Strun i Konfiguracja Instrumentu

Dokument określa spójny model mapowania fizycznych strun harfy (0–14) na nuty, pliki dźwiękowe `.wav`, stany przełączników oraz transpozycję oktawową.

---

## 1. Architektura i Single Source of Truth (SSOT)

- **Firmware (STM32):** Zna aktualny stan przełączników i domyślne strojenie oraz odpowiadające im sample `.wav`. Podczas procedury *Handshake* wysyła do aplikacji mobilnej strukturę konfiguracyjną po BLE.
- **Aplikacja mobilna (Android/Kotlin):** Odbiera strukturę nastrojenia, dynamicznie przelicza nuty z utworu na identyfikatory strun (`0..14`).
- **Identyfikatory komunikacji:** W protokole BLE oraz wewnątrz kodu struny oznaczane są indeksami `0..14`.

---

## 2. Podstawowe Strojenie Strun ($C_2 - C_4$)

Harfa posiada **15 fizycznych strun** strojonych w tonacji C-dur (2 oktawy diatoniczne + nuta wieńcząca $C_4$).

| ID Struny (`string_id`) | Stopień Gamy | Nuta Nazwa | Nuta MIDI | Częstotliwość [Hz] | Nazwa Pliku Sample `.wav` |
| :---: | :---: | :---: | :---: | :---: | :---: |
| **0** | C | C2 | 36 | 65.41 | `c2.wav` |
| **1** | D | D2 | 38 | 73.42 | `d2.wav` |
| **2** | E | E2 | 40 | 82.41 | `e2.wav` |
| **3** | F | F2 | 41 | 87.31 | `f2.wav` |
| **4** | G | G2 | 43 | 98.00 | `g2.wav` |
| **5** | A | A2 | 45 | 110.00 | `a2.wav` |
| **6** | B | B2 | 47 | 123.47 | `b2.wav` |
| **7** | C | C3 | 48 | 130.81 | `c3.wav` |
| **8** | D | D3 | 50 | 146.83 | `d3.wav` |
| **9** | E | E3 | 52 | 164.81 | `e3.wav` |
| **10** | F | F3 | 53 | 174.61 | `f3.wav` |
| **11** | G | G3 | 55 | 196.00 | `g3.wav` |
| **12** | A | A3 | 57 | 220.00 | `a3.wav` |
| **13** | B | B3 | 59 | 246.94 | `b3.wav` |
| **14** | C | C4 | 60 | 261.63 | `c4.wav` |

---



## 3. Przełączniki (Pedały) i Transpozycja

Harfa posiada **7 przełączników** (po jednym dla każdego stopnia gamy `[C, D, E, F, G, A, B]`).
Każdy przełącznik wpływa na **wszystkie struny danego stopnia** jednocześnie.

### Stany Przełącznika:
- `-1` (Flat / Bemol): obniża tonację stopnia o $0.5$ tonu ($-1$ półton MIDI)
- `0` (Natural / Kasownik): dźwięk bazowy ($0$ półtonów)
- `+1` (Sharp / Krzyżyk): podwyższa tonację stopnia o $0.5$ tonu ($+1$ półton MIDI)

### Transpozycja Oktawowa (`OctaveShift`):
- Zakres: `{-1, 0, +1}` (przesunięcie całości o $-12$, $0$ lub $+12$ półtonów MIDI).

---

## 4. Algorytm Przeliczania Nuty Wyjściowej (Obowiązujący zarówno w Firmware jak i Aplikacji Mobilnej)

Efektywny numer nuty MIDI dla danej struny wyliczany jest według wzoru:

$$\text{MIDI\_Effective}(id) = \text{BaseMIDI}[id] + \text{LeverOffset}[\text{Step}(id)] + (\text{OctaveShift} \times 12)$$

Gdzie:
- $\text{BaseMIDI}[id]$ – bazowa wartość MIDI z tabeli dla $id \in [0..14]$
- $\text{Step}(id)$ – stopień gamy z odwzorowania: $0 \to C, 1 \to D, 2 \to E, 3 \to F, 4 \to G, 5 \to A, 6 \to B$
- $\text{LeverOffset}$ – tablica 7 elementów ze stanem przełączników $\in \{-1, 0, +1\}$
- $\text{OctaveShift}$ – transpozycja oktawowa $\in \{-1, 0, +1\}$

Tak uzyskany effectiveMIDI będzie nam służył jako forma identyfikacji przekształconej nuty w Aplikacji Mobilej oraz w samej Harfie.

---

## 5. Pełne spektrum dźwięków Harfy

Tabela zawiera wszystkie dźwięki występujące w zakresie mapowania z trzech
oktaw, od `B0` do `C#5`. Pliki dla dźwięków z krzyżykiem używają oznaczenia
`cs`, np. `C#1` jest zapisane jako `cs1.wav`.

| Dźwięk | MIDI | Plik `.wav` | Dźwięk | MIDI | Plik `.wav` | Dźwięk | MIDI | Plik `.wav` |
| :---: | :---: | :--- | :---: | :---: | :--- | :---: | :---: | :--- |
| **B0** | 23 | `b0.wav` | **E2** | 40 | `e2.wav` | **A3** | 57 | `a3.wav` |
| **C1** | 24 | `c1.wav` | **F2** | 41 | `f2.wav` | **A#3** | 58 | `as3.wav` |
| **C#1** | 25 | `cs1.wav` | **F#2** | 42 | `fs2.wav` | **B3** | 59 | `b3.wav` |
| **D1** | 26 | `d1.wav` | **G2** | 43 | `g2.wav` | **C4** | 60 | `c4.wav` |
| **D#1** | 27 | `ds1.wav` | **G#2** | 44 | `gs2.wav` | **C#4** | 61 | `cs4.wav` |
| **E1** | 28 | `e1.wav` | **A2** | 45 | `a2.wav` | **D4** | 62 | `d4.wav` |
| **F1** | 29 | `f1.wav` | **A#2** | 46 | `as2.wav` | **D#4** | 63 | `ds4.wav` |
| **F#1** | 30 | `fs1.wav` | **B2** | 47 | `b2.wav` | **E4** | 64 | `e4.wav` |
| **G1** | 31 | `g1.wav` | **C3** | 48 | `c3.wav` | **F4** | 65 | `f4.wav` |
| **G#1** | 32 | `gs1.wav` | **C#3** | 49 | `cs3.wav` | **F#4** | 66 | `fs4.wav` |
| **A1** | 33 | `a1.wav` | **D3** | 50 | `d3.wav` | **G4** | 67 | `g4.wav` |
| **A#1** | 34 | `as1.wav` | **D#3** | 51 | `ds3.wav` | **G#4** | 68 | `gs4.wav` |
| **B1** | 35 | `b1.wav` | **E3** | 52 | `e3.wav` | **A4** | 69 | `a4.wav` |
| **C2** | 36 | `c2.wav` | **F3** | 53 | `f3.wav` | **A#4** | 70 | `as4.wav` |
| **C#2** | 37 | `cs2.wav` | **F#3** | 54 | `fs3.wav` | **B4** | 71 | `b4.wav` |
| **D2** | 38 | `d2.wav` | **G3** | 55 | `g3.wav` | **C5** | 72 | `c5.wav` |
| **D#2** | 39 | `ds2.wav` | **G#3** | 56 | `gs3.wav` | **C#5** | 73 | `cs5.wav` |


## 6. Struktura Ramki Handshake BLE (Firmware $\to$ App) 

Podczas połączenia BLE harfa wysyła paczkę konfiguracyjną (9 bajtów): 

| Bajt | Opis | Wartości |
| :---: | :--- | :--- |
| **0** | `OctaveShift` | `-1`, `0`, `1` (zapisany jako uint8 z offsetem +1: `0..2`) |
| **1..7** | `LeverOffset[0..6]` | Stany dla C, D, E, F, G, A, B (zapisane z offsetem +1: `0..2`) |
| **8** | `BaseOctave` | Domyślna oktawa początkowa (np. `2` dla C2) |