"use client";

import { useState } from "react";
import { DataBadge } from "@/components/common/DataBadge";
import { usePlayerInfo, useRoom } from "@/components/room/RoomContext";
import { Av, Mini } from "@/components/ui/Character";
import { ConfirmDialog } from "@/components/ui/Dialog";
import { Icon } from "@/components/ui/Icon";
import { NetBars } from "@/components/ui/NetBars";
import { useToast } from "@/components/ui/Toasts";
import type { Player } from "@/lib/api-types";
import { dataStatusOf, startWaits } from "@/lib/players";

const MAX_PLAYERS = 8;

/** Host, You, then what they're waiting on, or Ready (Lobby boards). */
function PlayerTags({ player, isMe, isHost }: { player: Player; isMe: boolean; isHost: boolean }) {
  const status = dataStatusOf(player);
  const tags = [];
  if (isHost) {
    tags.push(
      <span key="host" className="tag tag--yellow">
        <Icon name="crown" size={12} />
        Host
      </span>,
    );
  }
  if (isMe) tags.push(<span key="you" className="tag tag--pink">You</span>);
  if (player.status === "DISCONNECTED") {
    tags.push(
      <span key="away" className="tag tag--red">
        <Icon name="wifiOff" size={12} />
        Disconnected
      </span>,
    );
  } else if (status !== "SYNCED") {
    tags.push(<DataBadge key="data" status={status} estimatedSeconds={player.dataEstimatedSeconds} />);
  } else if (player.noRecentMatches) {
    tags.push(
      <span key="recent" className="tag tag--cyan" title="They can play, but no question will be about them.">
        No recent matches
      </span>,
    );
  } else if (!isMe) {
    tags.push(
      <span key="ready" className="tag tag--mint">
        Ready
      </span>,
    );
  }
  if (player.look?.random) tags.push(<span key="random" className="tag tag--violet">Random look</span>);
  return (
    <span className="row" style={{ justifyContent: "center", minHeight: 22, gap: 6, flexWrap: "wrap" }}>
      {tags}
    </span>
  );
}

function PlayerCard({ player, onKick }: { player: Player; onKick?: () => void }) {
  const { snapshot, meId } = useRoom();
  const info = usePlayerInfo()(player.userId);
  const away = player.status !== "CONNECTED";
  return (
    <div className="pl">
      <span className="pl__net">
        <NetBars quality={away ? null : player.connectionQuality} />
      </span>
      {onKick && (
        <button type="button" className="btn btn--icon pl__kick" aria-label={`Remove ${info.name}`} onClick={onKick}>
          <Icon name="close" size={16} />
        </button>
      )}
      <Av look={info.look} size={64} away={away} face={player.status === "DISCONNECTED" ? "away" : undefined} />
      <span className="pl__nm">{info.name}</span>
      <PlayerTags player={player} isMe={player.userId === meId} isHost={snapshot.hostUserId === player.userId} />
    </div>
  );
}

function Stepper({
  label,
  hint,
  value,
  min,
  max,
  step,
  less,
  more,
  onChange,
}: {
  label: string;
  hint: string;
  value: number;
  min: number;
  max: number;
  step: number;
  less: string;
  more: string;
  onChange: (value: number) => void;
}) {
  return (
    <div className="stepper">
      <span className="label">{label}</span>
      <div className="stepper__row">
        <button type="button" className="btn btn--icon" aria-label={less} disabled={value <= min} onClick={() => onChange(Math.max(min, value - step))}>
          <Icon name="minus" />
        </button>
        <span className="num" aria-live="polite" aria-label={`${value} ${label.toLowerCase()}`}>
          {value}
        </span>
        <button type="button" className="btn btn--icon" aria-label={more} disabled={value >= max} onClick={() => onChange(Math.min(max, value + step))}>
          <Icon name="plus" />
        </button>
      </div>
      <span className="small faint" style={{ textAlign: "center" }}>
        {hint}
      </span>
    </div>
  );
}

