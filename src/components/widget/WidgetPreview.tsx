import { cn } from "@/lib/utils";
import {
  HISTORY,
  lastN,
  level,
  maxOf,
  streak,
  todayCount,
  weekOverWeek,
  type DayCommit,
} from "@/lib/mock-commits";
import { PALETTES, type WidgetSettings } from "@/lib/widget-settings";

function Delta({ accent, compact }: { accent: string; compact?: boolean }) {
  const wow = weekOverWeek();
  const color =
    wow.direction === "up"
      ? accent
      : wow.direction === "down"
        ? "oklch(0.68 0.19 25)"
        : "oklch(0.65 0.01 250)";
  const arrow = wow.direction === "up" ? "▲" : wow.direction === "down" ? "▼" : "▬";
  return (
    <span
      className="inline-flex items-baseline gap-1 text-[11px] leading-none tracking-tight"
      style={{ color }}
    >
      <span className="text-[9px]">{arrow}</span>
      <span className="font-semibold">{wow.label}</span>
      {!compact && (
        <span className="text-widget-muted text-[10px]">vs ub. tydz.</span>
      )}
    </span>
  );
}

export function HeatmapStrip({
  days,
  heat,
  size = 12,
  gap = 3,
  radius = 2,
}: {
  days: DayCommit[];
  heat: string[];
  size?: number;
  gap?: number;
  radius?: number;
}) {
  const max = maxOf(days);
  return (
    <div className="flex items-end" style={{ gap }}>
      {days.map((d, i) => (
        <span
          key={i}
          title={`${d.date.toLocaleDateString("pl-PL")}: ${d.count}`}
          style={{
            width: size,
            height: size,
            borderRadius: radius,
            background: heat[level(d.count, max)],
          }}
        />
      ))}
    </div>
  );
}

export function Sparkline({
  days,
  accent,
  width = 150,
  height = 26,
}: {
  days: DayCommit[];
  accent: string;
  width?: number;
  height?: number;
}) {
  const max = Math.max(maxOf(days), 1);
  const step = width / Math.max(days.length - 1, 1);
  const pts = days.map((d, i) => [i * step, height - (d.count / max) * (height - 3) - 1.5]);
  const line = pts.map((p, i) => `${i === 0 ? "M" : "L"}${p[0].toFixed(1)},${p[1].toFixed(1)}`).join(" ");
  const area = `${line} L${width},${height} L0,${height} Z`;
  return (
    <svg width={width} height={height} className="overflow-visible">
      <path d={area} fill={accent} opacity={0.16} />
      <path d={line} fill="none" stroke={accent} strokeWidth={1.6} strokeLinejoin="round" />
      <circle cx={pts[pts.length - 1][0]} cy={pts[pts.length - 1][1]} r={2.2} fill={accent} />
    </svg>
  );
}

export function GoalRing({
  value,
  goal,
  accent,
  size = 46,
}: {
  value: number;
  goal: number;
  accent: string;
  size?: number;
}) {
  const r = size / 2 - 4;
  const c = 2 * Math.PI * r;
  const pct = Math.min(value / Math.max(goal, 1), 1);
  return (
    <svg width={size} height={size} className="-rotate-90">
      <circle cx={size / 2} cy={size / 2} r={r} fill="none" strokeWidth={4} stroke="oklch(0.32 0.01 250)" />
      <circle
        cx={size / 2}
        cy={size / 2}
        r={r}
        fill="none"
        strokeWidth={4}
        stroke={accent}
        strokeLinecap="round"
        strokeDasharray={`${c * pct} ${c}`}
      />
    </svg>
  );
}

const SURFACES: Record<WidgetSettings["surface"], string> = {
  transparent: "bg-transparent",
  card: "bg-widget-surface/85 backdrop-blur-md ring-1 ring-widget-line",
  dark: "bg-widget-deep ring-1 ring-widget-line",
};

