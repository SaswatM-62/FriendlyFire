"use client";

import { SignedIn } from "@/components/common/SignedIn";
import { HistoryList } from "@/components/history/HistoryList";

export function HistoryRoute() {
  return <SignedIn what="your quiz history">{(me) => <HistoryList me={me} />}</SignedIn>;
}