/** The room code, the invite link and the quiz settings (steppers for the host). */
function Head() {
  const { snapshot, isHost, send } = useRoom();
  const toast = useToast();
  const { questionCount, secondsPerQuestion } = snapshot.settings;

  const copy = async (text: string, done: string) => {
    try {
      await navigator.clipboard.writeText(text);
      toast(done, "cyan");
    } catch {
      toast(`Copy this: ${text}`, "cyan");
    }
  };
  const link = () => `${window.location.origin}/room/${snapshot.roomCode}`;

  return (
    <section className="card head nf-phase" aria-labelledby="h-lobby">
      <div className="head__code">
        <h1 id="h-lobby" className="label">
          Room code
        </h1>
        <div className="row">
          <span className="bigcode">{snapshot.roomCode}</span>
          <button type="button" className="btn btn--icon" aria-label="Copy room code" onClick={() => copy(snapshot.roomCode, "Room code copied.")}>
            <Icon name="copy" />
          </button>
          <button type="button" className="btn btn--line btn--sm" onClick={() => copy(link(), "Invite link copied.")}>
            <Icon name="link" />
            Copy invite link
          </button>
        </div>
        <p className="small muted">{isHost ? "Read it out on voice chat, or send the invite link." : "Only the host can change the quiz settings."}</p>
      </div>
      <div className="settings" role="group" aria-label="Quiz settings">
        {isHost ? (
          <>
            <Stepper
              label="Questions"
              hint="5 to 20"
              value={questionCount}
              min={5}
              max={20}
              step={1}
              less="Fewer questions"
              more="More questions"
              onChange={(n) => send("settings", { questionCount: n, secondsPerQuestion })}
            />
            <Stepper
              label="Seconds each"
              hint="10 to 45"
              value={secondsPerQuestion}
              min={10}
              max={45}
              step={5}
              less="Less time"
              more="More time"
              onChange={(n) => send("settings", { questionCount, secondsPerQuestion: n })}
            />
          </>
        ) : (
          <>
            <div className="stepper ro">
              <span className="label">Questions</span>
              <span className="num">{questionCount}</span>
              <span className="small faint" style={{ textAlign: "center" }}>
                5 to 20
              </span>
            </div>
            <div className="stepper ro">
              <span className="label">Seconds each</span>
              <span className="num">{secondsPerQuestion}</span>
              <span className="small faint" style={{ textAlign: "center" }}>
                10 to 45
              </span>
            </div>
          </>
        )}
      </div>
    </section>
  );
}

/** Who we're waiting on, Leave room, and Start for the host. */
function Actions() {
  const { snapshot, isHost, meId, names, send, askLeave } = useRoom();
  const info = usePlayerInfo();
  const waits = startWaits(snapshot, names);
  const host = info(snapshot.hostUserId);
  const me = snapshot.players.find((p) => p.userId === meId);
  const ready = waits.length === 0;

  return (
    <section className="card actions" aria-label={isHost ? "Start" : "Status"}>
      <p className="actions__status" role="status" id="start-reasons">
        {isHost && ready ? (
          <span className="h2" style={{ fontSize: 17, color: "var(--mint-text)" }}>
            <Icon name="check" />
            Everyone&apos;s ready
          </span>
        ) : (
          <>
            <span className="label" style={{ color: "var(--yellow-text)" }}>
              {isHost ? `Waiting on ${waits.length}` : `Waiting for ${host.name} to start`}
            </span>
            {waits.map((wait) => (
              <span key={wait.text}>
                {wait.userId && <Mini look={info(wait.userId).look} />}
                {wait.text}
              </span>
            ))}
            {!isHost && me?.look?.random && (
              <span>
                <Mini look={info(meId).look} />
                Someone joined first with your favourite icon or colour, so you got a free one for this game.
              </span>
            )}
          </>
        )}
      </p>
      <button type="button" className="btn btn--ghost" onClick={askLeave}>
        Leave room
      </button>
      {isHost && (
        <button
          type="button"
          className="btn btn--primary"
          style={{ width: 220 }}
          disabled={!ready}
          aria-describedby={ready ? undefined : "start-reasons"}
          onClick={() => send("start", {})}
        >
          Start quiz
          {ready && <Icon name="arrowRight" />}
        </button>
      )}
    </section>
  );
}

/** The lobby (FF-505, Lobby boards): the code and settings, who's here, and Start. */
export function Lobby() {
  const { snapshot, isHost, meId, send } = useRoom();
  const info = usePlayerInfo();
  const [kicking, setKicking] = useState<string | null>(null);
  const spots = Math.max(0, MAX_PLAYERS - snapshot.players.length);

  return (
    <>
      <Head />
      <section className="players" aria-label="Players">
        {snapshot.players.map((player) => (
          <PlayerCard
            key={player.userId}
            player={player}
            onKick={isHost && player.userId !== meId ? () => setKicking(player.userId) : undefined}
          />
        ))}
        {Array.from({ length: spots }, (_, i) => (
          <div key={`open-${i}`} className="pl pl--empty">
            <Icon name="plus" size={22} />
            <span className="h2" style={{ fontSize: 16 }}>
              Open spot
            </span>
            <span className="small">Share the code</span>
          </div>
        ))}
      </section>
      <Actions />
      <ConfirmDialog
        open={kicking !== null}
        title={`Remove ${kicking ? info(kicking).name : ""}?`}
        danger
        confirmLabel={`Remove ${kicking ? info(kicking).name : ""}`}
        onCancel={() => setKicking(null)}
        onConfirm={() => {
          if (kicking) send("kick", { targetUserId: kicking });
          setKicking(null);
        }}
      >
        {kicking ? info(kicking).name : "They"} leave the room straight away and see &quot;The host removed you from the room&quot;.
      </ConfirmDialog>
    </>
  );
}
