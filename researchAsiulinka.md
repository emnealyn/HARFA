# Research Asiulinka

## Szacunkowy pobór prądu harfy laserowej

| Komponent | Liczba | Pobór prądu | Łączny pobór mocy |
|---|---:|---:|---:|
| Lasery czerwone 650 nm, 5 V | 16 | maks. 30 mA/szt. | 2,40 W |
| STM32WB55 | 1 | ~150 mA | 0,75 W |
| Fototranzystory | 16 | ~1,25 mA/szt. | 0,10 W |
| Czerwone diody LED (tryb nauki) | 16 | ~10 mA/aktywna dioda | 0,05 W |
| Wzmacniacz MAX98357A + 1 głośnik | 1 | ~650 mA | 3,25 W |
| Wyświetlacz | 1 | ~50 mA | 0,25 W |
| **SUMA (wariant z zapasem)** | | **~1510 mA** | **~7,55 W** |

## Zasilanie

### Wyłącznik zasilania
    Włącz/wyłącz.
- https://botland.com.pl/przelaczniki-kolyskowe-dwupozycyjne-3-pozycyjne/8382-przelacznik-on-off-rs-101-7c-t-120-250v6a-czarny-5904422311599.html
### Zasilacz 5V
    Podczas pracy z gniazdka ładuje akumulator i jedocześnie umożliwia grę na harfie podpiętej do prądu bez zużywania energii z baterii.
- https://botland.com.pl/zasilacze-dogniazdkowe/7160-zasilacz-impulsowy-5v3a-15w-wtyk-dc-5521mm-5902135147627.html
### Ładowarka PowerPath
    Zarządza przepływem energii. Gdy podłączymy zasilacz, zasila harfę i ładuje akumulator. Po odłączeniu zasilacza automatycznie przechodzi na zasilanie akumulatorowe.

- aktualnie są jakieś problemy z tą co znalazłam, nie spełnia jakichś wymagań, wybaczcie ale jest późno, mogę jutro to ogarnąć
### Akumulator
    Magazynuje energię. Dwa ogniwa połączone równolegle zapewniają większą pojemność, czyli dłuższy czas gry.
- https://allegro.pl/produkt/bateria-inncraft-energy-ie-1s2p-p700-7000mah-3-7v-ogniwa-18650-samsung-35e-2dd13f3c-c2a0-4908-941f-81726fd70bf2?srsltid=AU7gw4VZ-tHhzsgP0WTl0SDN4Oj1r0_dFkmJ9HMWXJO6FRvhPgoWQNl_&offerId=17765553536
### Miernik poziomu akumulatora
    Sprawdza poziom naładowania baterii i przekazuje tę informację do STM32.
- https://botland.com.pl/wskazniki-rozladowania/21997-miernik-naladowania-akumulatora-li-pol-li-ion-max17048-stemma-jst-ph-qt-qwiic-adafruit-5580.html
### Przetwornica utrzymująca 5V
    Utrzymuje stałe napięcie 5 V, niezależnie od zmian napięcia akumulatora. Zasila lasery oraz wzmacniacz audio, które wymagają stabilnych 5 V.
- https://botland.com.pl/przetwornice-step-up-step-down/19768-s13v30f5-przetwornica-step-upstep-down-5v-3a-pololu-4082-5904422347529.html
### Regulator do 3,3V
    Obniża napięcie z 5 V do 3,3 V, potrzebnych do zasilania mikrokontrolera STM32WB55 i części elektroniki.
- https://botland.com.pl/przetwornice-step-down/3018-d24v10f3-przetwornica-step-down-33v-1a-pololu-2830-5904422366537.html
## Pytania
- ### Czy warto dodawać bezpiecznik na wypadek zwarcia?

## Nagłośnienie

