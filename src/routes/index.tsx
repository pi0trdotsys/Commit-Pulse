import { createFileRoute } from "@tanstack/react-router";
import { useState } from "react";

import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Slider } from "@/components/ui/slider";
import { Switch } from "@/components/ui/switch";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { cn } from "@/lib/utils";

import { PhoneFrame } from "@/components/widget/PhoneFrame";
import { NotificationMock } from "@/components/widget/NotificationMock";
import { WidgetPreview, HeatmapStrip } from "@/components/widget/WidgetPreview";
import { HISTORY, lastN, streak, sum, todayCount, weekOverWeek } from "@/lib/mock-commits";
import {
  DEFAULT_SETTINGS,
  MODES,
  PALETTES,
  type Palette,
  type Range,
  type Surface,
  type WidgetSettings,
} from "@/lib/widget-settings";

export const Route = createFileRoute("/")({
  head: () => ({
    meta: [
      { title: "Commit Pulse — makieta widgetu 2x1 na Androida" },
      {
        name: "description",
        content:
          "Interaktywna makieta widgetu 2x1 z heatmapą commitów, licznikiem dnia, serią i porównaniem tydzień do tygodnia oraz ekranami aplikacji konfiguracyjnej.",
      },
      { property: "og:title", content: "Commit Pulse — makieta widgetu 2x1 na Androida" },
      {
        property: "og:description",
        content:
          "Heatmapa 14 dni, duża liczba commitów, streak i delta tydzień/tydzień — klikalny konfigurator widgetu.",
      },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary_large_image" },
    ],
  }),
  component: Index,
});

function Chip({
  active,
  onClick,
  children,
}: {
  active: boolean;
  onClick: () => void;
  children: React.ReactNode;
}) {
  return (
    <button
      onClick={onClick}
      className={cn(
        "rounded-full border px-3 py-1.5 text-xs font-medium transition-colors",
        active
          ? "border-transparent bg-foreground text-background"
          : "border-border text-muted-foreground hover:text-foreground",
      )}
    >
      {children}
    </button>
  );
}

function Section({ title, hint, children }: { title: string; hint?: string; children: React.ReactNode }) {
  return (
    <div className="space-y-3">
      <div>
        <h3 className="text-sm font-semibold">{title}</h3>
        {hint && <p className="text-xs text-muted-foreground">{hint}</p>}
      </div>
      {children}
    </div>
  );
}

