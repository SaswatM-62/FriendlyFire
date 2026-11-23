import { NextResponse } from "next/server";
import { auth0 } from "@/lib/auth0";
import { config as appConfig } from "@/lib/config";

/**
 * Next.js 16's request proxy (formerly middleware). The Auth0 SDK handles its routes
 * here (/auth/login, /auth/logout, /auth/callback, /auth/profile, /auth/access-token)
 * and keeps the session cookie rolling. With the fake backend on, or Auth0 not set
 * up, requests pass straight through.
 */
export async function proxy(request: Request) {
  if (appConfig.fakeBackend || !auth0) return NextResponse.next();
  return auth0.middleware(request);
}

export const config = {
  matcher: ["/((?!_next/static|_next/image|favicon.ico|icon.svg|apple-icon.png|manifest.webmanifest|brand/|sitemap.xml|robots.txt).*)"],
};
