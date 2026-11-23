import Link from "next/link";

/** Riot's required notice (FF-610). It must be on every page. */
export const RIOT_NOTICE =
  "FriendlyFire isn't endorsed by Riot Games and doesn't reflect the views or opinions of Riot Games or anyone officially involved in producing or managing Riot Games properties. Riot Games and all associated properties are trademarks or registered trademarks of Riot Games, Inc.";

export function Footer() {
  return (
    <footer className="foot">
      <nav aria-label="Site">
        <Link href="/how-it-works">How it works</Link>
        <Link href="/terms">Terms</Link>
        <Link href="/privacy">Privacy</Link>
      </nav>
      <p>{RIOT_NOTICE}</p>
    </footer>
  );
}
