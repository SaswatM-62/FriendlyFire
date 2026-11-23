import type { CSSProperties } from "react";
import type { ExpressionId, PlayerLook } from "@/lib/api-types";
import { CHARACTER_SHAPES, colourIndex } from "@/lib/characters";
import type { Shape } from "@/lib/characterShapes";

/** The CSS variables that colour a player: --pc (lines/text) and --pf (fills/glow). */
export function playerColourVars(look: Pick<PlayerLook, "colour">): CSSProperties {
  const n = colourIndex(look.colour);
  return { "--pc": `var(--player-${n})`, "--pf": `var(--player-${n}-fill)` } as CSSProperties;
}

/** One exported shape as SVG, with "ink" replaced by the player's colour. */
function ShapeSvg({ shape, ink }: { shape: Shape; ink: string }) {
  const { tag, ...attributes } = shape;
  const props: Record<string, string | number> = {};
  for (const [key, value] of Object.entries(attributes)) props[key] = value === "ink" ? ink : value;
  if (tag === "path") return <path {...props} />;
  if (tag === "ellipse") return <ellipse {...props} />;
  return <rect {...props} />;
}

interface CharacterProps {
  look: PlayerLook;
  size: number;
  /**
   * A face the game sets for a moment, over the player's own: "wow" when they're the
   * answer, "thinking" while they haven't answered, "away" when disconnected.
   */
  face?: ExpressionId;
  /** Greyed out: disconnected, or hasn't answered yet. */
  away?: boolean;
  /** Read out by screen readers; leave empty when the name is next to it. */
  label?: string;
}

/** One of the ten characters, in one of ten colours, with one of eleven faces. */
export function Character({ look, size, face, away, label }: CharacterProps) {
  const shapes = CHARACTER_SHAPES[look.icon];
  const n = colourIndex(look.colour);
  const expression = face ?? look.expression ?? "happy";
  // --char-ink lets a filled container (a locked answer tile) draw the character dark.
  const ink = `var(--char-ink, var(--player-${n}))`;
  return (
    <span
      className={`nf-char${away ? " is-away" : ""}`}
      data-face={expression}
      style={{ width: size, height: size, "--pf": `var(--player-${n}-fill)` } as CSSProperties}
      {...(label ? { role: "img", "aria-label": label } : {})}
    >
      <svg viewBox="-2 -2 68 68" width={size} height={size} aria-hidden="true">
        <g fill={`var(--char-ink, var(--player-${n}-fill))`} fillOpacity={0.14} stroke={ink} strokeWidth={3.2} strokeLinejoin="round">
          <ShapeSvg shape={shapes.body} ink={ink} />
        </g>
        {shapes.faces[expression].map((shape, i) => (
          <ShapeSvg key={i} shape={shape} ink={ink} />
        ))}
      </svg>
    </span>
  );
}

/** A character on its own (the boards' .av): tiles, lists, scoreboards. `away` fades it. */
export function Av({ look, size, face, away, label, className }: CharacterProps & { className?: string }) {
  return (
    <span className={`av${className ? ` ${className}` : ""}`}>
      <Character look={look} size={size} face={face} away={away} label={label} />
    </span>
  );
}

/** A small character in a round badge (the boards' .mini): chat, pickers, who played. */
export function Mini({ look, face, label }: Pick<CharacterProps, "look" | "face" | "label">) {
  return (
    <span className="mini">
      <Character look={look} size={18} face={face} label={label} />
    </span>
  );
}
