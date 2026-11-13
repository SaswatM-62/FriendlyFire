/**
 * Auth0 post-login Action: puts what the Gateway needs into the access token (FF-003,
 * FF-602). Access tokens carry only the subject by default; the Gateway reads these
 * claims (services/gateway-service/.../security/TokenClaims.java):
 *
 *   <namespace>email           the player's email (invite list, EnsureUser)
 *   <namespace>email_verified  email/password accounts must verify before playing
 *   <namespace>name            the suggested display name for a new player
 *   <namespace>roles           "admin" for the project managers
 *
 * NAMESPACE must equal AUTH0_CLAIM_NAMESPACE in .env (default below). Auth0 recommends
 * namespacing custom claims, so they can't clash with its own; the namespace is only a
 * name and is never fetched.
 *
 * Install it in each tenant (dev now, production in FF-608):
 *   1. Actions > Library > Create Action > Build from scratch, trigger "Login / Post Login".
 *   2. Paste this file, then Deploy.
 *   3. Actions > Triggers > post-login: drag the Action into the flow and Apply.
 *   4. User Management > Roles: create "admin" and assign it to the project managers.
 * Check it: sign in on the frontend, then GET /api/me answers 200 (403
 * EMAIL_NOT_VERIFIED means the Action isn't running).
 */
const NAMESPACE = "https://friendlyfire.dev/";

exports.onExecutePostLogin = async (event, api) => {
  api.accessToken.setCustomClaim(`${NAMESPACE}email`, event.user.email ?? "");
  api.accessToken.setCustomClaim(`${NAMESPACE}email_verified`, event.user.email_verified === true);
  api.accessToken.setCustomClaim(`${NAMESPACE}name`, event.user.name ?? event.user.nickname ?? "");
  api.accessToken.setCustomClaim(`${NAMESPACE}roles`, event.authorization?.roles ?? []);
};
