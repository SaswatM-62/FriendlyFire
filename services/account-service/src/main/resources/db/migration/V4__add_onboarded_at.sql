-- When a user finished (or skipped the end of) the new-player tutorial; NULL until then.
ALTER TABLE account.users ADD COLUMN onboarded_at TIMESTAMPTZ;

-- Accounts made before the tutorial existed don't see it.
UPDATE account.users SET onboarded_at = created_at;
