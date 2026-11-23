import type { CharacterId, ColourId, ExpressionId, PickableExpression } from "@/lib/api-types";

export { CHARACTER_SHAPES } from "@/lib/characterShapes";

/** The default order: player N in a room gets CHARACTER_ORDER[N-1] in colour N. */
export const CHARACTER_ORDER: CharacterId[] = ["blob", "ghost", "boxy", "onigiri", "star", "cloud", "shield", "flower", "gem", "drop"];

/** The colours, in token order: "pink" is --player-1, "silver" is --player-10. */
export const COLOUR_ORDER: ColourId[] = ["pink", "cyan", "mint", "orange", "violet", "yellow", "blue", "lime", "fuchsia", "silver"];

/** The faces a player can pick, in the Profile picker's order. */
export const PICKABLE_EXPRESSIONS: PickableExpression[] = ["happy", "smirk", "cocky", "locked-in", "fierce", "wink", "gloat", "shades"];

export const EXPRESSION_LABELS: Record<ExpressionId, string> = {
  happy: "Happy",
  smirk: "Smirk",
  cocky: "Cocky",
  "locked-in": "Locked in",
  fierce: "Fierce",
  wink: "Wink",
  gloat: "Gloat",
  shades: "Shades",
  wow: "Wow",
  thinking: "Thinking",
  away: "Away",
};

/**
 * A spread of faces for lineups and for players whose favourite face we don't know yet
 * (the boards: player 1 Cocky, player 2 Smirk, player 3 Locked in...).
 */
export const FACE_ORDER: PickableExpression[] = ["cocky", "smirk", "locked-in", "gloat", "wink", "fierce", "shades", "happy", "smirk", "gloat"];

/** 1-based index of a colour's --player-N token. */
export function colourIndex(colour: ColourId): number {
  return COLOUR_ORDER.indexOf(colour) + 1;
}

export function isCharacterId(value: unknown): value is CharacterId {
  return typeof value === "string" && (CHARACTER_ORDER as string[]).includes(value);
}

export function isColourId(value: unknown): value is ColourId {
  return typeof value === "string" && (COLOUR_ORDER as string[]).includes(value);
}

export function isPickableExpression(value: unknown): value is PickableExpression {
  return typeof value === "string" && (PICKABLE_EXPRESSIONS as string[]).includes(value);
}
