import { Auth0Client } from "@auth0/nextjs-auth0/server";
import { NextResponse } from "next/server";

/**
 * Server-side sign-in (FF-102). The settings come from .env (AUTH0_*, APP_BASE_URL;
 * see .env.example and FF-003). Without them sign-in is switched off and every page
 * still renders, signed out.
 */
export function isAuth0Configured(): boolean {
  const env = process.env;
  return Boolean(env.AUTH0_DOMAIN && env.AUTH0_CLIENT_ID && env.AUTH0_CLIENT_SECRET && env.AUTH0_SECRET && env.APP_BASE_URL);
}

export const auth0: Auth0Client | null = isAuth0Configured()
  ? new Auth0Client({
      authorizationParameters: {
        // The access token is for the Gateway (FF-103 checks this audience).
        audience: process.env.AUTH0_AUDIENCE,
        scope: "openid profile email offline_access",
      },
      // Sign-in always lands on the home page: no return URLs, so sign-in can't be used
      // to send players to another site. A pending invite is offered there instead.
      signInReturnToPath: "/",
      async onCallback(error, context) {
        const base = context.appBaseUrl ?? process.env.APP_BASE_URL!;
        if (error) {
          return NextResponse.redirect(new URL(`/login?error=${encodeURIComponent(error.code ?? "sign_in_failed")}`, base));
        }
        return NextResponse.redirect(new URL("/", base));
      },
    })
  : null;
