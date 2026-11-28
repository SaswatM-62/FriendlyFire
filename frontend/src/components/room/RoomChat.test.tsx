import { afterEach, beforeEach, describe, expect, it } from "vitest";
import { screen } from "@testing-library/react";
import { ChatPanel } from "@/components/room/RoomChat";
import type { ChatLine } from "@/lib/chat";
import { lobbySnapshot, renderRoom } from "@/test/room";

// jsdom has no layout: give the log a height and keep what it's scrolled to.
const sizes = ["scrollHeight", "clientHeight", "scrollTop"] as const;
const saved = sizes.map((key) => [key, Object.getOwnPropertyDescriptor(HTMLElement.prototype, key)] as const);

describe("ChatPanel", () => {
  beforeEach(() => {
    Object.defineProperty(HTMLElement.prototype, "scrollHeight", { configurable: true, get: () => 1_000 });
    Object.defineProperty(HTMLElement.prototype, "clientHeight", { configurable: true, get: () => 300 });
    Object.defineProperty(HTMLElement.prototype, "scrollTop", {
      configurable: true,
      get(this: HTMLElement & { top?: number }) {
        return this.top ?? 0;
      },
      set(this: HTMLElement & { top?: number }, value: number) {
        this.top = value;
      },
    });
  });
  afterEach(() => {
    for (const [key, descriptor] of saved) {
      if (descriptor) Object.defineProperty(HTMLElement.prototype, key, descriptor);
      else delete (HTMLElement.prototype as unknown as Record<string, unknown>)[key];
    }
  });

  it("opens at the newest line, so a new screen's chat isn't stuck at the top", () => {
    const lines: ChatLine[] = Array.from({ length: 30 }, (_, i) => ({ id: `c${i}`, kind: "sys", text: `Question ${i + 1} started` }));
    renderRoom(<ChatPanel lines={lines} onSend={() => {}} onClose={() => {}} />, lobbySnapshot());

    expect(screen.getByRole("list", { name: "Chat messages" }).scrollTop).toBe(1_000);
  });
});
