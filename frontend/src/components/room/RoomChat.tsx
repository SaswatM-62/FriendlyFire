"use client";

import { useEffect, useId, useRef, useState, useSyncExternalStore, type FormEvent, type KeyboardEvent } from "react";
import { usePlayerInfo, useRoom } from "@/components/room/RoomContext";
import { Character } from "@/components/ui/Character";
import { Icon } from "@/components/ui/Icon";
import type { ChatLine } from "@/lib/chat";
import { colourIndex } from "@/lib/characters";

const PHONE = "(max-width: 600px)";

function subscribePhone(listener: () => void) {
  const query = window.matchMedia?.(PHONE);
  query?.addEventListener("change", listener);
  return () => query?.removeEventListener("change", listener);
}

/** True on phone-width screens, where room chat is a bottom sheet. */
export function usePhone(): boolean {
  return useSyncExternalStore(subscribePhone, () => window.matchMedia?.(PHONE).matches ?? false, () => false);
}

/**
 * Room chat (Calmer revamp): the right-hand column on every room screen, a bottom sheet
 * on phones. The log shows what everyone said and what happened; new lines scroll into
 * view unless you've scrolled up to read.
 */
export function ChatPanel({ lines, onSend, onClose }: { lines: ChatLine[]; onSend: (text: string) => void; onClose: () => void }) {
  const { meId } = useRoom();
  const info = usePlayerInfo();
  const inputId = useId();
  const log = useRef<HTMLOListElement>(null);
  const [text, setText] = useState("");
  const last = lines[lines.length - 1];

  useEffect(() => {
    const el = log.current;
    if (!el || !last) return;
    const nearBottom = el.scrollHeight - el.scrollTop - el.clientHeight < 120;
    if (nearBottom || (last.kind === "msg" && last.userId === meId)) el.scrollTop = el.scrollHeight;
  }, [last, meId]);

  const send = (event: FormEvent) => {
    event.preventDefault();
    const trimmed = text.trim();
    if (!trimmed) return;
    onSend(trimmed);
    setText("");
  };

  const onKeyDown = (event: KeyboardEvent) => {
    if (event.key === "Escape") onClose();
  };

  return (
    <section className="card chat" aria-labelledby="h-chat" onKeyDown={onKeyDown}>
      <div className="spread">
        <h2 id="h-chat" className="label">
          Room chat
        </h2>
        <button type="button" className="btn btn--icon chat__close" style={{ width: 32, height: 32 }} aria-label="Close chat" onClick={onClose}>
          <Icon name="close" size={16} />
        </button>
      </div>
      {/* Focusable, so keyboard users can scroll back through it. */}
      <ol ref={log} className="chat__log" aria-live="polite" aria-label="Chat messages" tabIndex={0}>
        {lines.length === 0 && <li className="msg msg--sys">Say hi to your squad</li>}
        {lines.map((line) => {
          if (line.kind === "sys") {
            return (
              <li key={line.id} className="msg msg--sys">
                {line.text}
              </li>
            );
          }
          const p = info(line.userId);
          const you = line.userId === meId;
          return (
            <li key={line.id} className={`msg${you ? " msg--you" : ""}`}>
              <span className="mini">
                <Character look={p.look} size={18} />
              </span>
              <p>
                <b style={{ color: `var(--player-${colourIndex(p.look.colour)})` }}>{p.name}</b>
                <span className="muted">{line.text}</span>
              </p>
            </li>
          );
        })}
      </ol>
      <form className="chat__form" onSubmit={send}>
        <label className="sr" htmlFor={inputId}>
          Message
        </label>
        <input
          id={inputId}
          className="input"
          placeholder="Say something"
          maxLength={200}
          autoComplete="off"
          value={text}
          onChange={(e) => setText(e.target.value)}
        />
        <button type="submit" className="btn btn--icon" aria-label="Send" disabled={!text.trim()}>
          <Icon name="send" />
        </button>
      </form>
    </section>
  );
}