function Index() {
  const [s, setS] = useState<WidgetSettings>(DEFAULT_SETTINGS);
  const set = <K extends keyof WidgetSettings>(k: K, v: WidgetSettings[K]) =>
    setS((prev) => ({ ...prev, [k]: v }));

  const wow = weekOverWeek();
  const pal = PALETTES[s.palette];

  return (
    <main className="min-h-screen bg-background text-foreground">
      <header className="border-b border-border/60">
        <div className="mx-auto flex max-w-6xl flex-wrap items-end justify-between gap-4 px-6 py-8">
          <div>
            <p className="text-xs uppercase tracking-[0.28em] text-muted-foreground">
              Makieta · Android widget 2×1
            </p>
            <h1 className="font-display mt-2 text-5xl leading-none tracking-tight">
              Commit&nbsp;Pulse
            </h1>
            <p className="mt-2 max-w-lg text-sm text-muted-foreground">
              Heatmapa 14 dni + duża liczba commitów dzisiaj + seria w rogu, a pod spodem
              porównanie tydzień do tygodnia. Reszta trybów przełączalna w aplikacji.
            </p>
          </div>
          <div className="flex gap-6 text-right">
            <Stat label="dziś" value={String(todayCount())} />
            <Stat label="seria" value={`${streak()} dni`} />
            <Stat label="7 dni" value={String(sum(lastN(HISTORY, 7)))} />
            <Stat label="w/w" value={wow.label} accent />
          </div>
        </div>
      </header>

      <div className="mx-auto max-w-6xl px-6 py-10">
        <Tabs defaultValue="preview" className="gap-8">
          <TabsList className="grid w-full max-w-2xl grid-cols-4">
            <TabsTrigger value="preview">Podgląd</TabsTrigger>
            <TabsTrigger value="style">Personalizacja</TabsTrigger>
            <TabsTrigger value="notify">Powiadomienia</TabsTrigger>
            <TabsTrigger value="account">Konto</TabsTrigger>
          </TabsList>

          {/* 1 — podgląd i tryby */}
          <TabsContent value="preview" className="grid gap-10 lg:grid-cols-[auto_1fr]">
            <PhoneFrame>
              <WidgetPreview settings={s} scale={1.42} />
              <div className="mt-6 grid grid-cols-4 gap-4 opacity-60">
                {Array.from({ length: 8 }).map((_, i) => (
                  <div key={i} className="space-y-1.5">
                    <div className="aspect-square rounded-2xl bg-widget-surface/60" />
                    <div className="h-1.5 w-3/4 rounded-full bg-widget-surface/60" />
                  </div>
                ))}
              </div>
            </PhoneFrame>

            <div className="space-y-4">
              <Section title="Tryb wizualizacji" hint="Kliknij, żeby zobaczyć podmianę na ekranie głównym.">
                <div className="space-y-3">
                  {MODES.map((m) => (
                    <button
                      key={m.id}
                      onClick={() => set("mode", m.id)}
                      className={cn(
                        "flex w-full items-center gap-5 rounded-2xl border p-4 text-left transition-colors",
                        s.mode === m.id
                          ? "border-foreground/40 bg-card"
                          : "border-border hover:border-foreground/25",
                      )}
                    >
                      <div className="shrink-0 overflow-hidden rounded-xl bg-widget-deep p-2">
                        <WidgetPreview settings={{ ...s, mode: m.id }} scale={0.78} />
                      </div>
                      <div>
                        <p className="text-sm font-semibold">{m.name}</p>
                        <p className="text-xs text-muted-foreground">{m.desc}</p>
                      </div>
                    </button>
                  ))}
                </div>
              </Section>
            </div>
          </TabsContent>

          {/* 2 — personalizacja */}
          <TabsContent value="style" className="grid gap-10 lg:grid-cols-[auto_1fr]">
            <div className="lg:sticky lg:top-8 lg:self-start">
              <div className="rounded-3xl bg-widget-deep p-6 ring-1 ring-widget-line">
                <WidgetPreview settings={s} scale={1.55} />
              </div>
              <p className="mt-3 text-xs text-muted-foreground">Podgląd na żywo · 180 × 70 dp</p>
            </div>

            <div className="grid gap-8 sm:grid-cols-2">
              <Section title="Paleta">
                <div className="flex flex-wrap gap-2">
                  {(Object.keys(PALETTES) as Palette[]).map((p) => (
                    <Chip key={p} active={s.palette === p} onClick={() => set("palette", p)}>
                      {PALETTES[p].name}
                    </Chip>
                  ))}
                </div>
                <div className="flex gap-1.5 pt-1">
                  {pal.heat.map((c) => (
                    <span key={c} className="size-6 rounded-md" style={{ background: c }} />
                  ))}
                </div>
              </Section>

              <Section title="Tło widgetu">
                <div className="flex flex-wrap gap-2">
                  {(
                    [
                      ["transparent", "Przezroczyste"],
                      ["card", "Karta"],
                      ["dark", "Ciemna karta"],
                    ] as [Surface, string][]
                  ).map(([v, label]) => (
                    <Chip key={v} active={s.surface === v} onClick={() => set("surface", v)}>
                      {label}
                    </Chip>
                  ))}
                </div>
              </Section>

              <Section title={`Cel dzienny — ${s.goal} commitów`}>
                <Slider
                  value={[s.goal]}
                  min={1}
                  max={20}
                  step={1}
                  onValueChange={([v]) => set("goal", v)}
                />
              </Section>

              <Section title="Zakres dni">
                <div className="flex gap-2">
                  {([7, 14, 30] as Range[]).map((r) => (
                    <Chip key={r} active={s.range === r} onClick={() => set("range", r)}>
                      {r} dni
                    </Chip>
                  ))}
                </div>
              </Section>

              <Section title="Odświeżanie">
                <div className="flex gap-2">
                  {(["15m", "1h", "6h"] as const).map((r) => (
                    <Chip key={r} active={s.refresh === r} onClick={() => set("refresh", r)}>
                      {r === "15m" ? "15 min" : r === "1h" ? "1 godz." : "6 godz."}
                    </Chip>
                  ))}
                </div>
              </Section>

              <Section title="Akcja tapnięcia">
                <div className="flex flex-wrap gap-2">
                  {(
                    [
                      ["app", "Otwórz aplikację"],
                      ["profile", "Profil GitHub"],
                      ["refresh", "Odśwież"],
                    ] as [WidgetSettings["tapAction"], string][]
                  ).map(([v, label]) => (
                    <Chip key={v} active={s.tapAction === v} onClick={() => set("tapAction", v)}>
                      {label}
                    </Chip>
                  ))}
                </div>
              </Section>
            </div>
          </TabsContent>

          {/* 3 — powiadomienia */}
          <TabsContent value="notify" className="grid gap-10 lg:grid-cols-[auto_1fr]">
            <PhoneFrame statusLabel="Pon 9:00">
              <NotificationMock settings={s} />
              <div className="mt-3 rounded-2xl bg-widget-surface/50 p-3 text-[11px] text-widget-muted">
                Przesuń, aby odrzucić · Otwórz podsumowanie
              </div>
            </PhoneFrame>

            <div className="space-y-8">
              <Section title="Podsumowanie tygodnia" hint="Wysyłane po zamknięciu minionego tygodnia.">
                <Card className="flex flex-row items-center justify-between p-4">
                  <Label htmlFor="digest" className="text-sm">
                    Włącz cotygodniowe podsumowanie
                  </Label>
                  <Switch
                    id="digest"
                    checked={s.weeklyDigest}
                    onCheckedChange={(v) => set("weeklyDigest", v)}
                  />
                </Card>

                <div className="flex flex-wrap items-end gap-4">
                  <div className="space-y-2">
                    <Label className="text-xs text-muted-foreground">Dzień</Label>
                    <div className="flex gap-1.5">
                      {["Pn", "Wt", "Śr", "Cz", "Pt", "So", "Nd"].map((d, i) => (
                        <Chip key={d} active={s.digestDay === i + 1} onClick={() => set("digestDay", i + 1)}>
                          {d}
                        </Chip>
                      ))}
                    </div>
                  </div>
                  <div className="w-28 space-y-2">
                    <Label className="text-xs text-muted-foreground">Godzina</Label>
                    <Input
                      type="time"
                      value={s.digestHour}
                      onChange={(e) => set("digestHour", e.target.value)}
                    />
                  </div>
                </div>
              </Section>

              <Section title="Alerty dodatkowe">
                <Card className="divide-y divide-border p-0">
                  <div className="flex items-center justify-between p-4">
                    <div>
                      <p className="text-sm">Seria zagrożona</p>
                      <p className="text-xs text-muted-foreground">
                        Ping o 20:00, jeśli dziś brak commita.
                      </p>
                    </div>
                    <Switch checked={s.alertStreak} onCheckedChange={(v) => set("alertStreak", v)} />
                  </div>
                  <div className="flex items-center justify-between p-4">
                    <div>
                      <p className="text-sm">Cel dzienny osiągnięty</p>
                      <p className="text-xs text-muted-foreground">Krótkie potwierdzenie po {s.goal} commitach.</p>
                    </div>
                    <Switch checked={s.alertGoal} onCheckedChange={(v) => set("alertGoal", v)} />
                  </div>
                </Card>
              </Section>

              <Section title="Ostatnie 14 dni" hint="Dane, na których liczona jest delta tydzień/tydzień.">
                <div className="rounded-2xl bg-widget-deep p-5 ring-1 ring-widget-line">
                  <HeatmapStrip days={lastN(HISTORY, 14)} heat={pal.heat} size={20} gap={5} radius={4} />
                  <div className="mt-4 flex gap-6 text-xs text-muted-foreground">
                    <span>
                      Ten tydzień: <span className="text-foreground">{wow.thisWeek}</span>
                    </span>
                    <span>
                      Poprzedni: <span className="text-foreground">{wow.lastWeek}</span>
                    </span>
                    <span style={{ color: pal.accent }}>{wow.label}</span>
                  </div>
                </div>
              </Section>
            </div>
          </TabsContent>

          {/* 4 — konto */}
          <TabsContent value="account" className="max-w-md space-y-6">
            <Section title="Połącz z GitHub" hint="Makieta — brak realnej integracji.">
              <Card className="space-y-4 p-6">
                <Button className="w-full">Zaloguj przez GitHub (OAuth)</Button>
                <div className="flex items-center gap-3 text-xs text-muted-foreground">
                  <span className="h-px flex-1 bg-border" /> albo <span className="h-px flex-1 bg-border" />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="pat" className="text-xs text-muted-foreground">
                    Personal Access Token (scope: read:user, repo)
                  </Label>
                  <Input id="pat" placeholder="ghp_••••••••••••••••" />
                </div>
                <Button variant="secondary" className="w-full">
                  Zapisz token
                </Button>
              </Card>
            </Section>

            <Section title="Źródła aktywności">
              <Card className="divide-y divide-border p-0 text-sm">
                {["Commity publiczne", "Commity prywatne", "Pull requesty", "Review'y"].map((x, i) => (
                  <div key={x} className="flex items-center justify-between p-4">
                    <span>{x}</span>
                    <Switch defaultChecked={i < 2} />
                  </div>
                ))}
              </Card>
            </Section>
          </TabsContent>
        </Tabs>
      </div>
    </main>
  );
}

function Stat({ label, value, accent }: { label: string; value: string; accent?: boolean }) {
  return (
    <div>
      <p
        className={cn("font-display text-3xl leading-none tracking-tight")}
        style={accent ? { color: "var(--trend-up)" } : undefined}
      >
        {value}
      </p>
      <p className="mt-1 text-[10px] uppercase tracking-[0.18em] text-muted-foreground">{label}</p>
    </div>
  );
}
