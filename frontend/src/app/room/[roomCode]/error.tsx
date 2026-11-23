"use client";

import Link from "next/link";
import { SiteChrome } from "@/components/chrome/SiteChrome";
import { MessageCard } from "@/components/ui/States";

/** Anything the room didn't handle itself (Not found / error board, "error"). */
export default function RoomError({ reset }: { error: Error; reset: () => void }) {
  return (
    <SiteChrome>
      <MessageCard
        icon="alert"
        tone="red"
        role="alert"
        title="Something went wrong on our side"
        actions={
          <>
            <button type="button" className="btn btn--primary" onClick={reset}>
              Retry
            </button>
            <Link className="btn btn--ghost" href="/">
              Back to home
            </Link>
          </>
        }
      >
        <p className="muted">It&apos;s not you. Try again in a moment.</p>
      </MessageCard>
    </SiteChrome>
  );
}