/** Widget 2x1 ≈ 180x70 dp, renderowany w skali. */
export function WidgetPreview({
  settings,
  scale = 1.6,
  className,
}: {
  settings: WidgetSettings;
  scale?: number;
  className?: string;
}) {
  const pal = PALETTES[settings.palette];
  const today = todayCount();
  const st = streak();
  const days = lastN(HISTORY, settings.mode === "heatmap30" ? 30 : settings.range);

  return (
    <div
      className={cn("origin-top-left", className)}
      style={{ width: 180 * scale, height: 70 * scale }}
    >
      <div
        className={cn(
          "flex h-[70px] w-[180px] origin-top-left flex-col justify-between rounded-[14px] px-3 py-2 text-widget-fg",
          SURFACES[settings.surface],
        )}
        style={{ transform: `scale(${scale})` }}
      >
        {settings.mode === "heatmap" && (
          <>
            <div className="flex items-start justify-between">
              <div className="flex items-baseline gap-1.5">
                <span className="font-display text-[30px] leading-[0.8] tracking-tight">{today}</span>
                <span className="text-widget-muted text-[9px] uppercase tracking-[0.14em]">dziś</span>
              </div>
              <span
                className="flex items-center gap-0.5 rounded-full px-1.5 py-0.5 text-[9px] font-semibold"
                style={{ background: `color-mix(in oklab, ${pal.accent} 18%, transparent)`, color: pal.accent }}
              >
                <Flame className="size-2.5" strokeWidth={2.4} /> {st}d
              </span>
            </div>
            <Delta accent={pal.accent} />
            <HeatmapStrip days={lastN(HISTORY, 14)} heat={pal.heat} size={9.5} gap={2.5} />
          </>
        )}

        {settings.mode === "sparkline" && (
          <>
            <div className="flex items-start justify-between">
              <div className="flex items-baseline gap-1.5">
                <span className="font-display text-[30px] leading-[0.8] tracking-tight">{today}</span>
                <span className="text-widget-muted text-[9px] uppercase tracking-[0.14em]">dziś</span>
              </div>
              <Delta accent={pal.accent} compact />
            </div>
            <Sparkline days={days} accent={pal.accent} width={156} height={24} />
          </>
        )}

        {settings.mode === "counter" && (
          <div className="flex h-full flex-col justify-center gap-1">
            <div className="flex items-baseline gap-2">
              <span className="font-display text-[44px] leading-[0.78] tracking-tight">{today}</span>
              <span className="text-widget-muted text-[9px] uppercase leading-tight tracking-[0.14em]">
                commitów
                <br />
                dzisiaj
              </span>
            </div>
            <Delta accent={pal.accent} />
          </div>
        )}

        {settings.mode === "goal" && (
          <div className="flex h-full items-center gap-3">
            <div className="relative shrink-0">
              <GoalRing value={today} goal={settings.goal} accent={pal.accent} />
              <span className="font-display absolute inset-0 flex items-center justify-center text-[15px]">
                {today}
              </span>
            </div>
            <div className="flex flex-col gap-1">
              <span className="text-[10px] font-semibold">
                cel {settings.goal}/dzień
              </span>
              <Delta accent={pal.accent} />
              <span className="text-widget-muted flex items-center gap-1 text-[9px]">
                <Flame className="size-2.5" strokeWidth={2.4} /> seria {st} dni
              </span>
            </div>
          </div>
        )}

        {settings.mode === "heatmap30" && (
          <div className="flex h-full flex-col justify-center gap-2">
            <div className="flex items-center justify-between">
              <span className="text-widget-muted text-[9px] uppercase tracking-[0.16em]">30 dni</span>
              <Delta accent={pal.accent} compact />
            </div>
            <HeatmapStrip days={days} heat={pal.heat} size={4.4} gap={1.6} radius={1} />
          </div>
        )}
      </div>
    </div>
  );
}
