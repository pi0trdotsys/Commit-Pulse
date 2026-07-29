import { HISTORY, PL_WEEKDAYS, lastN, sum, weekOverWeek } from "@/lib/mock-commits";
import { PALETTES, type WidgetSettings } from "@/lib/widget-settings";
import { HeatmapStrip } from "./WidgetPreview";

export function NotificationMock({ settings }: { settings: WidgetSettings }) {
  const pal = PALETTES[settings.palette];
  const wow = weekOverWeek();
  const week = lastN(HISTORY, 7);
  const up = wow.direction === "up";

  return (
    <div className="rounded-2xl bg-widget-surface/90 p-3 ring-1 ring-widget-line backdrop-blur-md">
      <div className="flex items-center gap-2 text-[11px] text-widget-muted">
        <span
          className="grid size-4 place-items-center rounded-[5px] text-[9px]"
          style={{ background: pal.accent, color: "oklch(0.16 0.01 250)" }}
        >
          <GitCommitHorizontal className="size-3" strokeWidth={2.4} />
        </span>
        <span>Commit Pulse</span>
        <span>·</span>
        <span>teraz</span>
      </div>

      <p className="mt-2 text-sm font-semibold text-widget-fg">
        Ubiegły tydzień: {sum(week)} commitów
      </p>
      <p className="mt-0.5 flex items-center gap-1.5 text-[12px]">
        <span style={{ color: up ? pal.accent : "oklch(0.68 0.19 25)" }}>
          {up ? "▲" : "▼"} {wow.label}
        </span>
        <span className="text-widget-muted">vs poprzedni tydzień</span>
      </p>

      <div className="mt-3 flex items-end gap-2">
        <HeatmapStrip days={week} heat={pal.heat} size={16} gap={4} radius={3} />
      </div>
      <div className="mt-1 flex gap-1 text-[8px] text-widget-muted">
        {week.map((d, i) => (
          <span key={i} className="w-4 text-center">
            {PL_WEEKDAYS[d.date.getDay()]}
          </span>
        ))}
      </div>
    </div>
  );
}
