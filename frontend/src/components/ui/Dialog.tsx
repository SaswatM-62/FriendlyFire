"use client";

import { useEffect, useId, useRef, useState, type KeyboardEvent, type ReactNode } from "react";

interface ConfirmDialogProps {
  open: boolean;
  title: string;
  children: ReactNode;
  /** Red (border and button) for actions that remove or delete something. */
  danger?: boolean;
  confirmLabel: string;
  /** The word the player must type first (Delete account: "DELETE"). */
  typeToConfirm?: string;
  busy?: boolean;
  onConfirm: () => void;
  onCancel: () => void;
}

/**
 * A confirmation on a 60% scrim (Dialogs board); destructive ones get a red border. Focus moves into it, Tab stays inside
 * it, Escape cancels, and focus goes back where it was when it closes.
 */
export function ConfirmDialog(props: ConfirmDialogProps) {
  if (!props.open) return null;
  return <OpenDialog {...props} />;
}

function OpenDialog({
  title,
  children,
  danger,
  confirmLabel,
  typeToConfirm,
  busy,
  onConfirm,
  onCancel,
}: ConfirmDialogProps) {
  const titleId = useId();
  const inputId = useId();
  const panel = useRef<HTMLDivElement>(null);
  const [typed, setTyped] = useState("");
  const confirmed = !typeToConfirm || typed.trim() === typeToConfirm;

  useEffect(() => {
    const previous = document.activeElement as HTMLElement | null;
    const first = panel.current?.querySelector<HTMLElement>("input, button");
    first?.focus();
    return () => previous?.focus();
  }, []);

  const onKeyDown = (event: KeyboardEvent) => {
    if (event.key === "Escape") {
      event.stopPropagation();
      onCancel();
      return;
    }
    if (event.key !== "Tab" || !panel.current) return;
    const focusable = [...panel.current.querySelectorAll<HTMLElement>("input, button:not([disabled])")];
    if (focusable.length === 0) return;
    const first = focusable[0];
    const last = focusable[focusable.length - 1];
    if (event.shiftKey && document.activeElement === first) {
      event.preventDefault();
      last.focus();
    } else if (!event.shiftKey && document.activeElement === last) {
      event.preventDefault();
      first.focus();
    }
  };

  return (
    <div className="modal" onKeyDown={onKeyDown}>
      <div className="modal__scrim" onClick={onCancel} />
      <div
        ref={panel}
        className={`card dlg${danger ? " dlg--danger" : ""}`}
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
      >
        <h2 id={titleId} className="h2">
          {title}
        </h2>
        <div className="muted">{children}</div>
        {typeToConfirm && (
          <label className="field" htmlFor={inputId}>
            <span className="label">Type {typeToConfirm} to confirm</span>
            <input
              id={inputId}
              className="input"
              placeholder={typeToConfirm}
              value={typed}
              autoComplete="off"
              onChange={(e) => setTyped(e.target.value)}
            />
          </label>
        )}
        <div className="btns">
          <button type="button" className="btn btn--ghost btn--sm" onClick={onCancel}>
            Cancel
          </button>
          <button
            type="button"
            className={`btn btn--sm ${danger ? "btn--red" : "btn--primary"}`}
            disabled={!confirmed || busy}
            onClick={onConfirm}
          >
            {confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
}
