export type WidgetMode = "heatmap" | "sparkline" | "counter" | "goal" | "heatmap30";
export type Palette = "github" | "mono" | "custom";
export type Surface = "transparent" | "card" | "dark";
export type Range = 7 | 14 | 30;

export type WidgetSettings = {
  mode: WidgetMode;
  palette: Palette;
  surface: Surface;
  goal: number;
  range: Range;
  refresh: "15m" | "1h" | "6h";
  tapAction: "app" | "profile" | "refresh";
  weeklyDigest: boolean;
  digestDay: number;
  digestHour: string;
  alertStreak: boolean;
  alertGoal: boolean;
};

export const DEFAULT_SETTINGS: WidgetSettings = {
  mode: "heatmap",
  palette: "github",
  surface: "card",
  goal: 8,
  range: 14,
  refresh: "1h",
  tapAction: "app",
  weeklyDigest: true,
  digestDay: 1,
  digestHour: "09:00",
  alertStreak: true,
  alertGoal: false,
};

export const MODES: { id: WidgetMode; name: string; desc: string }[] = [
  { id: "heatmap", name: "Heatmapa + liczba", desc: "14 dni, licznik dnia, streak — rekomendowany" },
  { id: "sparkline", name: "Sparkline", desc: "Krzywa 14 dni + licznik dnia" },
  { id: "counter", name: "Licznik", desc: "Duża liczba i delta tydzień/tydzień" },
  { id: "goal", name: "Pierścień celu", desc: "Postęp do celu dziennego" },
  { id: "heatmap30", name: "Heatmapa 30 dni", desc: "Kompaktowa, sam rytm pracy" },
];

export const PALETTES: Record<Palette, { name: string; heat: string[]; accent: string }> = {
  github: {
    name: "GitHub green",
    heat: [
      "oklch(0.28 0.02 160)",
      "oklch(0.45 0.09 158)",
      "oklch(0.58 0.13 157)",
      "oklch(0.71 0.17 156)",
      "oklch(0.84 0.19 152)",
    ],
    accent: "oklch(0.78 0.18 154)",
  },
  mono: {
    name: "Mono",
    heat: [
      "oklch(0.28 0.005 250)",
      "oklch(0.42 0.005 250)",
      "oklch(0.56 0.005 250)",
      "oklch(0.72 0.005 250)",
      "oklch(0.93 0.005 250)",
    ],
    accent: "oklch(0.93 0.005 250)",
  },
  custom: {
    name: "Custom / amber",
    heat: [
      "oklch(0.28 0.02 60)",
      "oklch(0.47 0.09 62)",
      "oklch(0.62 0.14 66)",
      "oklch(0.76 0.16 70)",
      "oklch(0.87 0.17 82)",
    ],
    accent: "oklch(0.83 0.17 76)",
  },
};
