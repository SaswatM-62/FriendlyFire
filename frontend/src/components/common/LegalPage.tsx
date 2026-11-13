import type { ReactNode } from "react";

export interface LegalSection {
  id: string;
  title: string;
  body: ReactNode;
}

/**
 * A long-form legal page (Terms board): contents on the left, the text in a card.
 * TODO(ff): the final Terms and Privacy Policy copy (FF-610)
 *   - the project managers write both, including the minimum age, then remove `draft`
 */
export function LegalPage({ title, updated, sections, draft }: { title: string; updated: string; sections: LegalSection[]; draft?: boolean }) {
  return (
    <div className="terms">
      <nav className="card toc" aria-label="Contents" style={{ alignSelf: "start", gap: 4 }}>
        <p className="label" style={{ padding: "0 10px 6px" }}>
          Contents
        </p>
        {sections.map((s) => (
          <a key={s.id} href={`#${s.id}`}>
            {s.title}
          </a>
        ))}
      </nav>
      <article className="card tbody">
        <div>
          {draft && (
            <p className="tag tag--yellow" style={{ marginBottom: 8 }}>
              Draft copy for layout
            </p>
          )}
          <h1 className="title">{title}</h1>
          <p className="small faint">Last updated {updated}</p>
        </div>
        {sections.map((s) => (
          <section key={s.id} id={s.id} aria-labelledby={`${s.id}-h`}>
            <h2 id={`${s.id}-h`}>{s.title}</h2>
            {s.body}
          </section>
        ))}
        <p className="muted">
          Questions? Email{" "}
          <a href="mailto:hello@friendlyfire.dev" style={{ color: "var(--cyan-text)" }}>
            hello@friendlyfire.dev
          </a>
          .
        </p>
      </article>
    </div>
  );
}
