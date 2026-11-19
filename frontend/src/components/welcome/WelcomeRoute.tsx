"use client";

import { SignedIn } from "@/components/common/SignedIn";
import { Welcome } from "@/components/welcome/Welcome";

export function WelcomeRoute() {
  return <SignedIn what="the tutorial">{(me) => <Welcome me={me} />}</SignedIn>;
}
