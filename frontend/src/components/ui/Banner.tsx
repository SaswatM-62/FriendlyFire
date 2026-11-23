import type { ReactNode } from "react";
import { Icon, type IconName } from "@/components/ui/Icon";

export type Tone = "mint" | "yellow" | "red" | "cyan" | "orange" | "violet" | "pink";

/** A full-width banner under the header (Messages board): a coloured edge and icon, text, maybe a button. */
export function Banner({
  tone,
  icon,
  children,
  action,
  role = "status",
}: {
  tone: Tone;
  icon: IconName;
  children: ReactNode;
  action?: ReactNode;
  /** "alert" for errors the player must see now. */
  role?: "status" | "alert";
}) {
  return (
    <div className="bn" role={role} style={{ borderColor: `var(--${tone}-text)` }}>
      <span style={{ color: `var(--${tone}-text)`, display: "flex" }}>
        <Icon name={icon} />
      </span>
      <span className="grow">{children}</span>
      {action}
    </div>
  );
}
