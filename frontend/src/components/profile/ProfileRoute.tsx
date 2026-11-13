"use client";

import { SignedIn } from "@/components/common/SignedIn";
import { ProfileView } from "@/components/profile/ProfileView";

export function ProfileRoute() {
  return <SignedIn what="your profile">{(me) => <ProfileView me={me} />}</SignedIn>;
}
