"use client";

import { MuteToggle } from "@/components/chrome/Toggles";
import { Logo } from "@/components/ui/Brand";
import { Icon } from "@/components/ui/Icon";
import { NetBars } from "@/components/ui/NetBars";
import type { ConnectionQuality } from "@/lib/api-types";
import { QUALITY_LABEL } from "@/lib/net";

/**
 * The bar across the top of every room screen: the room code, where the room is, your
 * connection (FF-310's network bar), the chat button on phones, sound and Leave. The
 * logo isn't a link here, so nobody leaves a quiz by accident; Leave asks first.
 */
export function RoomBar({
  code,
  status,
  quality,
  unread,
  onChat,
  onLeave,
}: {
  code: string;
  status?: string;
  quality?: ConnectionQuality | null;
  /** Chat messages since the phone chat sheet was last open. */
  unread?: number;
  onChat?: () => void;
  onLeave?: () => void;
}) {
  return (
    <header className="bar bar--room">
      <div className="bar__side">
        <span className="logo">
          <Logo word={false} />
        </span>
        <span className="code">
          <span className="sr">Room code </span>
          {code}
        </span>
      </div>
      {status && <span className="chip">{status}</span>}
      <div className="bar__side">
        {quality !== undefined && (
          <span className="chip netchip">
            <NetBars quality={quality} />
            <span className="netlab">{quality ? QUALITY_LABEL[quality] : "…"}</span>
          </span>
        )}
        {onChat && (
          <button
            type="button"
            className="btn btn--icon chatbtn"
            aria-label={`Room chat${unread ? `, ${unread} unread` : ""}`}
            onClick={onChat}
          >
            <Icon name="chat" />
            {unread ? <span className="dot">{unread > 9 ? "9+" : unread}</span> : null}
          </button>
        )}
        <MuteToggle />
        {onLeave && (
          <button type="button" className="btn btn--icon" aria-label="Leave room" onClick={onLeave}>
            <Icon name="signOut" />
          </button>
        )}
      </div>
    </header>
  );
}
