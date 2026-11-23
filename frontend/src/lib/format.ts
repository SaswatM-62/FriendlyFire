/** "1st", "2nd", "3rd", "4th", "11th", "22nd". */
export function ordinal(n: number): string {
  const tens = n % 100;
  if (tens >= 11 && tens <= 13) return `${n}th`;
  switch (n % 10) {
    case 1:
      return `${n}st`;
    case 2:
      return `${n}nd`;
    case 3:
      return `${n}rd`;
    default:
      return `${n}th`;
  }
}

/** 3180 -> "3,180". */
export function formatPoints(points: number): string {
  return points.toLocaleString("en-US");
}

/** Seconds as a clock: 12 -> "0:12", 125 -> "2:05". Negative counts as 0. */
export function formatClock(seconds: number): string {
  const s = Math.max(0, Math.ceil(seconds));
  return `${Math.floor(s / 60)}:${String(s % 60).padStart(2, "0")}`;
}

/** A sync estimate in minutes, rounded up: 170 -> "~3 min", 20 -> "~1 min". */
export function formatEstimate(seconds: number | null | undefined): string {
  if (seconds === null || seconds === undefined) return "a few min";
  return `~${Math.max(1, Math.ceil(seconds / 60))} min`;
}

const MINUTE = 60_000;
const HOUR = 60 * MINUTE;
const DAY = 24 * HOUR;

/** "just now", "5 min ago", "3 hours ago", "yesterday", "2 days ago", "last week", "3 weeks ago". */
export function relativeTime(iso: string, now: number): string {
  const elapsed = now - new Date(iso).getTime();
  if (elapsed < MINUTE) return "just now";
  if (elapsed < HOUR) return `${Math.floor(elapsed / MINUTE)} min ago`;
  if (elapsed < DAY) {
    const hours = Math.floor(elapsed / HOUR);
    return hours === 1 ? "1 hour ago" : `${hours} hours ago`;
  }
  const days = Math.floor(elapsed / DAY);
  if (days === 1) return "yesterday";
  if (days < 7) return `${days} days ago`;
  if (days < 14) return "last week";
  if (days < 60) return `${Math.floor(days / 7)} weeks ago`;
  return new Date(iso).toLocaleDateString("en-GB", { day: "numeric", month: "long", year: "numeric" });
}

/** "12 October 2026". */
export function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString("en-GB", { day: "numeric", month: "long", year: "numeric" });
}

const REGIONS: Record<string, string> = {
  na: "North America",
  eu: "Europe",
  ap: "Asia Pacific",
  kr: "Korea",
  latam: "Latin America",
  br: "Brazil",
};

/** "na" -> "North America"; unknown regions are shown upper-cased. */
export function regionName(region: string): string {
  return REGIONS[region.toLowerCase()] ?? region.toUpperCase();
}

/** "Maya" -> "Maya's room"; "Chris" -> "Chris' room". */
export function roomTitle(hostName: string | undefined): string {
  if (!hostName) return "Quiz";
  return hostName.endsWith("s") ? `${hostName}' room` : `${hostName}'s room`;
}
