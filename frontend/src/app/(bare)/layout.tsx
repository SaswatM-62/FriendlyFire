import type { ReactNode } from "react";
import { Footer } from "@/components/chrome/Footer";

/** Pages without the header (sign-in). Riot's notice is still in the footer. */
export default function BareLayout({ children }: { children: ReactNode }) {
  return (
    <div className="c">
      <main className="auth" id="main">
        {children}
      </main>
      <Footer />
    </div>
  );
}
