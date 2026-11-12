import type { Metadata } from "next";
import Link from "next/link";
import { LegalPage, type LegalSection } from "@/components/common/LegalPage";

export const metadata: Metadata = { title: "Terms of Service" };

const SECTIONS: LegalSection[] = [
  {
    id: "t1",
    title: "1. Who we are",
    body: <p>FriendlyFire is a free party game made by a team of students. It isn&apos;t made by or endorsed by Riot Games.</p>,
  },
  {
    id: "t2",
    title: "2. Your account",
    body: (
      <p>
        You sign in with Google or an email address. Keep your login to yourself. You can delete your account at any time from your{" "}
        <Link href="/profile">profile</Link>.
      </p>
    ),
  },
  {
    id: "t3",
    title: "3. Linking your Riot account",
    body: (
      <p>
        When you link your Riot account through Riot Sign-On, we read your Competitive and Unrated matches from the last 30 days to write
        questions. Only people in your room see questions about you. The <Link href="/privacy">Privacy Policy</Link> explains what we keep
        and for how long.
      </p>
    ),
  },
  {
    id: "t4",
    title: "4. Playing fair",
    body: <p>Be kind to your squad. Don&apos;t use FriendlyFire to harass anyone, and don&apos;t try to break or overload the service.</p>,
  },
  {
    id: "t5",
    title: "5. Changes",
    body: <p>We may update these terms. If we make a big change, we&apos;ll tell you in the app before it applies.</p>,
  },
];

export default function TermsPage() {
  return (
    <>
      <LegalPage title="Terms of Service" updated="1 November 2026" sections={SECTIONS} draft />
    </>
  );
}
