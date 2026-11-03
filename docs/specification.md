# Specyfikacja Techinczna HARFY.

* **Wersja** 1.0 (Single Source of Truth)
* **Data:** 2026-10-31

--- 

## 1. Architektura Systemu i Peryferia

### 1.1 Czujniki Strun (15 strun)
* **Liczba strun:** System składa się z dokładnie **15 strun optycznych** reprezentujących 2 oktawy + C dźwięków ([mapowanie strun](./note-mapping.md)).
* **Konstrukcja:** Każda struna tworzona jest przez dedykowaną **diodę laserową** oraz odbiornik w postaci **fototranzystora** (15 par).
* **Detekcja:** Przerwanie wiązki światła laserowego powoduje natychmiastowe wyzwolenie dźwięku (próbka WAV) oraz przesłanie komunikatu BLE.

### 1.2 Przełączniki Dźwigniowe (funkcja pedałów harfy) - 7 przełączników
* **Zasada działania:** Przełączniki ON-OFF-ON pełnią funkcję analogiczną do **pedałów w harfie klasycznej**.
* **Modulacja dźwięku:** Pozwalają na dynamiczną zmianę wysokości generowanego dźwięku o pół tonu w górę bądź dół dla przypisanych strun, po 1 na każdą nazwę dźwięku we wszystkich oktawach (C, D, E, F, G, A, H).

### 1.3 Wskaźniki i Wyświetlacz
* **Wyświetlacz OLED:** Służy do monitorowania **poziomu naładowania baterii**.
* **Diody LED (tryb nauki):** Dedykowane diody umieszczone bezpośrednio nad 15 strunami, podświetlają odpowiednie struny w trybie nauki współpracującym z aplikacją mobilną.

---

## 2. Tor Audio i Głośnik

* **Wzmacniacz:** Cyfrowy wzmacniacz Klasy D **MAX98357A** podłączony bezpośrednio do magistrali I2s mikrokontrolera.
* **Głośnik:** Szerokopasmowy głośnik **Visaton FRS 8 (4 Ohm)**.

---

## 3. Zasilanie i Zarządanie Energią

* **Ogniwo zasilające:** Akumulator Li-Ion **Samsung INR18650-35E 3400mAh (1S)**.
* **Moduł zarządzania zasilaniem:** **Waveshare 18603** (obsługa ładowania USB-C, ochrona ogniw oraz dystrybucja napięć).

---

## 4. Mikrokontroler i Łączność

* **Mikrokontroler:** **STM32WB55** ze zintegrowanym modułem BLE 5.2.
* **Komunikacja BLE:** Bezprzewodowa transmisja komunikatów i komend sterujących do dedykowanej aplikacji i spowrotem (do aplikacji: grane dźwięki i obecne tonacje, do harfy: kolejna nuta do nauki).
([protokół bluetooth](./protocol/ble-protocol.md)).

## 5. Konstrukcja Mechaniczna

* **Obudowa:** Rama urządzenia, elementy nośne oraz precyzyjne mocowania czujników laserowych drukowane w technologii 3D z filamentu naśladując kształt harfy klasycznej.
