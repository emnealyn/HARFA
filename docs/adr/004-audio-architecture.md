# ADR 004: Wybór architektury toru audio (I2S DAC vs Dedykowany DSP)

* **Stan:** Przyjęty
* **Data:** 2026-10-31
* **Osoby decyzyjne:** Joanna Walczak, Emilia Bonk

## Kontekst
Urządzenie wymaga spójnego i niskolatencyjnego toru audio do odtwarzania prądu i dźwięków harfy na podstawie sygnałów z czujników strun.

## Rozważane alternatywy

### Opcja A: ADAU1701 (DSP) + TPA3116D2 (Wzmacniacz Analogowy) ~80zł
Rozwiązanie opierające się na sprzętowym procesorze DSP programowanym przez narzędzie SigmaStudio.
* **Zalety:**
  * Sprzętowa realizacja efektów audio, korekcji graficznej (EQ) i filtrów bez obciążania głównego mikrokontrolera.
  * Wysokie możliwości dostrojenia brzmienia głośnika.
* **Wady:**
  * Znaczny wzrost kosztu zestawienia materiałowego (BOM).
  * Skomplikowany proces produkcji i wgrywania oprogramowania (osobny wsad dla DSP).

### Opcja B: Magistrala WAV/I2S + MAX98357A (DAC + Wzmacniacz Class-D) ~30zł
Układ zintegrowany łączący dekoder I2S, przetwornik cyfrowo-analogowy (DAC) oraz wzmacniacz Class-D.
* **Zalety:**
  * **Uproszczenie i zmiejszenie kosztów BOM:** Zamiast dwóch osobnych układów stosowany jest jeden mały tani układ.
  * **Prostszy Firmware:** Brak konieczności utrzymywania i wgrywania osobnego oprogramowania dla DSP.
* **Wady:**
  * Ewentualna miksacja audio i prosta korekcja barwy muszą być wykonywane po stronie MCU przed wysłaniem na magistralę I2S co dodatkowo obciąży mikrokontroler.

## Decyzja
Wybrano **Opcję B (WAV/I2S + MAX98357A)** jako oficjalną i jedyną architekturę toru audio dla projektu. 

Decyzja została podjęta po konsultacji z pracownikiem naukowym Politechniki Poznańskiej, który odradził korzystania z DSP ze względu na skomplikowaną naturę pracy z tym urządzeniem.

## Konsekwencje
* Układy ADAU1701 oraz TPA3110 zostają oficjalnie usunięte z listy komponentów (BOM).
* Schemat elektryczny (hardware) zostanie zaprojektowany z uwzględnieniem MAX98357 pod piny magistrali I2s mikrokontrolera.