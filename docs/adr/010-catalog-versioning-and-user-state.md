# ADR 010: Wersjonowany katalog tylko do odczytu i oddzielenie stanu użytkownika w Room

* **Stan:** Przyjęty
* **Data:** 2026-03-31
* **Osoby decyzyjne:** Alicja Kowalska

## Kontekst

W pierwotnej architekturze aplikacji cały katalog utworów (wraz z informacją o ulubionych) był serializowany z pliku `songs.json` i zapisywany bezpośrednio do tabeli `songs` w bazie danych SQLite (Room) podczas inicjalizacji aplikacji. 

Pojawiły się następujące problemy:
1. **Brak odporności na aktualizacje katalogu:** Każda zmiana struktury bazy, migracja niszcząca lub aktualizacja pliku `songs.json` w kolejnej wersji aplikacji powodowała nadpisanie danych lub utratę preferencji użytkownika (ulubione, postęp).
2. **Niestabilne identyfikatory:** Użycie auto-inkrementowanego klucza głównego `id: Int` w bazie Room uniemożliwiało jednoznaczne i stabilne powiązanie ulubionych oraz postępów użytkownika z konkretnym utworem po zmianie katalogu.
3. **Mieszanie warstw:** Encje bazy danych były ściśle powiązane ze strukturą danych katalogowych, a modele magazynowania przenikały do warstwy prezentacji.

## Rozważane alternatywy

### Opcja A: Trzymanie katalogu i stanu użytkownika razem w Room z migracjami
* **Zalety:**
  * Jedno źródło danych dla całej listy utworów.
* **Wady:**
  * Wysoka złożoność migracji przy każdej zmianie nut lub metadanych utworów w `songs.json`.
  * Ryzyko usunięcia danych użytkownika przy błędach migrowania lub aktualizacji katalogu.

### Opcja B: Oddzielenie wersjonowanego katalogu tylko do odczytu od stanu użytkownika w Room za pomocą stabilnego `songId` (Wybrana)
* **Zalety:**
  * Katalog w `assets/songs.json` jest traktowany jako wersjonowane, statyczne dane tylko do odczytu (wygodna aktualizacja katalogu w nowych wersjach aplikacji bez ryzyka naruszenia bazy).
  * Baza Room przechowuje wyłącznie stan użytkownika (ulubione w `favorite_songs`, postęp w `song_progress`) powiązany stabilnym kluczem biznesowym `songId: String`.
  * Pełne rozdzielenie DTO z assets, encji Room i czystych modeli domenowych.
* **Wady:**
  * Konieczność scalania danych katalogu z danymi z Room w warstwie repozytorium (`SongRepositoryImpl`).

## Decyzja

Wybrano **Opcję B**. Katalog utworów jest odczytywany jako niezmienne dane tylko do odczytu (`SongCatalogDataSource`), aktualizowane razem z wersją aplikacji, a baza danych Room odpowiada za trwałość danych użytkownika związanych z katalogiem (ulubione i postęp nauki) powiązanych ze stabilnym `songId: String`. Ustawienia aplikacji pozostają w DataStore, zgodnie z ADR-008.

## Konsekwencje

* Aktualizacje katalogu utworów (np. dodanie nowych utworów lub korekta nut w `songs.json`) nie powodują utraty ulubionych ani postępów użytkownika.
* Migracje z poprzedniej tabeli `songs` przenoszą istniejące ulubione do `favorite_songs`. Dla najstarszej wersji bazy stabilne `songId` są odtwarzane na podstawie historycznej kolejności katalogu, dlatego jej zmiana wymaga aktualizacji mapowania migracji.
* Zmiany treści utworu są wykrywane przez fingerprint lekcji, aby nie odtwarzać niekompatybilnego postępu. Postęp przeniesiony z wersji 16 nie ma jeszcze fingerprintu i zostanie opatrzony nim przy pierwszym zapisie.
* UI oraz ViewModele operują wyłącznie na modelach domenowych (`Song`, `LessonProgress`) i stabilnym identyfikatorze `songId`.
* Źródła danych, repozytorium i przywracanie postępu są weryfikowalne testami jednostkowymi; testy integracyjne samych migracji Room pozostają osobnym zakresem.
