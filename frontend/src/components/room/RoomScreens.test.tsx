import { screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { GetReady } from "@/components/room/Countdown";
import { EndedEarlyScreen, PodiumScreen } from "@/components/room/FinishedScreens";
import { Lobby } from "@/components/room/Lobby";
import { QuestionScreen } from "@/components/room/QuestionScreen";
import { RevealScreen, fastestCorrect } from "@/components/room/RevealScreen";
import { previousRanks } from "@/components/room/Scoreboard";
import { PausedScreen, VoteScreen } from "@/components/room/VoteScreens";
import type { RoomSnapshot, ScoreEntry } from "@/lib/api-types";
import { FAKE_PLAYERS, ME_ID } from "@/mocks/data";
import { fakePlayer } from "@/mocks/fakeRoom";
import { lobbySnapshot, renderRoom } from "@/test/room";

const p = FAKE_PLAYERS;
const soon = () => new Date(Date.now() + 15_000).toISOString();

function board(entries: [string, string, number, number][]): ScoreEntry[] {
  return entries.map(([userId, displayName, totalPoints, pointsLastRound], i) => ({
    userId,
    displayName,
    totalPoints,
    pointsLastRound,
    rank: i + 1,
    correctAnswers: 3 - Math.min(i, 3),
  }));
}

function questionSnapshot(over: Partial<RoomSnapshot> = {}): RoomSnapshot {
  return lobbySnapshot({
    phase: "QUESTION",
    quizId: "quiz-1",
    roundNumber: 3,
    phaseDeadline: soon(),
    currentQuestion: {
      questionId: "q3",
      text: "Who has the highest headshot rate?",
      options: [p.alex, p.maya, p.sam, p.jordan].map((x) => ({ optionId: x.userId, label: x.displayName })),
    },
    scoreboard: board([
      [p.alex.userId, "Alex", 1200, 0],
      [p.maya.userId, "Maya", 900, 0],
      [p.sam.userId, "Sam", 400, 0],
      [p.jordan.userId, "Jordan", 0, 0],
    ]),
    ...over,
  });
}

describe("Lobby", () => {
  it("the host can't start until everyone is ready, and sees why", () => {
    const snapshot = lobbySnapshot({}, [
      fakePlayer(p.alex),
      fakePlayer(p.maya, { dataStatus: "SYNCING", dataEstimatedSeconds: 170 }),
      fakePlayer(p.casey, { dataStatus: "NOT_LINKED" }),
    ]);
    renderRoom(<Lobby />, snapshot);

    const start = screen.getByRole("button", { name: "Start quiz" });
    expect(start).toBeDisabled();
    const reasons = document.getElementById(start.getAttribute("aria-describedby") ?? "")!;
    expect(reasons).toHaveTextContent("Waiting on 2");
    expect(reasons).toHaveTextContent("Maya is syncing (~3 min)");
    expect(reasons).toHaveTextContent("Casey needs to link Riot");
    expect(screen.getByText(/Syncing · ~3 min/)).toBeInTheDocument();
  });

  it("the host starts, changes settings and can remove players", async () => {
    const { send } = renderRoom(<Lobby />, lobbySnapshot());

    await userEvent.click(screen.getByRole("button", { name: "Start quiz" }));
    await userEvent.click(screen.getByRole("button", { name: "More questions" }));
    await userEvent.click(screen.getByRole("button", { name: "Less time" }));
    await userEvent.click(screen.getByRole("button", { name: "Remove Maya" }));
    await userEvent.click(within(screen.getByRole("dialog")).getByRole("button", { name: "Remove Maya" }));

    expect(send.mock.calls).toEqual([
      ["start", {}],
      ["settings", { questionCount: 11, secondsPerQuestion: 20 }],
      ["settings", { questionCount: 10, secondsPerQuestion: 15 }],
      ["kick", { targetUserId: p.maya.userId }],
    ]);
  });

  it("guests see the settings and who they're waiting for, without host controls", () => {
    renderRoom(<Lobby />, lobbySnapshot({ hostUserId: p.maya.userId }));

    expect(screen.getByText("Only the host can change the quiz settings.")).toBeInTheDocument();
    expect(screen.getByText("Waiting for Maya to start")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Start quiz" })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /Remove/ })).not.toBeInTheDocument();
  });

  it("tags players with no recent matches and disconnected players", () => {
    renderRoom(
      <Lobby />,
      lobbySnapshot({}, [fakePlayer(p.alex), fakePlayer(p.jordan, { noRecentMatches: true }), fakePlayer(p.sam, { status: "DISCONNECTED" })]),
    );

    expect(screen.getByText("No recent matches")).toBeInTheDocument();
    expect(screen.getByText("Disconnected")).toBeInTheDocument();
    expect(screen.getAllByText("Open spot")).toHaveLength(5);
  });
});

describe("QuestionScreen", () => {
  it("locks in an answer that can't be changed", async () => {
    const { send } = renderRoom(<QuestionScreen rejections={0} />, questionSnapshot());

    await userEvent.click(screen.getByRole("button", { name: /Maya/ }));

    expect(send).toHaveBeenCalledWith("answer", { questionId: "q3", optionId: p.maya.userId });
    expect(screen.getByRole("button", { name: /Maya/ })).toHaveAttribute("aria-pressed", "true");
    expect(screen.getByRole("button", { name: /Sam/ })).toBeDisabled();
    await userEvent.click(screen.getByRole("button", { name: /Sam/ }));
    expect(send).toHaveBeenCalledOnce();
  });

  it("unlocks again when the server rejects the answer", async () => {
    const snapshot = questionSnapshot();
    const { rerenderWith } = renderRoom(<QuestionScreen rejections={0} />, snapshot);
    await userEvent.click(screen.getByRole("button", { name: /Maya/ }));

    rerenderWith(snapshot, <QuestionScreen rejections={1} />);

    expect(screen.getByRole("button", { name: /Sam/ })).toBeEnabled();
  });

  it("shows how many have locked in; those still answering look Thinking", () => {
    const snapshot = questionSnapshot();
    snapshot.players[1].answeredCurrentQuestion = true;
    const { container } = renderRoom(<QuestionScreen rejections={0} />, snapshot);

    expect(screen.getByText("1 of 4 locked in")).toBeInTheDocument();
    const strip = [...container.querySelectorAll(".faces .nf-char")].map((c) => c.getAttribute("data-face"));
    // Alex (cocky), Maya answered (smirk), Sam and Jordan still thinking.
    expect(strip).toEqual(["thinking", "smirk", "thinking", "thinking"]);
  });

  it("dims the other tiles once you lock in", async () => {
    const { container } = renderRoom(<QuestionScreen rejections={0} />, questionSnapshot());

    await userEvent.click(screen.getByRole("button", { name: /Maya/ }));

    const tiles = [...container.querySelectorAll(".tile")].map((t) => t.className.replace("tile", "").trim());
    expect(tiles).toEqual(["tile--dim", "tile--locked", "tile--dim", "tile--dim"]);
    expect(screen.getByRole("button", { name: /Maya/ })).toHaveTextContent("Locked in");
  });

  it("says time's up when you didn't lock in", () => {
    renderRoom(<QuestionScreen rejections={0} />, questionSnapshot({ phaseDeadline: new Date(Date.now() - 1_000).toISOString() }));

    expect(screen.getByText("You didn't lock in, so no points this time.")).toBeInTheDocument();
    expect(screen.getByRole("timer", { name: "Time's up" })).toBeInTheDocument();
  });
});

describe("RevealScreen", () => {
  const reveal = (alexPicked: string, alexPoints: number) =>
    questionSnapshot({
      phase: "REVEAL",
      currentQuestion: {
        ...questionSnapshot().currentQuestion!,
        reveal: {
          correctOptionId: p.maya.userId,
          explanation: "Maya landed 31% headshots across their last 38 matches.",
          results: [
            { userId: p.alex.userId, chosenOptionId: alexPicked, correct: alexPicked === p.maya.userId, points: alexPoints },
            { userId: p.maya.userId, chosenOptionId: p.sam.userId, correct: false, points: 0 },
            { userId: p.sam.userId, chosenOptionId: p.maya.userId, correct: true, points: 610 },
            { userId: p.jordan.userId, correct: false, points: 0 },
          ],
        },
      },
      scoreboard: board([
        [p.alex.userId, "Alex", 1940, alexPoints],
        [p.maya.userId, "Maya", 900, 0],
        [p.sam.userId, "Sam", 1010, 610],
        [p.jordan.userId, "Jordan", 0, 0],
      ]),
    });

  it("highlights the right player and shows the receipts", () => {
    renderRoom(<RevealScreen />, reveal(p.maya.userId, 740));

    const correct = screen.getByText("Correct").closest(".tile")!;
    expect(correct).toHaveClass("tile--ok");
    expect(correct).toHaveTextContent("Maya");
    // Maya is the answer: her character says Wow.
    expect(correct.querySelector(":scope > .av .nf-char")).toHaveAttribute("data-face", "wow");
    expect(screen.getByText("Maya landed 31% headshots across their last 38 matches.")).toBeInTheDocument();
    expect(screen.getByText("Called it!")).toBeInTheDocument();
    expect(screen.getByLabelText("Picked by you, Sam")).toBeInTheDocument();
  });

  it("says who it was when you were wrong", () => {
    renderRoom(<RevealScreen />, reveal(p.sam.userId, 0));

    expect(screen.getByText("So close!")).toBeInTheDocument();
    expect(screen.getByText("It was Maya. You picked Sam.")).toBeInTheDocument();
    expect(screen.getByText("Wrong pick").closest(".tile")).toHaveTextContent("Sam");
  });

  it("counts down to the next question", () => {
    renderRoom(<RevealScreen />, reveal(p.maya.userId, 740));

    expect(screen.getByRole("timer", { name: /^Next question in \d+ seconds$/ })).toBeInTheDocument();
  });

  it("counts down to the final results after the last question", () => {
    renderRoom(<RevealScreen />, { ...reveal(p.maya.userId, 740), roundNumber: 10 });

    expect(screen.getByRole("timer", { name: /^Final results in \d+ seconds$/ })).toBeInTheDocument();
  });

  it("works out the fastest correct answer and rank changes", () => {
    expect(fastestCorrect(reveal(p.maya.userId, 740).currentQuestion!.reveal!.results)).toBe(p.alex.userId);
    const ranks = previousRanks(board([
      [p.alex.userId, "Alex", 1940, 740],
      [p.sam.userId, "Sam", 1010, 610],
      [p.maya.userId, "Maya", 900, 0],
    ]));
    // Before this round: Alex 1200, Maya 900, Sam 400.
    expect([...ranks.entries()]).toEqual([
      [p.alex.userId, 1],
      [p.sam.userId, 3],
      [p.maya.userId, 2],
    ]);
  });
});

describe("GetReady", () => {
  it("names the next question and says where you stand", () => {
    renderRoom(
      <GetReady />,
      questionSnapshot({
        phase: "STARTING",
        currentQuestion: undefined,
        scoreboard: board([
          [p.maya.userId, "Maya", 3180, 740],
          [p.alex.userId, "Alex", 3150, 610],
          [p.sam.userId, "Sam", 1010, 0],
        ]),
      }),
    );

    expect(screen.getByRole("heading", { name: "Question 4 of 10" })).toBeInTheDocument();
    expect(screen.getByText("You're 2nd")).toBeInTheDocument();
    expect(screen.getByText("3,150 points · 30 behind Maya")).toBeInTheDocument();
  });
});

describe("VoteScreen and PausedScreen", () => {
  const voteSnapshot = (pauseAllowed = true): RoomSnapshot =>
    lobbySnapshot({
      phase: "VOTE",
      quizId: "quiz-1",
      roundNumber: 3,
      players: [fakePlayer(p.alex), fakePlayer(p.maya, { status: "DISCONNECTED" }), fakePlayer(p.sam), fakePlayer(p.jordan)],
      activeVote: { aboutUserId: p.maya.userId, deadline: soon(), pauseVotes: 1, continueVotes: 0, endVotes: 0, votedUserIds: [p.sam.userId], pauseAllowed },
    });

  it("casts one vote and shows the counts", async () => {
    const { send } = renderRoom(<VoteScreen />, voteSnapshot());

    expect(screen.getByText("Maya disconnected.")).toBeInTheDocument();
    expect(screen.getByText("1 of 3 players voted.", { exact: false })).toBeInTheDocument();
    await userEvent.click(screen.getByRole("button", { name: /End quiz/ }));

    expect(send).toHaveBeenCalledWith("vote", { option: "END_QUIZ" });
    expect(screen.getByRole("button", { name: /End quiz/ })).toHaveAttribute("aria-pressed", "true");
    expect(screen.getByRole("button", { name: /Continue without Maya/ })).toBeDisabled();
  });

  it("offers no pause once the pause time is used up", () => {
    renderRoom(<VoteScreen />, voteSnapshot(false));

    expect(screen.getByRole("button", { name: /Wait 1 min/ })).toBeDisabled();
    expect(screen.getByText("No pause time left")).toBeInTheDocument();
  });

  it("shows who the room is waiting for and the pause time left", () => {
    renderRoom(
      <PausedScreen />,
      lobbySnapshot({ phase: "PAUSED", waitingForUserId: p.maya.userId, pauseSecondsLeft: 120, phaseDeadline: soon() }),
    );

    expect(screen.getByRole("heading", { name: "Waiting for Maya" })).toBeInTheDocument();
    expect(document.querySelector(".nf-char")).toHaveAttribute("data-face", "away");
    expect(screen.getByText("Pause time left: 2:00 of 3:00")).toBeInTheDocument();
  });
});

describe("the end of the quiz", () => {
  const finished = (over: Partial<RoomSnapshot> = {}) =>
    lobbySnapshot({
      phase: "FINISHED",
      quizId: "quiz-1",
      roundNumber: 10,
      endReason: "COMPLETED",
      scoreboard: board([
        [p.maya.userId, "Maya", 7420, 0],
        [p.sam.userId, "Sam", 6980, 0],
        [p.alex.userId, "Alex", 6150, 0],
        [p.jordan.userId, "Jordan", 5300, 0],
      ]),
      ...over,
    });

  it("crowns the winner, lists the standings, and goes back to the lobby", async () => {
    const backToLobby = vi.fn();
    renderRoom(<PodiumScreen chat={<p>chat</p>} onBackToLobby={backToLobby} />, finished());

    expect(screen.getByRole("heading", { level: 1 })).toHaveTextContent("Maya wins!");
    const standings = screen.getByRole("region", { name: "Final standings" });
    expect(within(standings).getAllByRole("listitem")).toHaveLength(4);
    expect(within(standings).getAllByText("3 of 10 correct")).toHaveLength(1);
    expect(screen.getByText("chat")).toBeInTheDocument();
    await userEvent.click(screen.getByRole("button", { name: "Back to lobby" }));

    expect(backToLobby).toHaveBeenCalled();
    expect(screen.getByRole("link", { name: "Back to home" })).toHaveAttribute("href", "/");
  });

  it("explains an early end", () => {
    renderRoom(<EndedEarlyScreen onBackToLobby={() => {}} />, finished({ endReason: "VOTED_TO_END", roundNumber: 3 }));

    expect(screen.getByText(/Most of you voted to end it. Here's how it stood after 3 questions./)).toBeInTheDocument();
    expect(within(screen.getByRole("list", { name: "Standings so far" })).getAllByRole("listitem")).toHaveLength(4);
  });

  it("says you won when you did", () => {
    renderRoom(
      <PodiumScreen chat={null} onBackToLobby={() => {}} />,
      finished({ scoreboard: board([[ME_ID, "Alex", 9000, 0], [p.maya.userId, "Maya", 100, 0]]) }),
    );

    expect(screen.getByRole("heading", { level: 1 })).toHaveTextContent("You win!");
  });
});
