import { Suspense, type ReactNode } from "react";
import { Footer } from "@/components/chrome/Footer";
import { SyncBanner } from "@/components/chrome/SyncBanner";
import { StaticTopBar, TopBar } from "@/components/chrome/TopBar";

/**
 * Every page outside a room, and the room's own messages (closed, removed, joining):
 * the frame with its faint icon pattern, the header, the sync banner, the page, and
 * the footer with Riot's notice. On desktop it fills one window and the page scrolls
 * inside it if it has to; under 900px the whole page scrolls.
 */
export function SiteChrome({ children }: { children: ReactNode }) {
  return (
    <div className="c">
      <Suspense fallback={<StaticTopBar />}>
        <TopBar />
      </Suspense>
      <main className="main" id="main">
        <SyncBanner />
        {children}
      </main>
      <Footer />
    </div>
  );
}
