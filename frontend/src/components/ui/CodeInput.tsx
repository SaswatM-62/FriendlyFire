"use client";

import { normalizeRoomCode } from "@/lib/api";

interface CodeInputProps {
  value: string;
  onChange: (code: string) => void;
  error?: boolean;
  /** Describes the input, e.g. the id of an error message. */
  describedBy?: string;
}

/** The room code field: pasting "k7qm2x" or "K7QM-2X" gives K7QM2X. */
export function CodeInput({ value, onChange, error, describedBy }: CodeInputProps) {
  return (
    <label className="field" style={{ flex: 1 }}>
      <span className="sr">Room code</span>
      <input
        className="codein"
        placeholder="K7QM2X"
        aria-invalid={error || undefined}
        aria-describedby={describedBy}
        inputMode="text"
        autoComplete="off"
        autoCapitalize="characters"
        spellCheck={false}
        maxLength={12}
        value={value}
        onChange={(e) => onChange(normalizeRoomCode(e.target.value))}
      />
    </label>
  );
}
