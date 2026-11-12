"use client";

import Link from "next/link";
import type { ReactNode } from "react";
import { useSession } from "@/components/providers/SessionProvider";
import { Av } from "@/components/ui/Character";
import { Icon, type IconName } from "@/components/ui/Icon";
import type { PlayerLook } from "@/lib/api-types";
import { CHARACTER_ORDER, COLOUR_ORDER, FACE_ORDER } from "@/lib/characters";

/** Player N's default look, as on the board. */
const look = (i: number): PlayerLook => ({ icon: CHARACTER_ORDER[i], colour: COLOUR_ORDER[i], expression: FACE_ORDER[i] });

const SETUP: { n: string; icon: IconName; tone: string; title: string; text: string }[] = [
  { n: "01", icon: "user", tone: "cyan", title: "Sign in", text: "With Google or an email. Everyone in the room needs an account." },
  { n: "02", icon: "link", tone: "mint", title: "Link your Riot account", text: "You confirm it on Riot’s own page. We never see your password." },
  { n: "03", icon: "users", tone: "violet", title: "Open a room", text: "Send the code or invite link. Up to 8 players, with chat." },
];

const QUESTIONS = [
  "Who gets first blood most often?",
  "Who has the best headshot rate?",
  "Who plays the most late-night matches?",
  "Who tops the scoreboard least?",
];

function Phase({ n, title, text, children }: { n: number; title: string; text: string; children: ReactNode }) {
  return (
    <div className="phase">
      <div className="viz" aria-hidden="true">
        {children}
      </div>
      <h3>
        <span className="num">{n}</span>
        {title}
      </h3>
      <p className="small muted">{text}</p>
    </div>
  );
}

function Rule({ icon, tone, children }: { icon: IconName; tone: string; children: ReactNode }) {
  return (
    <li>
      <span style={{ color: `var(--${tone}-text)` }}>
        <Icon name={icon} />
      </span>
      <span className="muted">{children}</span>
    </li>
  );
}

/** /how-it-works (How it works board): setup, one question start to finish, scoring, data. */
export function HowItWorks() {
  const session = useSession();
  const signedIn = session.status === "signedIn";
  const picks = [
    { name: "Maya", colour: 2 },
    { name: "Sam", colour: 3, on: true },
    { name: "Riley", colour: 5 },
    { name: "Casey", colour: 9 },
  ];
  const podium = [
    { player: 1, place: 2, height: 46 },
    { player: 0, place: 1, height: 66 },
    { player: 2, place: 3, height: 32 },
  ];

  return (
    <div className="how" style={{ display: "flex", flexDirection: "column", flex: 1, minHeight: 0 }}>
      <header className="how__head">
        <div>
          <p className="label">How it works</p>
          <h1 className="title">Trivia about your squad, built from your own matches</h1>
        </div>
        <div className="row">
          <Link className="btn btn--line" href="/">
            Back
          </Link>
          {signedIn ? (
            <Link className="btn btn--primary" href="/">
              Start playing
              <Icon name="arrowRight" />
            </Link>
          ) : (
            <button type="button" className="btn btn--primary" onClick={session.signIn}>
              Start playing
              <Icon name="arrowRight" />
            </button>
          )}
        </div>
      </header>

      <div className="setup" role="list" aria-label="Get set up">
        {SETUP.map((step) => (
          <section key={step.n} className="card hstep" role="listitem">
            <span className="hstep__ic" style={{ color: `var(--${step.tone}-text)` }}>
              <Icon name={step.icon} size={24} />
            </span>
            <div className="hstep__tx">
              <h2>
                <span className="num">{step.n}</span>
                {step.title}
              </h2>
              <p className="small muted">{step.text}</p>
            </div>
          </section>
        ))}
      </div>

      <div className="how__grid">
        <section className="card round" aria-labelledby="h-round">
          <h2 id="h-round" className="label">
            One question, start to finish
          </h2>
          <div className="phases">
            <Phase n={1} title="Read the question" text="Every question is about someone in the room. You get 20 seconds unless the host changes it.">
              <p className="viz__q">Who had the most clutches in the last month?</p>
              <div className="timer">
                <div className="track">
                  <div className="fill" style={{ width: "65%", background: "var(--cyan)" }} />
                </div>
                <span className="num">13</span>
              </div>
            </Phase>
            <Phase n={2} title="Lock in a player" text="Pick who you think it is. Once you lock in, you can’t change it.">
              <div className="mt">
                {picks.map((p) => (
                  <span key={p.name} className={p.on ? "on" : undefined}>
                    <i style={{ background: `var(--player-${p.colour}-fill)` }} />
                    {p.name}
                  </span>
                ))}
              </div>
              <p className="small" style={{ color: "var(--orange-text)", fontWeight: 600 }}>
                Locked in · 4.1 s
              </p>
            </Phase>
            <Phase n={3} title="See the receipts" text="The answer comes with the stat behind it. The next question starts after 10 seconds.">
              <div className="vr">
                <Av look={look(0)} size={28} />
                <span className="num">+740</span>
              </div>
              <p style={{ font: "800 15px/18px var(--font-title)", color: "var(--mint-text)", whiteSpace: "nowrap" }}>Called it!</p>
              <p className="small muted">Sam won 9 of 14 clutch rounds.</p>
            </Phase>
            <Phase n={4} title="Crown a winner" text="After the last question, the podium shows who knows the squad best.">
              <div className="pod">
                {podium.map((p) => (
                  <div key={p.place}>
                    <Av look={look(p.player)} size={26} />
                    <i style={{ height: p.height, background: `var(--player-${p.player + 1}-fill)` }}>{p.place}</i>
                  </div>
                ))}
              </div>
            </Phase>
          </div>
          <div className="qs">
            <h3 className="label">Questions look like</h3>
            <ul>
              {QUESTIONS.map((q) => (
                <li key={q}>{q}</li>
              ))}
            </ul>
          </div>
        </section>

        <div className="how__side">
          <section className="card" aria-labelledby="h-score">
            <h2 id="h-score" className="label">
              Scoring
            </h2>
            <ul className="rules">
              <Rule icon="check" tone="mint">
                <b>Right answer:</b> 500 to 1,000 points. The faster you lock in, the more you get.
              </Rule>
              <Rule icon="close" tone="red">
                <b>Wrong or no answer:</b> 0 points.
              </Rule>
              <Rule icon="clock" tone="cyan">
                <b>Tie:</b> players with the same points share the place.
              </Rule>
              <Rule icon="crown" tone="yellow">
                <b>The host</b> picks 5 to 20 questions and 10 to 45 seconds each.
              </Rule>
            </ul>
          </section>
          <section className="card" aria-labelledby="h-data">
            <h2 id="h-data" className="label">
              Your data
            </h2>
            <ul className="rules">
              <Rule icon="refresh" tone="violet">
                We use your Competitive and Unrated matches from the last 30 days, and re-sync if they’re over a day old.
              </Rule>
              <Rule icon="shield" tone="mint">
                Only people in your room see questions about you.
              </Rule>
              <Rule icon="link" tone="cyan">
                Unlink your Riot account anytime from Profile.
              </Rule>
            </ul>
          </section>
          <section className="card" aria-labelledby="h-room">
            <h2 id="h-room" className="label">
              In the room
            </h2>
            <ul className="rules">
              <Rule icon="chat" tone="pink">
                Chat stays open on every screen, questions included.
              </Rule>
              <Rule icon="pause" tone="yellow">
                If someone drops, everyone votes: wait a minute for them, carry on without them, or end the quiz.
              </Rule>
            </ul>
          </section>
        </div>
      </div>
    </div>
  );
}
