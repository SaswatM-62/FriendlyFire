import type { ReactNode } from "react";
import { Scope } from "@/components/ui/Brand";

/**
 * The quiz card in the main area (the boards' .stagecard), with the scope rings behind:
 * questions, countdowns, votes. `center` centres it (countdowns, votes, pauses).
 */
export function StageCard({ center, vote, className = "", children }: { center?: boolean; vote?: boolean; className?: string; children: ReactNode }) {
  const cls = ["card stagecard scoped nf-phase", center && "stagecard--center", vote && "stagecard--vote", className].filter(Boolean).join(" ");
  return (
    <div className={cls}>
      <Scope />
      {children}
    </div>
  );
}
