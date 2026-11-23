"use client";

import { useEffect, useState } from "react";
import { useSyncStatus } from "@/components/providers/SyncStatusProvider";
import { Banner } from "@/components/ui/Banner";
import type { DataStatus } from "@/lib/api-types";
import { formatEstimate } from "@/lib/format";

const SHOW_DONE_MS = 5_000;

/**
 * "Syncing your matches · about 4 min" under the header on every page while a sync
 * runs (Architecture 2.3), "Matches synced." for a few seconds when it finishes, and
 * a retry when it failed.
 */
export function SyncBanner() {
  const { sync, refresh } = useSyncStatus();
  const status = sync?.status ?? null;
  const [previous, setPrevious] = useState<DataStatus | null>(status);
  const [justFinished, setJustFinished] = useState(false);

  if (status !== previous) {
    setPrevious(status);
    setJustFinished((previous === "SYNCING" || previous === "NOT_SYNCED") && status === "SYNCED");
  }

  useEffect(() => {
    if (!justFinished) return;
    const timer = setTimeout(() => setJustFinished(false), SHOW_DONE_MS);
    return () => clearTimeout(timer);
  }, [justFinished]);

  if (!sync) return null;
  if (status === "SYNCING" || status === "NOT_SYNCED") {
    return (
      <Banner tone="yellow" icon="clock">
        {status === "SYNCING" ? (
          <>
            <strong>Syncing your matches</strong> · about {formatEstimate(sync.estimatedSeconds).replace("~", "")}. You can still join rooms.
          </>
        ) : (
          "Your matches are over a day old. Refreshing them now."
        )}
      </Banner>
    );
  }
  if (status === "FAILED") {
    return (
      <Banner
        tone="red"
        icon="alert"
        action={
          <button type="button" className="btn btn--line btn--sm" onClick={refresh}>
            Retry
          </button>
        }
      >
        We couldn&apos;t sync your matches.{sync.errorMessage ? ` ${sync.errorMessage}` : ""}
      </Banner>
    );
  }
  if (justFinished) {
    return (
      <Banner tone="mint" icon="check">
        Matches synced.
      </Banner>
    );
  }
  return null;
}
