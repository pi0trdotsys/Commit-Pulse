## Cel

Klikalna makieta (web) widgetu 2x1 dla Androida + ekrany aplikacji konfiguracyjnej. Bez kodu Kotlin — czysta wizualizacja do dopracowania designu, z fikcyjnymi danymi commitów.

## Widget domyślny (rekomendacja)

```text
┌──────────────────────────────────────────┐
│  12   ▲ +18% vs ub. tydz.        🔥 7d   │
│ dziś                                     │
│  ▪▪▫▪▪▪▪ ▪▪▪▪▪▫▪   (14 dni, heatmapa)    │
└──────────────────────────────────────────┘
```
- Duża liczba commitów dzisiaj (lewa strona, dominanta)
- Delta tydzień/tydzień ze strzałką (kolor: wzrost/spadek/neutral)
- Streak w prawym górnym rogu z ikoną płomienia
- Pasek 14 kwadratów heatmapy na dole, 5 poziomów intensywności

## Ekran 1 — Podgląd i tryby

Makieta telefonu z tapetą, widget renderowany 1:1 w proporcji 2x1 (180×70 dp). Pod spodem karuzela trybów wizualizacji do przełączania na żywo:
1. Heatmapa 14 dni + liczba + streak (domyślny)
2. Sparkline 14 dni + liczba
3. Sam duży licznik + delta WoW
4. Pierścień postępu do celu dziennego + liczba
5. Heatmapa 30 dni (kompaktowa, bez liczby)

## Ekran 2 — Personalizacja

- Paleta: GitHub green / mono / custom (3 warianty tokenów kolorystycznych)
- Tło: przezroczyste / karta / ciemna karta
- Cel dzienny (slider 1–20)
- Zakres dni (7 / 14 / 30)
- Interwał odświeżania (15 min / 1 h / 6 h)
- Akcja tapnięcia (otwórz aplikację / profil GitHub / odśwież)
- Każda zmiana natychmiast widoczna w przypiętym podglądzie widgetu u góry

## Ekran 3 — Powiadomienia tygodniowe

- Przełącznik „Podsumowanie tygodnia", wybór dnia i godziny (domyślnie pon. 9:00)
- Makieta notyfikacji Androida: „Ubiegły tydzień: 47 commitów ▲ +18% vs poprzedni tydzień", mini-heatmapa 7 dni w rozwinięciu
- Opcjonalne alerty: streak zagrożony, cel dzienny osiągnięty

## Ekran 4 — Konto (makieta)

Ekran łączenia z GitHub (PAT / OAuth) — tylko wizualnie, bez realnej integracji.

## Szczegóły techniczne

- TanStack Start, jedna trasa `/` z nawigacją zakładkową między czterema ekranami (stan lokalny, bez backendu)
- Dane: deterministyczny generator fikcyjnej historii commitów w `src/lib/mock-commits.ts` (90 dni), funkcje `todayCount`, `streak`, `weekOverWeek`
- Komponenty: `WidgetPreview` (dispatcher trybów), `HeatmapStrip`, `Sparkline`, `GoalRing`, `PhoneFrame`, `NotificationMock`
- Design system: nowe tokeny oklch w `src/styles.css` — poziomy heatmapy (`--heat-0..4`), akcenty wzrost/spadek, powierzchnia widgetu; ciemny motyw jako domyślny (ekran główny Androida)
- Typografia: kondensowany grotesk na liczby, neutralny sans na etykiety — bez Inter/Poppins
- Zero backendu, zero Lovable Cloud
