"use client";

import { useRouter } from "next/navigation";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useFavouriteLook } from "@/components/chrome/useFavouriteLook";
import { useRoomTransport } from "@/components/providers/AppProviders";
import { useApi } from "@/components/providers/ApiProvider";
import { useSession } from "@/components/providers/SessionProvider";
import { useSound } from "@/components/providers/SoundProvider";
import { Countdown, GetReady } from "@/components/room/Countdown";
import { SiteChrome } from "@/components/chrome/SiteChrome";
import { EndedEarlyScreen, PodiumScreen } from "@/components/room/FinishedScreens";
import { Lobby } from "@/components/room/Lobby";
import { HURRY_MS, QuestionScreen } from "@/components/room/QuestionScreen";
import { RevealScreen } from "@/components/room/RevealScreen";
import { RoomBar } from "@/components/room/RoomBar";
import { ChatPanel, usePhone } from "@/components/room/RoomChat";
import { RoomContext, type RoomView } from "@/components/room/RoomContext";
import { RoomMessage } from "@/components/room/RoomMessages";
import { PausedScreen, VoteScreen } from "@/components/room/VoteScreens";
import { ConfirmDialog } from "@/components/ui/Dialog";
import { Spinner } from "@/components/ui/States";
import { CHAT_KEEP, chatLinesFor, type ChatLine } from "@/lib/chat";
import { useToast } from "@/components/ui/Toasts";
import type { RealtimeError, RoomEvent, RoomSnapshot } from "@/lib/api-types";
import { msUntil } from "@/lib/clock";
import { messageFor } from "@/lib/errors";
import { displayNames, endedEarly, playerLooks } from "@/lib/players";
import { useNow } from "@/lib/useNow";
import { useRoomSocket } from "@/lib/useRoomSocket";

function stageStatus(s: RoomSnapshot, showLobby: boolean): string {
  const q = `Question ${s.roundNumber} of ${s.settings.questionCount}`;
  if (showLobby) return `Lobby · ${s.players.length} of 8 players`;
  switch (s.phase) {
    case "LOBBY":
      return `Lobby · ${s.players.length} of 8 players`;
    case "STARTING":
      return s.roundNumber === 0 ? "Starting" : `Question ${s.roundNumber + 1} of ${s.settings.questionCount} · Get ready`;
    case "QUESTION":
      return q;
    case "REVEAL":
      return `${q} · Reveal`;
    case "VOTE":
      return `${q} · Vote`;
    case "PAUSED":
      return `${q} · Paused`;
    case "FINISHED":
      return endedEarly(s) ? "Ended early" : "Final results";
  }
}

/** "You opened the room" when you made it, otherwise "You joined the room". */
function firstLine(s: RoomSnapshot, meId: string): ChatLine {
  const opened = s.hostUserId === meId && s.phase === "LOBBY" && s.players.length <= 1;
  return { id: "c0", kind: "sys", text: opened ? "You opened the room" : "You joined the room" };
}

/** Effects for an event: a sound or a toast. The screen itself always comes from the snapshot. */
function useEventEffects(meId: string) {
  const toast = useToast();
  const { play } = useSound();
  return useCallback(
    (event: RoomEvent, previous: RoomSnapshot | null) => {
      const s = event.snapshot;
      const name = (id?: string) => s.players.find((p) => p.userId === id)?.displayName ?? previous?.players.find((p) => p.userId === id)?.displayName ?? "Someone";
      switch (event.type) {
        case "PLAYER_JOINED":
          if (event.subjectUserId !== meId) play("join");
          break;
        case "HOST_CHANGED":
          toast(event.subjectUserId === meId ? "You're the host now." : `${name(event.subjectUserId)} is now the host.`, "yellow");
          break;
        case "QUESTION_STARTED":
          play("go");
          break;
        case "QUESTION_REVEALED": {
          play("reveal");
          const mine = s.currentQuestion?.reveal?.results.find((r) => r.userId === meId);
          if (mine) setTimeout(() => play(mine.correct ? "correct" : "wrong"), 350);
          break;
        }
        case "PLAYER_RECONNECTED":
          if (previous?.phase === "PAUSED" || previous?.phase === "VOTE") toast(`${name(event.subjectUserId)} is back.`, "mint");
          break;
        case "VOTE_RESOLVED": {
          const who = name(previous?.activeVote?.aboutUserId ?? event.subjectUserId);
          if (s.phase === "PAUSED") toast(`Vote closed: waiting a minute for ${who}.`, "yellow");
          else if (s.phase === "FINISHED") toast("Vote closed: the quiz ends here.", "red");
          else toast(`Vote closed: continuing without ${who}.`, "mint");
          break;
        }
        case "QUIZ_FINISHED":
          if (!endedEarly(s)) play("podium");
          break;
        default:
          break;
      }
    },
    [meId, play, toast],
  );
}

