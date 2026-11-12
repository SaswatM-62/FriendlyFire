/**
 * localStorage for per-browser conveniences only (theme, mute, favourite look, a
 * pending invite). Never tokens. Storage can be missing or throw (private windows,
 * blocked site data), so every call is wrapped and callers get a default instead.
 */
export function readStorage(key: string): string | null {
  try {
    return window.localStorage.getItem(key);
  } catch {
    return null;
  }
}

export function writeStorage(key: string, value: string | null): void {
  try {
    if (value === null) window.localStorage.removeItem(key);
    else window.localStorage.setItem(key, value);
  } catch {
    // Not saved: the setting lasts until the page is closed.
  }
}

export function readJson<T>(key: string, isValid: (value: unknown) => value is T): T | null {
  const raw = readStorage(key);
  if (raw === null) return null;
  try {
    const value: unknown = JSON.parse(raw);
    return isValid(value) ? value : null;
  } catch {
    return null;
  }
}
