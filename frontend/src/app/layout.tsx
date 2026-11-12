import type { Metadata, Viewport } from "next";
import type { ReactNode } from "react";
import { AppProviders } from "@/components/providers/AppProviders";
import { THEME_SCRIPT } from "@/lib/preferences";
import { fontVariables } from "./fonts";
import "./globals.css";

const description = "Trivia built from your friends' own VALORANT matches. Every question is about someone in the room.";

export const metadata: Metadata = {
  metadataBase: new URL(process.env.APP_BASE_URL ?? "http://localhost:3000"),
  title: { default: "FriendlyFire", template: "%s · FriendlyFire" },
  description,
  applicationName: "FriendlyFire",
  openGraph: {
    siteName: "FriendlyFire",
    type: "website",
    title: "FriendlyFire: how well do you know your squad?",
    description,
  },
  twitter: { card: "summary_large_image", title: "FriendlyFire", description },
};

export const viewport: Viewport = {
  themeColor: "#0B0812",
  colorScheme: "dark light",
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="en" data-theme="dark" className={fontVariables} suppressHydrationWarning>
      <head>
        <script dangerouslySetInnerHTML={{ __html: THEME_SCRIPT }} />
      </head>
      <body>
        <a href="#main" className="skip">
          Skip to content
        </a>
        <AppProviders>{children}</AppProviders>
      </body>
    </html>
  );
}
