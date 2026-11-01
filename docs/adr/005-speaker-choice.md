# ADR 005: Wybór przetwornika akustycznego.

* **Stan:** Przyjęty
* **Data:** 2026-10-31
* **Osoby decyzyjne:** Joanna Walczak

## Kontekst
Do odsłuchu dźwięku z harfy wymagany jest głośnik o szerokim zakresie dźwięków, skuteczności 80–90 dB, małych wymaganiach dotyczących obudowy oraz możliwie niedużych gabarytach, aby zmieścił się w podstawie/komorze harfy (orientacyjnie 10x10x10 cm). Wzmacniaczem audio wybranym do układu jest **MAX98357A**.

## Rozważane alternatywy
### Opcja A: Visaton FR 10 HM (~50 zł)
* **Zalety:** Najniższa cena, szerokie deklarowane pasmo wysokich częstotliwości, niewielka średnica ułatwiająca montaż w podstawie.
* **Wady:** Wysoka częstotliwość rezonansowa (Fs = 120 Hz) ogranicza odtwarzanie niskich nut, wysokie Qts (1,13) utrudnia uzyskanie wyrównanej charakterystyki w małej zamkniętej komorze.

### Opcja B: Dayton Audio PC105-4 (~100 zł)
* **Zalety:** Bardzo wysoka skuteczność (90,3 dB @ 2,83V/1m) bardzo korzystna przy zasilaniu bateryjnym, niższa częstotliwość rezonansowa (Fs = 81,4 Hz), impedancja 4 Ω pozwalająca uzyskać dużą moc przy niskim napięciu wzmacniacza, bardzo dobry stosunek możliwości do ceny.
* **Wady:** Pasmo kończy się na 15 kHz, mniejsze wychylenie liniowe (Xmax = 2 mm) niż RS100-4, większe Vas (3,68 l) wymagające analizy zachowania w małej obudowie.

### Opcja C: Dayton Audio PS95-8 (~115–150 zł)
* **Zalety:** Kompaktowe wymiary (3,5 cala), konstrukcja Point Source z korektorem fazy, niewielkie Vas (0,94 l) korzystne przy małej obudowie, szerokie pasmo wysokich tonów (do 20 kHz).
* **Wady:** Wysoka częstotliwość rezonansowa (Fs = 119,8 Hz), najmniejsza powierzchnia membrany, niższą skuteczność (85,56 dB), impedancja 8 Ω ograniczająca moc przy wzmacniaczu zasilanym z 5 V, moc znamionowa tylko 10 W.

### Opcja D: Dayton Audio RS100-4 (~200–250 zł)
* **Zalety:** Największe liniowe wychylenie membrany (Xmax = 4 mm), małe Vas (1,45 l) i zalecana mała komora zamknięta (~0,85 l), szerokie pasmo do 20 kHz, wysoka odporność na zniekształcenia przy nakładających się nutach i dynamicznych atakach.
* **Wady:** Najwyższa cena w zestawieniu, niższa skuteczność (87,5 dB) niż PC105-4, sztywna aluminiowa membrana mogąca wymagać korekcji.

## Decyzja
Wybrano **Opcję A Visaton FRS 8 4 Ohm**.

Głośnik ten zapewnia optymalne dopasowanie elektryczne do wzmacniacza MAX98357A oraz odpowiednie brzmienie przy wymiarach możliwych do wbudowania w podstawę harfy.

## Konsekwencje
* Głośnik `Visaton FRS 8 4 Ohm` zostaje wpisany do pliku `docs/bom.csv` jako zaakceptowany komponent.
* Projekt obudowy 3D uwzględnia dedykowane mocowanie dla tego głośnika.