"use client";

import { useMe } from "@/components/providers/MeProvider";
import { isCharacterId, isColourId, isPickableExpression } from "@/lib/characters";
import { DEFAULT_LOOK, type FavouriteLook } from "@/lib/preferences";

/**
 * Your favourite character, colour and face (Profile "Your look"), as your account keeps
 * it (FF-519): the pink Blob until you pick one, or while signed out.
 */
export function useFavouriteLook(): FavouriteLook {
  const look = useMe().me?.look;
  if (!look || !isCharacterId(look.icon) || !isColourId(look.colour)) return DEFAULT_LOOK;
  return { icon: look.icon, colour: look.colour, expression: isPickableExpression(look.expression) ? look.expression : "happy" };
}