### Umiejscowienie głośnika
- Ja bym mocno głosowała za umieszczeniem głośnika w podstawie harfy, gdzie możemy mu zrobić porządne pudełko, w którym fale ładnie będą się rozchodzić (jak macie 2 minutki to posłuchajcie różnicy między głośnikiem w dziczy, a głośnikiem zamkniętym https://www.youtube.com/watch?v=RmLTBnpwZMY)

to nasze pudło nie będzie aż tak wielkie jak tam na filmie (mniej więcej 10x10x10cm)
- Pomysł dania głośnika w odpowiedniku "pudła rezonansowego" harfy klasycznej jest super pod względem koncepcyjnym, ale nie będziemy w stanie zapewnić mu takiej przestrzeni, jakiej on potrzebuje, żeby ładnie grać.
- Jeśli chodzi o głośniki podłużne, tak jak @Emilia zapodała pomysł, nie istnieją one za bardzo w formie komponentów, a bardziej jako gotowe urządzenia do podłączenia do telewizora albo na bluetooth - będziemy miały kłopot, żeby to wsadzić do naszej harfy.

 ### W zależności od budżetu jest do wyboru dużo głośników.
- My szukamy głośnika z **szerokim zakresem dźwięków**; skuteczności **80-90db** (głośnik może grać głośniej przy mniejszym poborze energii); **małe wymagania dotyczące obudowy**; możliwie **nie ogromny**, żeby się zmieścił do naszej harfusi

### Wzmacniacz
Według ChataGPT wystarczający będzie taki sam jak mieliśmy ostatnio, zaufam mu (możecie wy poszukać, jeśli ChatGPT was nie przekonał)

https://allegro.pl/produkt/wzmacniacz-max98357a-dekoder-dac-i2s-3w-e61f304e-434a-4804-9a68-e7efd52b16e2?srsltid=AU7gw4Wqodh7l9hu6HcvVeiDK7KKfLtc6YNKECSiBRiRvHNS8iDRDrf9&offerId=16428282656

#### nie będę udawać, że opracowanie poniżej przygotowałam ja xDD

## Głośniki

### 1. Visaton FR 10 HM — około 50 zł

**Typ:** szerokopasmowy, 10 cm

| Parametr | Wartość |
|---|---|
| Impedancja | 4 Ω |
| Moc znamionowa | 20 W |
| Pasmo przenoszenia | 95 Hz – 22 kHz |
| Skuteczność | 86 dB (1 W / 1 m) |
| Fs | 120 Hz |
| Qts | 1,13 |
| Vas | 2,1 l |
| Maksymalne wychylenie | ±2 mm (limit wychylenia) |

### Zalety
- Najniższa cena spośród porównywanych modeli.
- Szerokie deklarowane pasmo wysokich częstotliwości.
- Niewielka średnica ułatwia montaż w podstawie.
- Wystarczający do prostego instrumentu elektronicznego.

### Wady
- Wysoka częstotliwość rezonansowa ogranicza odtwarzanie niskich nut.
- Wysokie Qts utrudnia uzyskanie wyrównanej charakterystyki w małej zamkniętej komorze.
- Mniejszy potencjał odtwarzania niskich częstotliwości.
- Konstrukcja ekonomiczna, bez szczególnych zalet względem droższych modeli pod kątem małej obudowy.

### Ocena do harfy
Dobry wybór budżetowy, ale przy większym budżecie warto rozważyć lepsze przetworniki.

**Specyfikacja:**
https://www.visaton.de/en/products/drivers/fullrange-systems/fr-10-hm-4-ohm

---

### 2. Dayton Audio PC105-4 — około 100 zł

**Typ:** szerokopasmowy, 4 cale (około 10 cm)

| Parametr | Wartość |
|---|---|
| Impedancja | 4 Ω |
| Moc RMS | 40 W |
| Pasmo przenoszenia | 80 Hz – 15 kHz |
| Skuteczność | 90,3 dB (2,83 V / 1 m) |
| Fs | 81,4 Hz |
| Qts | 0,51 |
| Vas | około 3,68 l |
| Xmax | 2 mm |
| Membrana | Kompozyt polimerowy |

### Zalety
- Bardzo dobra skuteczność, korzystna przy zasilaniu bateryjnym.
- Niższa częstotliwość rezonansowa niż Visaton i PS95-8.
- Duża powierzchnia membrany.
- Kompozytowa membrana z tłumieniem rezonansów.
- Miedziana nakładka na nabiegunnik ograniczająca indukcyjność i zniekształcenia.
- Dobry stosunek parametrów do ceny.
- 4 Ω pozwala uzyskać stosunkowo dużą moc przy niskich napięciach zasilania wzmacniacza.

### Wady
- Deklarowane pasmo kończy się na 15 kHz.
- Xmax wynosi 2 mm, czyli połowę wartości RS100-4.
- Większe Vas niż RS100-4 — wymaga analizy zachowania w małej obudowie.
- Nie ma tak dużego zapasu liniowego wychylenia membrany.

### Ocena do harfy
Bardzo mocny kandydat. Wysoka skuteczność i korzystne parametry sprawiają, że dobrze pasuje do instrumentu zasilanego akumulatorowo.

**Najlepszy stosunek możliwości do ceny.**

**Specyfikacja:**
https://www.daytonaudio.com/product/1542/pc105-4-4-full-range-poly-cone-driver

---

### 3. Dayton Audio PS95-8 — około 115–150 zł

**Typ:** szerokopasmowy Point Source, 3,5 cala

| Parametr | Wartość |
|---|---|
| Impedancja | 8 Ω |
| Moc RMS | 10 W |
| Pasmo przenoszenia | 110 Hz – 20 kHz |
| Skuteczność | 85,56 dB (2,83 V / 1 m) |
| Fs | 119,8 Hz |
| Qts | 0,72 |
| Vas | 0,94 l |
| Xmax | 2,5 mm |
| Membrana | Polipropylenowa |

### Zalety
- Kompaktowe wymiary.
- Konstrukcja Point Source z korektorem fazy.
- Odlewany kosz ograniczający drgania konstrukcji.
- Dobre podstawy do szczegółowego odtwarzania średnich i wysokich częstotliwości.
- Szerokie deklarowane pasmo wysokich tonów.
- Niewielkie Vas, korzystne przy projektowaniu małej obudowy.

### Wady
- Wysoka częstotliwość rezonansowa — słabszy potencjał niskich tonów.
- Najmniejsza powierzchnia membrany w zestawieniu.
- Niższa skuteczność niż PC105-4.
- Impedancja 8 Ω może ograniczać uzyskiwaną moc przy wzmacniaczu zasilanym z 5 V.
- Moc znamionowa tylko 10 W.

### Ocena do harfy
Ciekawy wybór do kompaktowego instrumentu, w którym priorytetem są szczegóły średnich i wysokich tonów.

Mniej atrakcyjny, jeśli zależy nam na pełniejszym brzmieniu niższych nut.

**Specyfikacja:**
https://daytonaudio.com/product/1241/ps95-8-3-1-2-point-source-full-range-driver-8-ohm

---

### 4. Dayton Audio RS100-4 — około 200–250 zł

**Typ:** szerokopasmowy Reference, 4 cale

| Parametr | Wartość |
|---|---|
| Impedancja | 4 Ω |
| Moc RMS | 30 W |
| Pasmo przenoszenia | 80 Hz – 20 kHz |
| Skuteczność | 87,5 dB (2,83 V / 1 m) |
| Fs | 84,8 Hz |
| Qts | 0,53 |
| Vas | 1,45 l |
| Xmax | 4 mm |
| Membrana | Anodowane aluminium |
| Zalecana komora zamknięta | około 0,85 l |
| F3 w tej komorze | około 143 Hz |

### Zalety
- Największe liniowe wychylenie membrany spośród porównywanych modeli.
- Większa objętość przemieszczanego powietrza niż w PC105-4.
- Szerokie deklarowane pasmo do 20 kHz.
- Układ magnetyczny z rozwiązaniami ograniczającymi zniekształcenia.
- Sztywna aluminiowa membrana i odlewany kosz.
- Małe Vas i możliwość zastosowania kompaktowej obudowy.
- Dobre parametry do odtwarzania dynamicznych ataków oraz kilku nakładających się nut.

### Wady
- Najwyższa cena w zestawieniu.
- Niższa skuteczność niż PC105-4.
- Większe wychylenie nie gwarantuje lepszego brzmienia wysokich tonów.
- Aluminiowa membrana może wymagać korekcji nierówności charakterystyki.
- W małej obudowie zamkniętej niskie częstotliwości nadal będą mocno ograniczone.
- Nie wykorzystamy jego możliwości bez odpowiedniej obudowy i dobrego toru audio.

### Ocena do harfy
Bardzo ciekawy kandydat do bardziej dopracowanego instrumentu. Ma korzystne parametry pod kątem pracy w małej obudowie i większy zapas wychylenia membrany.

Nie można jednak zagwarantować lepszej jakości brzmienia od PC105-4 bez porównania charakterystyk i odsłuchu.

**Najciekawszy kandydat do dalszego projektowania obudowy.**

**Specyfikacja:**
https://www.daytonaudio.com/product/89/rs100-4-4-reference-full-range-driver-4-ohm

---

## Podsumowanie porównania

| Model | Cena orientacyjna | Główna zaleta | Główna wada |
|---|---:|---|---|
| Visaton FR 10 HM | 50 zł | Niska cena | Ograniczony potencjał niskich tonów |
| Dayton PC105-4 | 100 zł | Wysoka skuteczność | Mniejsze Xmax |
| Dayton PS95-8 | 115–150 zł | Kompaktowość i konstrukcja Point Source | Ograniczone niskie częstotliwości |
| Dayton RS100-4 | 200–250 zł | Duże Xmax i mała zalecana komora | Cena i niższa skuteczność |

## Wnioski

Do dalszego porównania wybieramy:

**Dayton PC105-4**
- Lepszy stosunek ceny do możliwości.
- Wysoka skuteczność korzystna dla zasilania akumulatorowego.
- Potencjalnie wystarczający do bardzo dobrego brzmienia harfy.

**Dayton RS100-4**
- Większy zapas wychylenia membrany.
- Korzystne parametry dla kompaktowej obudowy.
- Ciekawa konstrukcja pod kątem niskich zniekształceń.

### Rekomendacja

RS100-4 jest preferowanym kandydatem do projektu, jeśli priorytetem jest kompaktowa komora głośnikowa i zapas wychylenia membrany.

PC105-4 pozostaje bardzo atrakcyjną alternatywą ze względu na wyższą skuteczność i niższą cenę.

Ostateczny wybór powinien nastąpić po analizie charakterystyk częstotliwościowych, symulacji obudowy i ocenie wymaganego poziomu głośności.

Sam droższy przetwornik nie gwarantuje naturalniejszego dźwięku — kluczowe są również próbki WAV, miksowanie, wzmacniacz i obudowa akustyczna.
