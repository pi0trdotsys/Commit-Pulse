import type { ReactNode } from "react";
import { cn } from "@/lib/utils";

export function PhoneFrame({
  children,
  className,
  statusLabel = "9:41",
}: {
  children: ReactNode;
  className?: string;
  statusLabel?: string;
}) {
  return (
    <div
      className={cn(
        "relative w-[300px] rounded-[36px] bg-widget-deep p-2 ring-1 ring-widget-line shadow-[0_30px_80px_-30px_oklch(0_0_0/0.9)]",
        className,
      )}
    >
      <div
        className="relative h-[600px] overflow-hidden rounded-[30px]"
        style={{
          backgroundImage:
            "radial-gradient(120% 80% at 20% 0%, oklch(0.38 0.09 250) 0%, transparent 60%), radial-gradient(100% 70% at 90% 100%, oklch(0.4 0.11 160) 0%, transparent 55%), linear-gradient(160deg, oklch(0.19 0.02 260), oklch(0.13 0.01 260))",
        }}
      >
        <div className="flex items-center justify-between px-5 pt-3 text-[11px] text-widget-fg/80">
          <span>{statusLabel}</span>
          <span className="tracking-tight">▮▮▮ ▲ 84%</span>
        </div>
        <div className="px-4 pt-8">{children}</div>
      </div>
    </div>
  );
}
