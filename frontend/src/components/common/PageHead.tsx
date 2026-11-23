import type { ReactNode } from "react";

/**
 * A page's heading: an optional line above (a label or a back link), the title, a muted
 * line under it, and actions on the right.
 */
export function PageHead({
  above,
  title,
  subtitle,
  actions,
}: {
  above?: ReactNode;
  title: ReactNode;
  subtitle?: ReactNode;
  actions?: ReactNode;
}) {
  return (
    <header className="spread" style={{ alignItems: "flex-end", flexWrap: "wrap" }}>
      <div>
        {above}
        <h1 className="title">{title}</h1>
        {subtitle && <p className="muted">{subtitle}</p>}
      </div>
      {actions}
    </header>
  );
}
