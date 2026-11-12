import Link from "next/link";
import { useId, type CSSProperties } from "react";

/**
 * The crosshair app icon: dark (logo/ff-crosshair.svg) for dark backgrounds and the app
 * icon, light (ff-crosshair-light.svg: white square, lavender edge, deeper green and orange).
 */
export function CrosshairMark({ size, variant = "dark", className }: { size: number; variant?: "dark" | "light"; className?: string }) {
  const gradient = useId();
  const light = variant === "light";
  return (
    <svg viewBox="0 0 512 512" width={size} height={size} aria-hidden="true" className={className}>
      <defs>
        <linearGradient id={gradient} x1="0" y1="193" x2="0" y2="327" gradientUnits="userSpaceOnUse">
          <stop offset="0" stopColor={light ? "#E2500C" : "#FF6A26"} />
          <stop offset="1" stopColor={light ? "#EF8A14" : "#FFB13A"} />
        </linearGradient>
      </defs>
      {light ? (
        <rect x="4" y="4" width="504" height="504" rx="108" fill="#FFFFFF" stroke="#E2DAF7" strokeWidth="8" />
      ) : (
        <rect width="512" height="512" rx="112" fill="#16121F" />
      )}
      <g fill="none" stroke={light ? "#00A352" : "#3DFF8A"} strokeWidth="22" strokeLinecap="round">
        <circle cx="256" cy="256" r="166" />
        <path d="M256 44V120M256 392V468M44 256H120M392 256H468" />
      </g>
      <path
        d="M155 193H247V224H187V247H235V276H187V327H155Z M280 193H372V224H312V247H360V276H312V327H280Z"
        fill={`url(#${gradient})`}
      />
    </svg>
  );
}

/**
 * The logo's insides (the boards' .logo): the crosshair, dark or light by theme, then
 * "Friendly" in the text colour and "Fire" in orange. Room bars show the mark alone.
 */
export function Logo({ word = true }: { word?: boolean }) {
  return (
    <>
      <CrosshairMark size={32} className="dk" />
      <CrosshairMark size={32} variant="light" className="lt" />
      {word && (
        <b>
          Friendly<span>Fire</span>
        </b>
      )}
    </>
  );
}

/** The logo as the link home, in every header. */
export function LogoLink({ word = true }: { word?: boolean }) {
  return (
    <Link className="logo" href="/" aria-label="FriendlyFire home">
      <Logo word={word} />
    </Link>
  );
}

const TICK_ANGLES = Array.from({ length: 72 }, (_, i) => i * 5);
const BIG_TICKS = new Set([20, 110, 200, 290]);

function TickRing() {
  return (
    <svg viewBox="-505 -505 1010 1010" aria-hidden="true">
      {TICK_ANGLES.map((deg) => {
        const big = BIG_TICKS.has(deg);
        const a = (deg * Math.PI) / 180;
        const inner = big ? 418 : 428;
        return (
          <line
            key={deg}
            x1={(inner * Math.cos(a)).toFixed(1)}
            y1={(inner * Math.sin(a)).toFixed(1)}
            x2={(440 * Math.cos(a)).toFixed(1)}
            y2={(440 * Math.sin(a)).toFixed(1)}
            stroke={big ? "var(--tick)" : "var(--ring)"}
            strokeWidth={big ? 4 : 2}
            strokeLinecap="round"
          />
        );
      })}
    </svg>
  );
}

function Rings() {
  return (
    <svg viewBox="-505 -505 1010 1010" aria-hidden="true">
      {[140, 250, 370, 490].map((r) => (
        <circle key={r} r={r} fill="none" stroke="var(--ring)" strokeWidth="1.5" />
      ))}
      <circle r="310" fill="none" stroke="var(--ring)" strokeWidth="1.5" strokeDasharray="2 12" />
      <g transform="rotate(45)">
        <line x1="-490" y1="0" x2="-120" y2="0" stroke="var(--hair)" strokeWidth="1.5" />
        <line x1="120" y1="0" x2="490" y2="0" stroke="var(--hair)" strokeWidth="1.5" />
        <line x1="0" y1="-490" x2="0" y2="-120" stroke="var(--hair)" strokeWidth="1.5" />
        <line x1="0" y1="120" x2="0" y2="490" stroke="var(--hair)" strokeWidth="1.5" />
        <line x1="0" y1="-500" x2="0" y2="-462" stroke="var(--tick)" strokeWidth="7" strokeLinecap="round" />
        <line x1="0" y1="462" x2="0" y2="500" stroke="var(--tick)" strokeWidth="7" strokeLinecap="round" />
        <line x1="-500" y1="0" x2="-462" y2="0" stroke="var(--tick)" strokeWidth="7" strokeLinecap="round" />
        <line x1="462" y1="0" x2="500" y2="0" stroke="var(--tick)" strokeWidth="7" strokeLinecap="round" />
      </g>
    </svg>
  );
}

/**
 * The logo's crosshair ring, large and faint behind a screen. Its animation follows
 * the nearest data-motion: "lobby" spins, "last5" sweeps red, "reveal" hits.
 */
export function Scope({ variant, style }: { variant?: "corner" | "hero"; style?: CSSProperties }) {
  const cls = variant ? ` nf-scope--${variant}` : "";
  return (
    <div className={`nf-scope${cls}`} style={style} aria-hidden="true">
      <div className="nf-scope__layer">
        <Rings />
      </div>
      <div className="nf-scope__layer nf-scope__spin">
        <TickRing />
      </div>
      <div className="nf-scope__layer nf-scope__sweep" />
      <div className="nf-scope__layer nf-scope__hit" />
    </div>
  );
}
