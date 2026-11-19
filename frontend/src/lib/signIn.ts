/** How a player chose to sign in on the sign-in card (/login). */
export type SignInMethod = "google" | "email" | "emailSignUp";

// The tenant's connection names: Auth0's defaults, which the dev tenant uses (FF-003).
export const GOOGLE_CONNECTION = "google-oauth2";
export const EMAIL_CONNECTION = "Username-Password-Authentication";

/**
 * Where the Auth0 SDK's login route sends the player. The SDK passes the query on to
 * Auth0: `connection` skips Auth0's own page (Google goes straight to Google's account
 * picker; email shows only the email form), and `screen_hint=signup` opens its sign-up
 * tab. Without a method, Auth0's page offers both.
 */
export function signInPath(method?: SignInMethod): string {
  switch (method) {
    case "google":
      return `/auth/login?connection=${GOOGLE_CONNECTION}`;
    case "email":
      return `/auth/login?connection=${EMAIL_CONNECTION}`;
    case "emailSignUp":
      return `/auth/login?connection=${EMAIL_CONNECTION}&screen_hint=signup`;
    default:
      return "/auth/login";
  }
}
