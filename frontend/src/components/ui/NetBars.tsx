import type { ConnectionQuality } from "@/lib/api-types";
import { QUALITY_LABEL } from "@/lib/net";

/** Three signal bars: good all green, fair two yellow, poor one red; grey when unknown. */
export function NetBars({ quality }: { quality: ConnectionQuality | null | undefined }) {
  const cls = quality ? ` net--${quality.toLowerCase()}` : "";
  return (
    <span className={`net${cls}`} role="img" aria-label={`Connection: ${quality ? QUALITY_LABEL[quality].toLowerCase() : "unknown"}`}>
      <i />
      <i />
      <i />
    </span>
  );
}
