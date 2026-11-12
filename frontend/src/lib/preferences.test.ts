import { describe, expect, it } from "vitest";
import { DEFAULT_LOOK, favouriteLook, saveFavouriteLook } from "@/lib/preferences";

describe("favourite look", () => {
  it("defaults to the pink Blob, Happy", () => {
    expect(favouriteLook()).toEqual({ icon: "blob", colour: "pink", expression: "happy" });
  });

  it("remembers icon, colour and expression", () => {
    saveFavouriteLook({ icon: "shield", colour: "blue", expression: "shades" });

    expect(favouriteLook()).toEqual({ icon: "shield", colour: "blue", expression: "shades" });
  });

  it("reads a look saved before expressions existed as Happy", () => {
    window.localStorage.setItem("ff-look", JSON.stringify({ icon: "ghost", colour: "cyan" }));

    expect(favouriteLook()).toEqual({ icon: "ghost", colour: "cyan", expression: "happy" });
  });

  it("falls back to the default for a character that no longer exists (the Bean)", () => {
    window.localStorage.setItem("ff-look", JSON.stringify({ icon: "bean", colour: "blue", expression: "wink" }));

    expect(favouriteLook()).toEqual(DEFAULT_LOOK);
  });
});
