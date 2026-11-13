"use client";

import Link from "next/link";
import type { ReactNode } from "react";
import { SiteChrome } from "@/components/chrome/SiteChrome";
import type { Tone } from "@/components/ui/Banner";
import type { IconName } from "@/components/ui/Icon";
import { MessageCard } from "@/components/ui/States";

/** Loading the room (Joining a room board, "loading"). */
export function JoiningSkeleton({ code }: { code: string }) {
  return (
    <SiteChrome>
      <MessageCard icon="spinner" title={`Joining room ${code}…`} role="status">
        <p className="muted">Loading the room.</p>
      </MessageCard>
    </SiteChrome>
  );
}

/**
 * A room screen that's just a message, in the site's frame (Joining a room, Room closed
 * and Open somewhere else boards): not found, full, removed, closed... Back to home
 * comes last, after any other actions.
 */
export function RoomMessage({
  icon,
  tone,
  title,
  children,
  actions,
  wide,
}: {
  icon: IconName;
  tone: Tone | "muted";
  title: string;
  children: ReactNode;
  actions?: ReactNode;
  wide?: boolean;
}) {
  return (
    <SiteChrome>
      <MessageCard
        icon={icon}
        tone={tone}
        title={title}
        role="alert"
        wide={wide}
        actions={
          <>
            {actions}
            <Link className={`btn ${actions ? "btn--ghost" : "btn--primary"}`} href="/">
              Back to home
            </Link>
          </>
        }
      >
        {children}
      </MessageCard>
    </SiteChrome>
  );
}
