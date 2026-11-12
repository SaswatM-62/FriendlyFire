import { screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import { maskEmail } from "@/components/common/GatePages";
import { HistoryList } from "@/components/history/HistoryList";
import { QuizDetail } from "@/components/history/QuizDetail";
import { HomePage } from "@/components/home/HomePage";
import { HowItWorks } from "@/components/home/HowItWorks";
import { ProfileView, linkErrorMessage } from "@/components/profile/ProfileView";
import { RiotCallback } from "@/components/profile/RiotCallback";
import { FAKE_ME } from "@/mocks/data";
import { FakeGateway } from "@/mocks/gateway";
import { location, router } from "@/test/navigation";
import { renderWithApp, signedInSession } from "@/test/render";

describe("HomePage", () => {
  it("signed out: says what FriendlyFire is and offers sign-in", async () => {
    const session = { ...signedInSession(), status: "signedOut" as const };
    renderWithApp(<HomePage />, { session });

    expect(screen.getByRole("heading", { name: "How well do you know your squad?" })).toBeInTheDocument();
    await userEvent.click(screen.getByRole("button", { name: /Sign in to play/ }));
    expect(session.signIn).toHaveBeenCalled();
  });

  it("signed in: create or join, your sync status and who's playing now", async () => {
    renderWithApp(<HomePage />);

    expect(await screen.findByText("Welcome back, Alex")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Create room/ })).toBeInTheDocument();
    expect(await screen.findByText("Synced")).toBeInTheDocument();
    expect(await screen.findByRole("region", { name: "Live right now" })).toHaveTextContent(/rooms playing/);
    expect(screen.getByRole("link", { name: "Profile and history" })).toHaveAttribute("href", "/profile");
  });

  it("offers to link Riot to a new player", async () => {
    renderWithApp(<HomePage />, { gateway: new FakeGateway({ linked: false }) });

    expect(await screen.findByText("Welcome, Alex")).toBeInTheDocument();
    expect(screen.getByRole("region", { name: "Link your Riot account to play" })).toHaveTextContent("You can open or join a room first.");
    expect(screen.getByText("Not linked")).toBeInTheDocument();
  });

  it("offers the room from an invite link opened while signed out", async () => {
    window.localStorage.setItem("ff-pending-invite", "K7QM2X");
    renderWithApp(<HomePage />);

    const invite = await screen.findByRole("status", { name: "Pending invite" });
    await userEvent.click(within(invite).getByRole("button", { name: "Join" }));

    await waitFor(() => expect(router.push).toHaveBeenCalledWith("/room/K7QM2X"));
    expect(window.localStorage.getItem("ff-pending-invite")).toBeNull();
  });
});

describe("HistoryList", () => {
  it("loads more quizzes", async () => {
    renderWithApp(<HistoryList me={FAKE_ME} />);

    expect(await screen.findAllByRole("link", { name: /room/ })).toHaveLength(10);
    await userEvent.click(screen.getByRole("button", { name: "Load more" }));

    await waitFor(() => expect(screen.getAllByRole("link", { name: /room/ })).toHaveLength(14));
    expect(screen.queryByRole("button", { name: "Load more" })).not.toBeInTheDocument();
  });

  it("opening a quiz shows its final scoreboard", async () => {
    renderWithApp(<QuizDetail quizId="q-014" me={FAKE_ME} />);

    expect(await screen.findByText("Final scoreboard")).toBeInTheDocument();
    expect(screen.getByText("You came 2nd")).toBeInTheDocument();
    expect(screen.getByText("Deleted player")).toBeInTheDocument();
  });

  it("an unknown quiz says so", async () => {
    renderWithApp(<QuizDetail quizId="nope" me={FAKE_ME} />);

    expect(await screen.findByText("Quiz not found")).toBeInTheDocument();
  });
});

