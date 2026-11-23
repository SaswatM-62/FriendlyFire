"use client";

import { createContext, useCallback, useContext, useMemo, useRef, useState, type ReactNode } from "react";
import { Icon, type IconName } from "@/components/ui/Icon";

export type ToastTone = "mint" | "yellow" | "red" | "cyan" | "violet" | "pink";

interface Toast {
  id: number;
  tone: ToastTone;
  message: ReactNode;
}

const ICONS: Record<ToastTone, IconName> = {
  mint: "check",
  yellow: "crown",
  red: "alert",
  cyan: "copy",
  violet: "users",
  pink: "users",
};

type ShowToast = (message: ReactNode, tone?: ToastTone) => void;

const ToastContext = createContext<ShowToast>(() => {});

/** Shows a toast for 4 seconds: top right on desktop, bottom on phones. */
export function useToast(): ShowToast {
  return useContext(ToastContext);
}

const SHOW_FOR_MS = 4_000;

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);
  const nextId = useRef(1);

  const dismiss = useCallback((id: number) => setToasts((all) => all.filter((t) => t.id !== id)), []);

  const show = useCallback<ShowToast>(
    (message, tone = "cyan") => {
      const id = nextId.current++;
      setToasts((all) => [...all.slice(-3), { id, tone, message }]);
      setTimeout(() => dismiss(id), SHOW_FOR_MS);
    },
    [dismiss],
  );

  const value = useMemo(() => show, [show]);

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div className="toasts" role="status" aria-live="polite">
        {toasts.map((toast) => (
          <div key={toast.id} className="toast">
            <span style={{ color: `var(--${toast.tone}-text)`, display: "flex" }}>
              <Icon name={ICONS[toast.tone]} />
            </span>
            <span>{toast.message}</span>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}
