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

## 4. Algorytm Przeliczania Nuty Wyjściowej

Efektywny numer nuty MIDI dla danej struny wyliczany jest według wzoru:

$$\text{MIDI\_Effective}(id) = \text{BaseMIDI}[id] + \text{LeverOffset}[\text{Step}(id)] + (\text{OctaveShift} \times 12)$$

Gdzie:
- $\text{BaseMIDI}[id]$ – bazowa wartość MIDI z tabeli dla $id \in [0..14]$
- $\text{Step}(id)$ – stopień gamy z odwzorowania: $0 \to C, 1 \to D, 2 \to E, 3 \to F, 4 \to G, 5 \to A, 6 \to B$
- $\text{LeverOffset}$ – tablica 7 elementów ze stanem przełączników $\in \{-1, 0, +1\}$
- $\text{OctaveShift}$ – transpozycja oktawowa $\in \{-1, 0, +1\}$

---

## 5. Struktura Ramki Handshake BLE (Firmware $\to$ App)

Podczas połączenia BLE harfa wysyła paczkę konfiguracyjną (np. 9 bajtów):

| Bajt | Opis | Wartości |
| :---: | :--- | :--- |
| **0** | `OctaveShift` | `-1`, `0`, `1` (zapisany jako uint8 z offsetem +1: `0..2`) |
| **1..7** | `LeverOffset[0..6]` | Stany dla C, D, E, F, G, A, B (zapisane z offsetem +1: `0..2`) |
| **8** | `BaseOctave` | Domyślna oktawa początkowa (np. `2` dla C2) |