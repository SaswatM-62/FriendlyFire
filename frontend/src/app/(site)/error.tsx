"use client";

import Link from "next/link";
import { MessageCard } from "@/components/ui/States";

/** Anything a page didn't handle itself (Not found / error board, "error"). */
export default function SiteError({ reset }: { error: Error; reset: () => void }) {
  return (
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
  );
}
