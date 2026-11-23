-- Linked players and their sync status (FF-207). external_player_id is the PUUID: an
-- internal join key that never leaves the service.
CREATE TABLE players (
    user_id            text PRIMARY KEY,
    game               text NOT NULL,
    external_player_id text NOT NULL,
    region             text NOT NULL,
    -- IN_PROGRESS, COMPLETE or FAILED
    status             text NOT NULL,
    error_message      text NOT NULL DEFAULT '',
    last_synced_at     timestamptz,
    -- HenrikDev requests the last finished sync took, for estimates.
    last_sync_requests integer,
    updated_at         timestamptz NOT NULL
);
CREATE INDEX players_external ON players (game, external_player_id);

-- A match is stored once, however many linked players played it.
CREATE TABLE matches (
    game      text NOT NULL,
    match_id  text NOT NULL,
    played_at timestamptz NOT NULL,
    PRIMARY KEY (game, match_id)
);

-- One linked player's stats in one match (PlayerMatchStats). Other players are never stored.
CREATE TABLE player_match_stats (
    user_id   text NOT NULL REFERENCES players ON DELETE CASCADE,
    game      text NOT NULL,
    match_id  text NOT NULL,
    played_at timestamptz NOT NULL,
    won       boolean NOT NULL,
    kills     integer NOT NULL,
    deaths    integer NOT NULL,
    assists   integer NOT NULL,
    labels    jsonb NOT NULL,
    numbers   jsonb NOT NULL,
    PRIMARY KEY (user_id, game, match_id),
    FOREIGN KEY (game, match_id) REFERENCES matches
);
CREATE INDEX player_match_stats_newest ON player_match_stats (user_id, played_at DESC);
CREATE INDEX player_match_stats_match ON player_match_stats (game, match_id);
