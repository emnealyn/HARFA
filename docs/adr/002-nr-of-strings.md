# ADR 002: Dobór liczby strun w harfie.

* **Stan:** Przyjęty
* **Data:** 2026-10-31
* **Osoby decyzyjne:** Emilia Bonk

## Kontekst
Harfa ma dawać jak najbardziej przyjazne doświadcznie zapewniając jednocześnie prostotę ale też pole do kreatywności i mieszcząc się w wymiarach możliwych do druku w warunkach laboratorium uczelni.

## Rozważane alternatywy
### Opcja A: 1 Oktawa - 8 strun
* **Zalety:**
 * Przetestowany model na prototypie
 * Nie ma problemu ze zbyt małą liczbą wejść na płytce czy wielkością obudowy.
* **Wady:**
 * Mała różnorodność dźwięków zapewnia mniej ciekawe doświadczenie, trzeba się przełączać między oktawami
 * Dużo utworów wymaga przynajmniej 2 oktaw.
### Opcja B: 2 Oktawy - 15 strun
* **Zalety:**
 * Więcej dostępnych utworów do zagrania
 * Obudowa wciąż możliwa do wydrukowania na drukarce 40x40 bez nadmiernych komplikacji.
* **Wady:**
 * Wymaga przemyślenia ilości wejść analogowych na mikrokontrolerze. 
 * Testowo wymaga zaprojektowania większej obudowy.
 * Wciąż nie pozwala na zagranie części utworów.

 ### Opcja C: 3 Oktawa - 22 struny
 * **Zalety:**
 * Pokrywa większość popularnych utworów bez konieczności zmieniania oktaw.
* **Wady:**
 * Szerokość obudowy wynosiłaby już około 1.2-1.5m zmuszając do wyjatkowo dużego wydruku 3D. 
 * Należy szczególnie przemyśleć dobór mikrokonttrolera i rozważyć moduł z multiplekserem analogowym.

## Decyzja
Wybrano **Opcję B - (2 Oktawy - 15 strun)**.

15 strun daje możliwość zagrania znacznie większej gamy utworów niż oryginalne 8, mieszcząc się wciąż wygodnie na rozważanych mikrokontrolerach oraz zachowując wykonalne wymiary harfy.

## Konsekwencje
* Obudowa 3D musi uwzględnić rozmieszczenie 15 strun pomiędzy ramami.
* Należy zakupić 15 diód laserowych, fototranzystorów i zwykłych diód czerwonych