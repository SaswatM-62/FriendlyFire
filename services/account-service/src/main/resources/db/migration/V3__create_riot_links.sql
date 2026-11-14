CREATE TABLE account.riot_links (
    user_id UUID PRIMARY KEY REFERENCES account.users (user_id) ON DELETE CASCADE,
    puuid TEXT NOT NULL UNIQUE,
    game_name TEXT NOT NULL,
    tag_line TEXT NOT NULL,
    region TEXT NOT NULL,
    linked_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