describe("ProfileView", () => {
  it("checks the display name before saving", async () => {
    renderWithApp(<ProfileView me={FAKE_ME} />);
    const name = screen.getByRole("textbox", { name: "Display name" });

    await userEvent.clear(name);
    await userEvent.type(name, "   ");
    await userEvent.click(screen.getByRole("button", { name: "Save" }));

    expect(screen.getByRole("alert")).toHaveTextContent("Your name needs 1 to 24 characters.");
  });

  it("saves a new display name", async () => {
    const { gateway } = renderWithApp(<ProfileView me={FAKE_ME} />);
    const name = screen.getByRole("textbox", { name: "Display name" });

    await userEvent.clear(name);
    await userEvent.type(name, "Alexandra");
    await userEvent.click(screen.getByRole("button", { name: "Save" }));

    await waitFor(() => expect(gateway.me().displayName).toBe("Alexandra"));
    expect(await screen.findByText("Name saved.")).toBeInTheDocument();
  });

  it("deleting needs the word DELETE typed", async () => {
    const { gateway, session } = renderWithApp(<ProfileView me={FAKE_ME} />);

    await userEvent.click(screen.getByRole("button", { name: "Delete account" }));
    const dialog = screen.getByRole("dialog", { name: "Delete your account?" });
    const confirm = within(dialog).getByRole("button", { name: "Delete account" });
    expect(confirm).toBeDisabled();

    await userEvent.type(within(dialog).getByRole("textbox", { name: "Type DELETE to confirm" }), "DELETE");
    await userEvent.click(confirm);

    await waitFor(() => expect(session.signOut).toHaveBeenCalled());
    expect(gateway.signedIn).toBe(false);
  });

  it("unlinking asks first, then unlinks", async () => {
    const { gateway } = renderWithApp(<ProfileView me={FAKE_ME} />);

    await userEvent.click(screen.getByRole("button", { name: "Unlink Riot account" }));
    await userEvent.click(within(screen.getByRole("dialog")).getByRole("button", { name: "Unlink" }));

    await waitFor(() => expect(gateway.linked).toBe(false));
  });

  it("explains a failed link from the callback", () => {
    location.search = new URLSearchParams("error=RIOT_ALREADY_LINKED");
    renderWithApp(<ProfileView me={FAKE_ME} />);

    expect(screen.getByRole("alert")).toHaveTextContent("That Riot account is already linked to another FriendlyFire account.");
    expect(linkErrorMessage("access_denied")).toBe("You cancelled on Riot's page, so nothing was linked.");
  });

  it("picks a favourite icon, colour and expression", async () => {
    renderWithApp(<ProfileView me={FAKE_ME} />);

    await userEvent.click(screen.getByRole("radio", { name: /Shield/ }));
    await userEvent.click(screen.getByRole("radio", { name: "Blue" }));
    await userEvent.click(screen.getByRole("radio", { name: /Cocky/ }));

    expect(screen.getByText("Shield · blue · Cocky")).toBeInTheDocument();
    expect(screen.getByRole("radio", { name: /Cocky/ })).toHaveAttribute("aria-checked", "true");
    expect(JSON.parse(window.localStorage.getItem("ff-look")!)).toEqual({ icon: "shield", colour: "blue", expression: "cocky" });
  });

  it("offers the Link button, with what we use and the Privacy Policy", async () => {
    renderWithApp(<ProfileView me={{ ...FAKE_ME, riotLink: null }} />);

    expect(screen.getByRole("button", { name: "Link Riot account" })).toBeInTheDocument();
    expect(screen.getByText(/Competitive and Unrated matches from the last 30 days/)).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Privacy Policy" })).toHaveAttribute("href", "/privacy");
  });

  it("lists your recent quizzes, each linking to its results", async () => {
    renderWithApp(<ProfileView me={FAKE_ME} />);

    const recent = screen.getByRole("region", { name: "Recent quizzes" });
    expect(await within(recent).findAllByRole("link", { name: /room/ })).toHaveLength(5);
    expect(within(recent).getByRole("link", { name: "See all history" })).toHaveAttribute("href", "/history");
  });
});

describe("RiotCallback", () => {
  it("sends the code and state to the Gateway, then goes to Profile", async () => {
    location.search = new URLSearchParams("code=fake-code&state=fake-state");
    const { gateway } = renderWithApp(<RiotCallback />);

    await waitFor(() => expect(router.replace).toHaveBeenCalledWith("/profile?linked=1"));
    expect(gateway.linked).toBe(true);
  });

  it("goes back to Profile without calling the Gateway when the player denied", async () => {
    location.search = new URLSearchParams("error=access_denied");
    renderWithApp(<RiotCallback />);

    await waitFor(() => expect(router.replace).toHaveBeenCalledWith("/profile?error=access_denied"));
  });

  it("passes the Gateway's error code on to Profile", async () => {
    location.search = new URLSearchParams("code=taken&state=fake-state");
    renderWithApp(<RiotCallback />);

    await waitFor(() => expect(router.replace).toHaveBeenCalledWith("/profile?error=RIOT_ALREADY_LINKED"));
  });
});

describe("HowItWorks", () => {
  it("walks through a question and states the real rules", () => {
    const session = { ...signedInSession(), status: "signedOut" as const };
    renderWithApp(<HowItWorks />, { session });

    expect(screen.getByRole("heading", { level: 1 })).toHaveTextContent("Trivia about your squad, built from your own matches");
    for (const step of ["Read the question", "Lock in a player", "See the receipts", "Crown a winner"]) {
      expect(screen.getByRole("heading", { name: new RegExp(step) })).toBeInTheDocument();
    }
    // The scoring service: 500 to 1,000 points for a right answer; equal points share a place.
    expect(screen.getByRole("region", { name: "Scoring" })).toHaveTextContent("500 to 1,000 points");
    expect(screen.getByRole("region", { name: "Scoring" })).toHaveTextContent("players with the same points share the place");
    expect(screen.getByRole("button", { name: /Start playing/ })).toBeInTheDocument();
  });
});

describe("gate pages", () => {
  it("masks the email address", () => {
    expect(maskEmail("maya@example.com")).toBe("m***@example.com");
    expect(maskEmail(null)).toBe("your email address");
  });
});
