import type { Metadata } from "next";
import Link from "next/link";
import { SiteChrome } from "@/components/chrome/SiteChrome";
import { MessageCard } from "@/components/ui/States";

export const metadata: Metadata = { title: "Page not found" };

export default function NotFound() {
  return (
    <SiteChrome>
      <MessageCard
        icon={
          <span className="num" style={{ fontSize: 56, lineHeight: 1, color: "var(--orange-text)" }} aria-hidden="true">
            404
          </span>
        }
        title="This page doesn't exist"
        actions={
          <Link className="btn btn--primary" href="/">
            Back to home
          </Link>
        }
      >
        <p className="muted">The link might be old, or the address has a typo.</p>
      </MessageCard>
    </SiteChrome>
  );
}
