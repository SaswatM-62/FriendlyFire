import type { Metadata } from "next";
import { Suspense } from "react";
import { SignInCard } from "@/components/common/SignInCard";
import { isAuth0Configured } from "@/lib/auth0";
import { config } from "@/lib/config";

export const metadata: Metadata = { title: "Sign in" };

export default function LoginPage() {
  return (
    <Suspense fallback={null}>
      <SignInCard available={config.fakeBackend || isAuth0Configured()} />
    </Suspense>
  );
}
