import type { Metadata } from "next";
import Link from "next/link";
import "./globals.css";

export const metadata: Metadata = {
  title: "FriendlyFire",
  description: "Trivia built from your friends' own VALORANT matches.",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="en" className="h-full antialiased">
      <body className="flex min-h-full flex-col">
        <nav className="flex gap-4 border-b border-gray-200 px-8 py-3 text-sm dark:border-gray-800">
          <Link href="/" className="font-semibold">
            FriendlyFire
          </Link>
          <Link href="/login">Log in</Link>
          <Link href="/profile">Profile</Link>
          <Link href="/history">History</Link>
        </nav>
        {children}
      </body>
    </html>
  );
}
