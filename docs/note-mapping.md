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

W każdej tabeli nuta bazowa jest podana razem z numerem MIDI. Nuty po zmianie
położenia dźwigni również zawierają numer MIDI; `LeverOffset +1` podnosi dźwięk
o półton, a `LeverOffset -1` obniża go o półton.

### OKTAWA 1

| ID Struny (`string_id`) | Stopień Gamy | Nuta Bazowa | Nuta (`LeverOffset + 1`) | Nuta (`LeverOffset - 1`) |
| :---: | :---: | :---: | :---: | :---: |
| **0** | C | **C1** (MIDI 24) | **C#1** (MIDI 25) | **B0** (MIDI 23) |
| **1** | D | **D1** (MIDI 26) | **D#1** (MIDI 27) | **C#1** (MIDI 25) |
| **2** | E | **E1** (MIDI 28) | **F1** (MIDI 29) | **D#1** (MIDI 27) |
| **3** | F | **F1** (MIDI 29) | **F#1** (MIDI 30) | **E1** (MIDI 28) |
| **4** | G | **G1** (MIDI 31) | **G#1** (MIDI 32) | **F#1** (MIDI 30) |
| **5** | A | **A1** (MIDI 33) | **A#1** (MIDI 34) | **G#1** (MIDI 32) |
| **6** | B | **B1** (MIDI 35) | **C2** (MIDI 36) | **A#1** (MIDI 34) |
| **7** | C | **C2** (MIDI 36) | **C#2** (MIDI 37) | **B1** (MIDI 35) |
| **8** | D | **D2** (MIDI 38) | **D#2** (MIDI 39) | **C#2** (MIDI 37) |
| **9** | E | **E2** (MIDI 40) | **F2** (MIDI 41) | **D#2** (MIDI 39) |
| **10** | F | **F2** (MIDI 41) | **F#2** (MIDI 42) | **E2** (MIDI 40) |
| **11** | G | **G2** (MIDI 43) | **G#2** (MIDI 44) | **F#2** (MIDI 42) |
| **12** | A | **A2** (MIDI 45) | **A#2** (MIDI 46) | **G#2** (MIDI 44) |
| **13** | B | **B2** (MIDI 47) | **C3** (MIDI 48) | **A#2** (MIDI 46) |
| **14** | C | **C3** (MIDI 48) | **C#3** (MIDI 49) | **B2** (MIDI 47) |

---

### OKTAWA 2

| ID Struny (`string_id`) | Stopień Gamy | Nuta Bazowa | Nuta (`LeverOffset + 1`) | Nuta (`LeverOffset - 1`) |
| :---: | :---: | :---: | :---: | :---: |
| **0** | C | **C2** (MIDI 36) | **C#2** (MIDI 37) | **B1** (MIDI 35) |
| **1** | D | **D2** (MIDI 38) | **D#2** (MIDI 39) | **C#2** (MIDI 37) |
| **2** | E | **E2** (MIDI 40) | **F2** (MIDI 41) | **D#2** (MIDI 39) |
| **3** | F | **F2** (MIDI 41) | **F#2** (MIDI 42) | **E2** (MIDI 40) |
| **4** | G | **G2** (MIDI 43) | **G#2** (MIDI 44) | **F#2** (MIDI 42) |
| **5** | A | **A2** (MIDI 45) | **A#2** (MIDI 46) | **G#2** (MIDI 44) |
| **6** | B | **B2** (MIDI 47) | **C3** (MIDI 48) | **A#2** (MIDI 46) |
| **7** | C | **C3** (MIDI 48) | **C#3** (MIDI 49) | **B2** (MIDI 47) |
| **8** | D | **D3** (MIDI 50) | **D#3** (MIDI 51) | **C#3** (MIDI 49) |
| **9** | E | **E3** (MIDI 52) | **F3** (MIDI 53) | **D#3** (MIDI 51) |
| **10** | F | **F3** (MIDI 53) | **F#3** (MIDI 54) | **E3** (MIDI 52) |
| **11** | G | **G3** (MIDI 55) | **G#3** (MIDI 56) | **F#3** (MIDI 54) |
| **12** | A | **A3** (MIDI 57) | **A#3** (MIDI 58) | **G#3** (MIDI 56) |
| **13** | B | **B3** (MIDI 59) | **C4** (MIDI 60) | **A#3** (MIDI 58) |
| **14** | C | **C4** (MIDI 60) | **C#4** (MIDI 61) | **B3** (MIDI 59) |

---

### OKTAWA 3

| ID Struny (`string_id`) | Stopień Gamy | Nuta Bazowa | Nuta (`LeverOffset + 1`) | Nuta (`LeverOffset - 1`) |
| :---: | :---: | :---: | :---: | :---: |
| **0** | C | **C3** (MIDI 48) | **C#3** (MIDI 49) | **B2** (MIDI 47) |
| **1** | D | **D3** (MIDI 50) | **D#3** (MIDI 51) | **C#3** (MIDI 49) |
| **2** | E | **E3** (MIDI 52) | **F3** (MIDI 53) | **D#3** (MIDI 51) |
| **3** | F | **F3** (MIDI 53) | **F#3** (MIDI 54) | **E3** (MIDI 52) |
| **4** | G | **G3** (MIDI 55) | **G#3** (MIDI 56) | **F#3** (MIDI 54) |
| **5** | A | **A3** (MIDI 57) | **A#3** (MIDI 58) | **G#3** (MIDI 56) |
| **6** | B | **B3** (MIDI 59) | **C4** (MIDI 60) | **A#3** (MIDI 58) |
| **7** | C | **C4** (MIDI 60) | **C#4** (MIDI 61) | **B3** (MIDI 59) |
| **8** | D | **D4** (MIDI 62) | **D#4** (MIDI 63) | **C#4** (MIDI 61) |
| **9** | E | **E4** (MIDI 64) | **F4** (MIDI 65) | **D#4** (MIDI 63) |
| **10** | F | **F4** (MIDI 65) | **F#4** (MIDI 66) | **E4** (MIDI 64) |
| **11** | G | **G4** (MIDI 67) | **G#4** (MIDI 68) | **F#4** (MIDI 66) |
| **12** | A | **A4** (MIDI 69) | **A#4** (MIDI 70) | **G#4** (MIDI 68) |
| **13** | B | **B4** (MIDI 71) | **C5** (MIDI 72) | **A#4** (MIDI 70) |
| **14** | C | **C5** (MIDI 72) | **C#5** (MIDI 73) | **B4** (MIDI 71) |


## 6. Struktura Ramki Handshake BLE (Firmware $\to$ App) 

Podczas połączenia BLE harfa wysyła paczkę konfiguracyjną (9 bajtów): 

| Bajt | Opis | Wartości |
| :---: | :--- | :--- |
| **0** | `OctaveShift` | `-1`, `0`, `1` (zapisany jako uint8 z offsetem +1: `0..2`) |
| **1..7** | `LeverOffset[0..6]` | Stany dla C, D, E, F, G, A, B (zapisane z offsetem +1: `0..2`) |
| **8** | `BaseOctave` | Domyślna oktawa początkowa (np. `2` dla C2) |