import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it } from "vitest";
import { Welcome } from "@/components/welcome/Welcome";
import { clearInvite, rememberInvite } from "@/lib/preferences";
import { FakeGateway, NEW_PLAYER_NAME } from "@/mocks/gateway";
import { location, router } from "@/test/navigation";
import { renderWithApp } from "@/test/render";

function newPlayer(options: { linked?: boolean } = {}) {
  const gateway = new FakeGateway({ linked: options.linked ?? false });
  gateway.newPlayer();
  if (options.linked) gateway.setLinked(true);
  return gateway;
}

describe("Welcome", () => {
  beforeEach(() => {
    location.search = new URLSearchParams();
    window.localStorage.clear();
    clearInvite();
  });

  it("walks a new player through welcome, name and Riot, then finishes the tutorial", async () => {
    const gateway = newPlayer();
    renderWithApp(<Welcome me={gateway.me()} />, { gateway });

    expect(screen.getByRole("heading", { name: "Welcome to FriendlyFire" })).toBeInTheDocument();
    expect(screen.getByText("Step 1 of 3")).toBeInTheDocument();
    await userEvent.click(screen.getByRole("button", { name: /Set up my account/ }));

    // An email sign-up's "Player 4821" is the hint, not the value.
    const field = screen.getByRole("textbox", { name: "Your name" });
    expect(field).toHaveValue("");
    expect(field).toHaveAttribute("placeholder", NEW_PLAYER_NAME);
    await userEvent.type(field, "Sam");
    await userEvent.click(screen.getByRole("button", { name: /Save and continue/ }));

    expect(await screen.findByRole("heading", { name: "Link your Riot account" })).toBeInTheDocument();
    expect(gateway.me().displayName).toBe("Sam");
    await userEvent.click(screen.getByRole("button", { name: "Later" }));

    expect(await screen.findByRole("heading", { name: "Nearly there" })).toBeInTheDocument();
    await waitFor(() => expect(gateway.onboarded).toBe(true));
    expect(screen.getByRole("button", { name: /Create a room/ })).toBeInTheDocument();
  });

  it("can't skip the name step, but an empty name keeps the suggested one", async () => {
    const gateway = newPlayer();
    window.localStorage.setItem("ff-welcome-seen", "1");
    renderWithApp(<Welcome me={gateway.me()} />, { gateway });

    // A return visit starts at the name step; there's no Skip yet.
    expect(screen.getByRole("heading", { name: "What should your friends call you?" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Skip tutorial" })).not.toBeInTheDocument();
    await userEvent.click(screen.getByRole("button", { name: /Save and continue/ }));

    expect(await screen.findByRole("heading", { name: "Link your Riot account" })).toBeInTheDocument();
    expect(gateway.me().displayName).toBe(NEW_PLAYER_NAME);
  });

  it("skips the rest of the tutorial from the Riot step and goes home", async () => {
    const gateway = newPlayer();
    window.localStorage.setItem("ff-welcome-seen", "1");
    renderWithApp(<Welcome me={gateway.me()} />, { gateway });
    await userEvent.click(screen.getByRole("button", { name: /Save and continue/ }));

    await userEvent.click(await screen.findByRole("button", { name: "Skip tutorial" }));

    await waitFor(() => expect(router.push).toHaveBeenCalledWith("/"));
    expect(gateway.onboarded).toBe(true);
  });

  it("comes back from Riot to the finish screen and offers the room they were invited to", async () => {
    const gateway = newPlayer({ linked: true });
    location.search = new URLSearchParams("linked=1");
    rememberInvite("K7QM2X");
    renderWithApp(<Welcome me={gateway.me()} />, { gateway });

    expect(screen.getByRole("heading", { name: "You're all set" })).toBeInTheDocument();
    await waitFor(() => expect(gateway.onboarded).toBe(true));
    await userEvent.click(screen.getByRole("button", { name: /Join room K7QM2X/ }));
    expect(router.push).toHaveBeenCalledWith("/room/K7QM2X");
  });

  it("shows why linking failed on the Riot step", () => {
    const gateway = newPlayer();
    location.search = new URLSearchParams("error=access_denied");
    renderWithApp(<Welcome me={gateway.me()} />, { gateway });

    expect(screen.getByRole("alert")).toHaveTextContent("You cancelled on Riot's page, so nothing was linked.");
  });

  it("replays from How it works without saving anything", async () => {
    const gateway = new FakeGateway();
    location.search = new URLSearchParams("replay=1");
    renderWithApp(<Welcome me={gateway.me()} />, { gateway });

    expect(screen.getByRole("heading", { name: "Welcome to FriendlyFire" })).toBeInTheDocument();
    expect(router.replace).not.toHaveBeenCalledWith("/");
  });

  it("sends someone who already finished home", async () => {
    const gateway = new FakeGateway();
    renderWithApp(<Welcome me={gateway.me()} />, { gateway });

    await waitFor(() => expect(router.replace).toHaveBeenCalledWith("/"));
  });
});
