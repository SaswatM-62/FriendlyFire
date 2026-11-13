"use client";

import type { CSSProperties, KeyboardEvent, ReactNode } from "react";
import { announceLookChange, useFavouriteLook } from "@/components/chrome/useFavouriteLook";
import { Av, Character } from "@/components/ui/Character";
import { CHARACTER_ORDER, CHARACTER_SHAPES, COLOUR_ORDER, EXPRESSION_LABELS, PICKABLE_EXPRESSIONS, colourIndex } from "@/lib/characters";
import { saveFavouriteLook, type FavouriteLook } from "@/lib/preferences";

/** Arrow keys move through a radio group, as in a native one. */
function onArrows<T>(event: KeyboardEvent, items: T[], current: T, choose: (item: T) => void) {
  const step = event.key === "ArrowRight" || event.key === "ArrowDown" ? 1 : event.key === "ArrowLeft" || event.key === "ArrowUp" ? -1 : 0;
  if (step === 0) return;
  event.preventDefault();
  const next = items[(items.indexOf(current) + step + items.length) % items.length];
  choose(next);
  const group = event.currentTarget.parentElement;
  (group?.children[items.indexOf(next)] as HTMLElement | undefined)?.focus();
}

/** One radio group of the picker (icon, colour or expression). */
function PickerGroup<T extends string>({
  id,
  title,
  items,
  current,
  className,
  render,
  onChoose,
}: {
  id: string;
  title: string;
  items: T[];
  current: T;
  className: string;
  render: (item: T) => { className: string; style?: CSSProperties; label?: string; content?: ReactNode };
  onChoose: (item: T) => void;
}) {
  return (
    <div className="pick">
      <p className="label" id={id}>
        {title}
      </p>
      <div className={`pick ${className}`} role="radiogroup" aria-labelledby={id}>
        {items.map((item) => {
          const checked = item === current;
          const view = render(item);
          return (
            <button
              key={item}
              type="button"
              className={view.className}
              role="radio"
              aria-checked={checked}
              aria-label={view.label}
              tabIndex={checked ? 0 : -1}
              style={view.style}
              onClick={() => onChoose(item)}
              onKeyDown={(e) => onArrows(e, items, current, onChoose)}
            >
              {view.content}
            </button>
          );
        })}
      </div>
    </div>
  );
}

const capitalise = (word: string) => word[0].toUpperCase() + word.slice(1);

/** "Your look": a favourite icon, colour and expression (Profile board). */
export function LookPicker() {
  const look = useFavouriteLook();

  const choose = (change: Partial<FavouriteLook>) => {
    saveFavouriteLook({ ...look, ...change });
    announceLookChange();
  };

  return (
    <section className="card" aria-labelledby="h-look">
      <div className="spread">
        <h2 id="h-look" className="label">
          Your look
        </h2>
        <span className="small faint">
          {CHARACTER_SHAPES[look.icon].label} · {look.colour} · {EXPRESSION_LABELS[look.expression]}
        </span>
      </div>
      <div className="row" style={{ gap: 16 }}>
        <Av look={look} size={64} />
        <p className="small muted">
          Shows <strong>Wow</strong> when you&apos;re the answer and <strong>Away</strong> if you drop.
        </p>
      </div>
      <PickerGroup
        id="l-icon"
        title="Icon"
        className="icons"
        items={CHARACTER_ORDER}
        current={look.icon}
        onChoose={(icon) => choose({ icon })}
        render={(icon) => ({
          className: "ib",
          label: CHARACTER_SHAPES[icon].label,
          content: <Character look={{ ...look, icon }} size={34} />,
        })}
      />
      <PickerGroup
        id="l-col"
        title="Colour"
        className="cols"
        items={COLOUR_ORDER}
        current={look.colour}
        onChoose={(colour) => choose({ colour })}
        render={(colour) => ({
          className: "sw",
          label: capitalise(colour),
          style: { background: `var(--player-${colourIndex(colour)}-fill)` },
        })}
      />
      <PickerGroup
        id="l-expr"
        title="Expression"
        className="exprs"
        items={PICKABLE_EXPRESSIONS}
        current={look.expression}
        onChoose={(expression) => choose({ expression })}
        render={(expression) => ({ className: "eb", content: EXPRESSION_LABELS[expression] })}
      />
      <p className="small faint">
        If someone in the room already has your icon or colour, whoever joined first keeps it and you get a random free one for that game.
        Saved in this browser for now: rooms use your icon and colour once FriendlyFire saves looks on your account.
      </p>
    </section>
  );
}
