import { describe, expect, it } from "vitest";
import { signInPath } from "@/lib/signIn";

describe("signInPath", () => {
  it("sends Google straight to Google", () => {
    expect(signInPath("google")).toBe("/auth/login?connection=google-oauth2");
  });

  it("opens Auth0's email form, or its sign-up tab", () => {
    expect(signInPath("email")).toBe("/auth/login?connection=Username-Password-Authentication");
    expect(signInPath("emailSignUp")).toBe(
      "/auth/login?connection=Username-Password-Authentication&screen_hint=signup",
    );
  });

  it("without a method, Auth0's page offers both", () => {
    expect(signInPath()).toBe("/auth/login");
  });
});
