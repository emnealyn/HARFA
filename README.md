# HARFA - System syntezy dźwięku oparty na detekcji przerwania wiązki laserowej

> **Projekt inżynierski** - Politechnika Poznańska  
> **Wydział:** Informatyka i Telekomunikacja (Informatyka Stacjonarna)  
> **Promotor:** dr inż. Przemysław Zakrzewski  
> **Wykonawcy:** Emilia Bonk, Alicja Kowalska, Joanna Walczak

---

## O projekcie

---

## Architektura systemu

- [Granice odpowiedzialności STM32WB55 i aplikacji mobilnej](docs/architecture/001-system-responsibilities.md)

--- 

## Struktura repozytorium

```text
.
├── docs/                   # Dokumentacja techniczna i materiały dyplomowe
├── firmware/               # Oprogramowanie układowe mikrokontrolera STM32 (C/HAL)
│   ├── mock/               # Skrypty testowe i symulatory komunikacji
│   └── src/                # Główny kod źródłowy (odczyt czujników, synteza, BT, LED)
├── hardware/               # Pliki projektu fizycznego
│   ├── 3d/                 # Modele 3D obudowy do druku (STEP, STL, Fusion 360)
│   └── wiring/             # Schematy elektryczne, pinout i instrukcje lutowania
├── mobile/                 # Aplikacja mobilna Android (Kotlin, Jetpack Compose)
│   ├── app/                # Kod źródłowy aplikacji
│   └── build.gradle.kts
└── README.md
```

---

## Stos technologiczy

---

## Licencja i własności intelektualne
