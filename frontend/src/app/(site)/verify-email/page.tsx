import type { Metadata } from "next";
import { VerifyEmail } from "@/components/common/GatePages";

export const metadata: Metadata = { title: "Check your inbox", robots: { index: false } };

export default function VerifyEmailPage() {
  return (
    <>
      <VerifyEmail />
    </>
  );
}
