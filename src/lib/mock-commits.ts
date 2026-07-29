// Deterministyczny generator fikcyjnej historii commitów (bez backendu).

export type DayCommit = { date: Date; count: number };

function mulberry32(seed: number) {
  return function () {
    seed |= 0;
    seed = (seed + 0x6d2b79f5) | 0;
    let t = Math.imul(seed ^ (seed >>> 15), 1 | seed);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

/** 90 dni historii, ostatni element = dzisiaj. */
export function generateHistory(days = 90, seed = 20260729): DayCommit[] {
  const rand = mulberry32(seed);
  const today = new Date();
  today.setHours(0, 0, 0, 0);

  const out: DayCommit[] = [];
  for (let i = days - 1; i >= 0; i--) {
    const date = new Date(today);
    date.setDate(today.getDate() - i);
    const weekday = date.getDay();
    const weekendPenalty = weekday === 0 || weekday === 6 ? 0.45 : 1;
    // lekki trend wzrostowy pod koniec okresu
    const trend = 0.7 + (0.6 * (days - i)) / days;
    const r = rand();
    let count = 0;
    if (r > 0.14 / weekendPenalty) {
      count = Math.round(rand() * 11 * weekendPenalty * trend);
    }
    out.push({ date, count });
  }
  return out;
}

export const HISTORY = generateHistory();

export function lastN(history: DayCommit[], n: number): DayCommit[] {
  return history.slice(-n);
}

export function todayCount(history: DayCommit[] = HISTORY): number {
  return history[history.length - 1]?.count ?? 0;
}

export function streak(history: DayCommit[] = HISTORY): number {
  let s = 0;
  for (let i = history.length - 1; i >= 0; i--) {
    if (history[i].count > 0) s++;
    else break;
  }
  return s;
}

export function sum(days: DayCommit[]): number {
  return days.reduce((a, d) => a + d.count, 0);
}

export type WeekDelta = {
  thisWeek: number;
  lastWeek: number;
  percent: number;
  direction: "up" | "down" | "flat";
  label: string;
};

export function weekOverWeek(history: DayCommit[] = HISTORY): WeekDelta {
  const thisWeek = sum(history.slice(-7));
  const lastWeek = sum(history.slice(-14, -7));
  const percent =
    lastWeek === 0
      ? thisWeek > 0
        ? 100
        : 0
      : Math.round(((thisWeek - lastWeek) / lastWeek) * 100);
  const direction = percent > 1 ? "up" : percent < -1 ? "down" : "flat";
  const sign = percent > 0 ? "+" : "";
  return {
    thisWeek,
    lastWeek,
    percent,
    direction,
    label: `${sign}${percent}%`,
  };
}

/** Poziom intensywności 0..4 dla heatmapy. */
export function level(count: number, max: number): 0 | 1 | 2 | 3 | 4 {
  if (count <= 0) return 0;
  const q = count / Math.max(max, 1);
  if (q <= 0.25) return 1;
  if (q <= 0.5) return 2;
  if (q <= 0.75) return 3;
  return 4;
}

export function maxOf(days: DayCommit[]): number {
  return days.reduce((a, d) => Math.max(a, d.count), 0);
}

export const PL_WEEKDAYS = ["Nd", "Pn", "Wt", "Śr", "Cz", "Pt", "So"];
