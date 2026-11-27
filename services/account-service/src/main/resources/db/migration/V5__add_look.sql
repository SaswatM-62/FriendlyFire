-- A user's favourite look (Profile "Your look", FF-519): NULL until they pick one, and
-- then all three are set. The values are checked by the service (FavouriteLook).
ALTER TABLE account.users
    ADD COLUMN look_icon TEXT,
    ADD COLUMN look_colour TEXT,
    ADD COLUMN look_expression TEXT,
    ADD CONSTRAINT users_look_all_or_nothing CHECK (
        (look_icon IS NULL AND look_colour IS NULL AND look_expression IS NULL)
        OR (look_icon IS NOT NULL AND look_colour IS NOT NULL AND look_expression IS NOT NULL));
