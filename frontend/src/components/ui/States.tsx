import type { CSSProperties, ReactNode } from "react";
import { Icon, type IconName } from "@/components/ui/Icon";
import type { Tone } from "@/components/ui/Banner";

/** A grey placeholder block while something loads. */
export function Skeleton({ width, height, round, className }: { width: number | string; height: number; round?: boolean; className?: string }) {
  return (
    <span
      className={`skel${className ? ` ${className}` : ""}`}
      style={{ width, maxWidth: "100%", height, borderRadius: round ? "50%" : undefined }}
    />
  );
}

/** A page-shaped skeleton: a title and a few cards. */
export function PageSkeleton({ label = "Loading…" }: { label?: string }) {
  return (
    <div className="flex flex-col gap-6" aria-busy="true">
      <Skeleton width={220} height={38} />
      <Skeleton width="100%" height={140} />
      <Skeleton width="100%" height={110} />
      <p className="sr" role="status">
        {label}
      </p>
    </div>
  );
}

/** The cyan spinner (the boards' .spin); 56 px in message cards. */
export function Spinner({ size = 56, label }: { size?: number; label?: string }) {
  const border = size < 32 ? 3 : 4;
  const style: CSSProperties = { width: size, height: size, borderWidth: border };
  return label ? <span className="spin" role="img" aria-label={label} style={style} /> : <span className="spin" aria-hidden="true" style={style} />;
}

/**
 * A message on its own in the middle of the page (the boards' message cards): an icon
 * or spinner, a title, a line or two, and what to do next. Errors, empty states and
 * the room's "closed", "removed" and "joining" screens all use it.
 */
export function MessageCard({
  icon,
  tone = "cyan",
  title,
  children,
  actions,
  role,
  wide,
}: {
  /** An icon, "spinner", or anything else for the top (the 404's big number). */
  icon: IconName | "spinner" | ReactNode;
  tone?: Tone | "muted";
  title: string;
  children?: ReactNode;
  actions?: ReactNode;
  role?: "alert" | "status";
  /** Room for a standings list. */
  wide?: boolean;
}) {
  let top: ReactNode;
  if (icon === "spinner") top = <Spinner label="Loading" />;
  else if (typeof icon === "string")
    top = (
      <span className="mico" style={{ color: tone === "muted" ? "var(--text-muted)" : `var(--${tone}-text)` }}>
        <Icon name={icon as IconName} size={26} />
      </span>
    );
  else top = icon;
  return (
    <div className="mwrap">
      <section className="card mcard nf-phase" aria-labelledby="mt" role={role} style={wide ? { width: "min(600px, 100%)" } : undefined}>
        {top}
        <h1 id="mt" className="title">
          {title}
        </h1>
        {children}
        {actions && <div className="btns">{actions}</div>}
      </section>
    </div>
  );
}

/** An error inside a card, with a retry button. */
export function InlineError({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <div className="row" role="alert" style={{ flexWrap: "wrap" }}>
      <span className="row small" style={{ gap: 6, color: "var(--red-text)" }}>
        <Icon name="alert" size={16} />
        {message}
      </span>
      {onRetry && (
        <button type="button" className="btn btn--line btn--sm" onClick={onRetry}>
          <Icon name="refresh" size={14} />
          Try again
        </button>
      )}
    </div>
  );
}
