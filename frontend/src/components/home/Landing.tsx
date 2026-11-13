"use client";

import { useSyncExternalStore } from "react";
import { useSession } from "@/components/providers/SessionProvider";
import { Av } from "@/components/ui/Character";
import { Icon } from "@/components/ui/Icon";
import type { PlayerLook } from "@/lib/api-types";
import { CHARACTER_ORDER, COLOUR_ORDER, FACE_ORDER } from "@/lib/characters";
import { pendingInvite } from "@/lib/preferences";

const noop = () => () => {};

/** Players 1 to 4 in their default looks, as on the board. */
const look = (i: number): PlayerLook => ({ icon: CHARACTER_ORDER[i], colour: COLOUR_ORDER[i], expression: FACE_ORDER[i] });

const SAMPLE: { name: string; look: PlayerLook; correct?: boolean }[] = [
  { name: "Jun", look: look(1), correct: true },
  { name: "Priya", look: look(0) },
  { name: "Theo", look: look(2) },
  { name: "Ana", look: look(3) },
];

const STEPS = [
  { n: "01", title: "Sign in", text: "With Google or an email. It takes ten seconds." },
  { n: "02", title: "Link your Riot account", text: "We read your Competitive and Unrated matches from the last 30 days." },
  { n: "03", title: "Open a room", text: "Send the code to your squad. Up to 8 of you can play." },
  { n: "04", title: "Play the quiz", text: "Lock in fast, then see who knows the squad best." },
];

/** The home page for signed-out visitors (Landing board). */
export function Landing() {
  const { signIn } = useSession();
  const invite = useSyncExternalStore(noop, pendingInvite, () => null);

  return (
    <>
      {invite && (
        <p className="invite" role="status">
          <Icon name="link" />
          <span>
            <strong>You&apos;re invited to room {invite}.</strong> Sign in to join.
          </span>
        </p>
      )}
      <section className="land">
        <div className="land__copy">
          <p className="label">Free · no ads · made for squads</p>
          <h1 className="hero">How well do you know your squad?</h1>
          <p className="lead">Trivia built from your friends&apos; VALORANT matches. Every question is about someone in the room.</p>
          <div className="row">
            <button type="button" className="btn btn--primary" onClick={signIn}>
              Sign in to play
              <Icon name="arrowRight" />
            </button>
            <a className="btn btn--ghost" href="#how">
              How it works
            </a>
          </div>
          <p className="row small faint" style={{ gap: 8 }}>
            <Icon name="shield" size={16} />
            Only people in your room see questions about you. Unlink anytime.
          </p>
        </div>
        <section className="card sample" aria-labelledby="h-sample">
          <div className="spread">
            <h2 id="h-sample" className="label">
              Sample question
            </h2>
            <span className="tag tag--mint">Revealed</span>
          </div>
          <p className="h2">Who had the most clutches in the last month?</p>
          <div className="tiles2">
            {SAMPLE.map((p) => (
              <div key={p.name} className={`t2 ${p.correct ? "t2--ok" : "t2--dim"}`}>
                <Av look={p.look} size={36} />
                <span>{p.name}</span>
                {p.correct ? (
                  <span className="tag tag--mint">
                    <Icon name="check" size={12} />
                    <span className="t2__word">Correct</span>
                  </span>
                ) : (
                  <span />
                )}
              </div>
            ))}
          </div>
          <p className="muted">Jun won 9 clutches in 34 matches. Priya and Ana called it.</p>
        </section>
      </section>
      <section id="how" className="steps" aria-label="How it works">
        {STEPS.map((step) => (
          <div key={step.n} className="step">
            <span className="num">{step.n}</span>
            <h3>{step.title}</h3>
            <p className="small muted">{step.text}</p>
          </div>
        ))}
      </section>
    </>
  );
}
