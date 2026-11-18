import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it } from "vitest";
import { SignInCard } from "@/components/common/SignInCard";
import { location } from "@/test/navigation";
import { renderWithApp, signedInSession } from "@/test/render";

const signedOut = () => ({ ...signedInSession(), status: "signedOut" as const });

describe("SignInCard", () => {
  beforeEach(() => {
    location.search = new URLSearchParams();
  });

  it("goes straight to Google, or to Auth0's email form", async () => {
    const session = signedOut();
    renderWithApp(<SignInCard available />, { session });

    await userEvent.click(screen.getByRole("button", { name: /Continue with Google/ }));
    expect(session.signInWith).toHaveBeenLastCalledWith("google");
    await userEvent.click(screen.getByRole("button", { name: /Continue with email/ }));
    expect(session.signInWith).toHaveBeenLastCalledWith("email");
  });

  it("signs up with email on Auth0's sign-up tab", async () => {
    const session = signedOut();
    renderWithApp(<SignInCard available />, { session });

    const link = screen.getByRole("link", { name: "sign up with email" });
    expect(link).toHaveAttribute("href", "/auth/login?connection=Username-Password-Authentication&screen_hint=signup");
    await userEvent.click(link);
    expect(session.signInWith).toHaveBeenLastCalledWith("emailSignUp");
  });

  it("says when a sign-in didn't finish", () => {
    location.search = new URLSearchParams("error=authorization_error");
    renderWithApp(<SignInCard available />, { session: signedOut() });

    expect(screen.getByRole("alert")).toHaveTextContent("Sign-in didn't finish. Try again.");
  });

  it("explains when sign-in isn't set up on the server", () => {
    renderWithApp(<SignInCard available={false} />, { session: signedOut() });

    expect(screen.queryByRole("button", { name: /Continue with Google/ })).not.toBeInTheDocument();
    expect(screen.getByText(/Sign-in isn't set up on this server yet/)).toHaveAttribute("role", "status");
  });
});
