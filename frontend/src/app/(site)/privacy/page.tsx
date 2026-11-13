import type { Metadata } from "next";
import Link from "next/link";
import { LegalPage, type LegalSection } from "@/components/common/LegalPage";

export const metadata: Metadata = { title: "Privacy Policy" };

const SECTIONS: LegalSection[] = [
  {
    id: "p1",
    title: "1. What we collect",
    body: (
      <ul>
        <li>
          <strong>Your sign-in:</strong> your email address and name from Google or your email login, handled by Auth0.
        </li>
        <li>
          <strong>Your display name</strong> and, if you choose, your favourite character and colour.
        </li>
        <li>
          <strong>If you link Riot:</strong> your Riot ID and region, and the Competitive and Unrated matches you played in the last 30
          days.
        </li>
        <li>
          <strong>Quiz results:</strong> your scores in the quizzes you play, so your history works.
        </li>
      </ul>
    ),
  },
  {
    id: "p2",
    title: "2. Linking is opt-in",
    body: (
      <p>
        We only read your matches after you link your Riot account through Riot Sign-On and approve it there. We don&apos;t keep your Riot
        password or sign-in token: we use the sign-on once to learn which account is yours, then throw the token away.
      </p>
    ),
  },
  {
    id: "p3",
    title: "3. Other players in your matches",
    body: (
      <p>
        To compare players (for example, who topped the scoreboard in a match), we store the stats of everyone in a linked player&apos;s
        matches. We only ever show or ask questions about players who linked their own account. Nobody else&apos;s name or stats appear in
        FriendlyFire.
      </p>
    ),
  },
  {
    id: "p4",
    title: "4. Keeping it fresh",
    body: (
      <p>
        Your match data counts as fresh for 24 hours. When it&apos;s older, we fetch your last 30 days again and replace what we had, for
        example when you sign in or join a room.
      </p>
    ),
  },
  {
    id: "p5",
    title: "5. Who sees what",
    body: <p>Only people in the same room as you see questions about you. Your history shows the quizzes you played and their final scores.</p>,
  },
  {
    id: "p6",
    title: "6. Unlinking and deleting",
    body: (
      <>
        <p>
          <strong>Unlink</strong> your Riot account on your <Link href="/profile">profile</Link> and we delete the matches we synced for you.
        </p>
        <p>
          <strong>Delete your account</strong> and we remove all our data about you. In other players&apos; past quizzes you show as
          &quot;Deleted player&quot;. Your Auth0 sign-in itself stays with Auth0: if you sign in again, you start a brand-new account.
        </p>
      </>
    ),
  },
];

export default function PrivacyPage() {
  return (
    <>
      <LegalPage title="Privacy Policy" updated="1 November 2026" sections={SECTIONS} draft />
    </>
  );
}