/**
 * A room you're in: one live connection, the latest snapshot, and the screen for its
 * phase (Architecture 3.1, "Rendering a room"). Nothing is decided here: who's host,
 * whether time's up and who was right all come from the server.
 */
export function RoomLive({ code, meId, initial }: { code: string; meId: string; initial: RoomSnapshot }) {
  const api = useApi();
  const session = useSession();
  const router = useRouter();
  const toast = useToast();
  const transport = useRoomTransport();
  const [base, setBase] = useState(initial);
  const [closed, setClosed] = useState(false);
  const [left, setLeft] = useState(false);
  const [confirmLeave, setConfirmLeave] = useState(false);
  const [answerRejections, setAnswerRejections] = useState(0);
  const onEffects = useEventEffects(meId);
  const myExpression = useFavouriteLook().expression;

  // Room chat: what people said and what happened, newest last (planned for the server).
  const [chat, setChat] = useState<ChatLine[]>(() => [firstLine(initial, meId)]);
  const nextLine = useRef(1);
  const phone = usePhone();
  const [chatOpen, setChatOpen] = useState(false);
  const [unread, setUnread] = useState(0);
  const chatVisible = useRef(true);
  useEffect(() => {
    chatVisible.current = !phone || chatOpen;
  }, [phone, chatOpen]);
  // After the podium, "Back to lobby" shows the lobby again until the host starts.
  const [lobbyAfter, setLobbyAfter] = useState<string | null>(null);

  const onEvent = useCallback(
    (event: RoomEvent, previous: RoomSnapshot | null) => {
      if (event.type === "ROOM_CLOSED") setClosed(true);
      const lines = chatLinesFor(event, previous, meId);
      if (lines.length > 0) {
        setChat((all) => [...all, ...lines.map((line): ChatLine => ({ ...line, id: `c${nextLine.current++}` }))].slice(-CHAT_KEEP));
        if (event.type === "CHAT_MESSAGE" && event.chat?.userId !== meId && !chatVisible.current) setUnread((n) => n + 1);
      }
      onEffects(event, previous);
    },
    [onEffects, meId],
  );

  const onError = useCallback(
    (error: RealtimeError) => {
      if (error.destination?.endsWith("/answer")) setAnswerRejections((n) => n + 1);
      toast(messageFor(error.error.code, error.error.message), "red");
    },
    [toast],
  );

  // Back after a dropped connection: rejoin over REST, then the new snapshot takes over (FF-309).
  const onReconnected = useCallback(() => {
    api.joinRoom(code, { quiet: true }).then(setBase, () => {});
  }, [api, code]);

  const socket = useRoomSocket(code, { enabled: !left, transport, initialSnapshot: base, onEvent, onError, onReconnected });
  const snapshot = socket.snapshot ?? base;
  const now = useNow(250);

  const view = useMemo<RoomView>(
    () => ({
      snapshot,
      meId,
      isHost: snapshot.hostUserId === meId,
      looks: playerLooks(snapshot.players, { userId: meId, expression: myExpression }),
      names: displayNames(snapshot.players),
      clockOffsetMs: socket.clockOffsetMs,
      send: socket.send,
      askLeave: () => setConfirmLeave(true),
    }),
    [snapshot, meId, myExpression, socket.clockOffsetMs, socket.send],
  );

  const leave = () => {
    socket.send("leave", {});
    setLeft(true);
    setConfirmLeave(false);
    router.push("/");
  };

  const openChat = () => {
    setChatOpen(true);
    setUnread(0);
  };
  const sendChat = (text: string) => socket.send("chat", { text });

  if (socket.connection === "elsewhere") {
    // The newest connection wins (ADR-017): reloading reconnects here and takes the room back.
    return (
      <RoomMessage
        icon="monitor"
        tone="yellow"
        title="You're playing in another tab or on another device"
        actions={
          <button type="button" className="btn btn--primary" onClick={() => window.location.reload()}>
            Play here
          </button>
        }
      >
        <p className="muted">You opened this room somewhere else, so it closed here. Play here to move back.</p>
      </RoomMessage>
    );
  }
  if (socket.connection === "deleted") {
    return (
      <RoomMessage
        icon="close"
        tone="red"
        title="Your account was deleted"
        actions={
          <button type="button" className="btn btn--primary" onClick={session.signOut}>
            Sign out
          </button>
        }
      >
        <p className="muted">Your FriendlyFire account was deleted on another tab or device, so this room closed here.</p>
      </RoomMessage>
    );
  }
  if (closed) {
    return (
      <RoomMessage icon="door" tone="muted" title="This room has closed">
        <p className="muted">Thanks for playing. Everyone left, or it sat empty for too long. Start a new room any time.</p>
      </RoomMessage>
    );
  }
  const inRoom = snapshot.players.some((p) => p.userId === meId);
  if (!inRoom && !left) {
    return (
      <RoomMessage icon="close" tone="red" title="The host removed you from the room">
        <p className="muted">You can join another room or start your own.</p>
      </RoomMessage>
    );
  }

  const finished = snapshot.phase === "FINISHED";
  const showLobby = finished && lobbyAfter !== null && lobbyAfter === (snapshot.quizId ?? "");
  const backToLobby = () => setLobbyAfter(snapshot.quizId ?? "");

  // The quiz ended early: a message in the site's frame, with the standings (Room closed board).
  if (finished && endedEarly(snapshot) && !showLobby) {
    return (
      <RoomContext.Provider value={view}>
        <SiteChrome>
          <EndedEarlyScreen onBackToLobby={backToLobby} />
        </SiteChrome>
      </RoomContext.Provider>
    );
  }

  const reconnecting = socket.connection === "reconnecting";
  const questionLeft = snapshot.phase === "QUESTION" && now > 0 ? msUntil(snapshot.phaseDeadline, now, socket.clockOffsetMs) : Infinity;
  const motion =
    snapshot.phase === "LOBBY" || showLobby
      ? "lobby"
      : snapshot.phase === "REVEAL"
        ? "reveal"
        : questionLeft > 0 && questionLeft <= HURRY_MS
          ? "last5"
          : "question";

  let screen;
  switch (showLobby ? "LOBBY" : snapshot.phase) {
    case "LOBBY":
      screen = <Lobby />;
      break;
    case "STARTING":
      screen = snapshot.roundNumber === 0 ? <Countdown /> : <GetReady key={snapshot.roundNumber} />;
      break;
    case "QUESTION":
      screen = snapshot.currentQuestion ? <QuestionScreen key={snapshot.currentQuestion.questionId} rejections={answerRejections} /> : null;
      break;
    case "REVEAL":
      screen = snapshot.currentQuestion?.reveal ? <RevealScreen key={snapshot.currentQuestion.questionId} /> : null;
      break;
    case "VOTE":
      screen = snapshot.activeVote ? <VoteScreen key={snapshot.activeVote.aboutUserId + snapshot.activeVote.deadline} /> : null;
      break;
    case "PAUSED":
      screen = <PausedScreen key={snapshot.phaseDeadline} />;
      break;
    case "FINISHED":
      screen = null;
      break;
  }

  const chatPanel = <ChatPanel lines={chat} onSend={sendChat} onClose={() => setChatOpen(false)} />;
  const podium = finished && !showLobby;

  return (
    <RoomContext.Provider value={view}>
      <div className="c" data-chat={phone && chatOpen ? "open" : "closed"} data-motion={motion}>
        <RoomBar
          code={code}
          status={stageStatus(snapshot, showLobby)}
          quality={socket.quality}
          unread={unread}
          onChat={openChat}
          onLeave={() => setConfirmLeave(true)}
        />
        <main className="main" id="main">
          {podium ? (
            <PodiumScreen key={snapshot.quizId} chat={chatPanel} onBackToLobby={backToLobby} />
          ) : (
            <div className="room">
              <section className="room__main" aria-label="Quiz">
                {reconnecting && (
                  <div className="card recon" role="status">
                    <Spinner size={22} />
                    <p>
                      <strong>Reconnecting…</strong> <span className="muted">Your answers are safe.</span>
                    </p>
                  </div>
                )}
                {reconnecting ? (
                  <div className="faded room__main" inert>
                    {screen}
                  </div>
                ) : (
                  screen
                )}
              </section>
              <aside className="room__side" aria-label="Room chat">
                {chatPanel}
              </aside>
            </div>
          )}
        </main>
      </div>
      <ConfirmDialog
        open={confirmLeave}
        title="Leave the room?"
        confirmLabel="Leave room"
        onCancel={() => setConfirmLeave(false)}
        onConfirm={leave}
      >
        {snapshot.phase === "LOBBY"
          ? "You can come back with the code while the room is open."
          : "You'll lose your place in this quiz. Your squad can carry on without you."}
      </ConfirmDialog>
    </RoomContext.Provider>
  );
}
