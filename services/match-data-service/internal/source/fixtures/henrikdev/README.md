# HenrikDev fixtures

`matches.json.gz` is one HenrikDev v4 response (`{"status": 200, "data": [...]}`) with 20
real Competitive matches from patches 13.08–13.10, **anonymized**:

- every PUUID, name, tag, party ID, match ID and cosmetic ID is made up;
- the 5-stack that played them is the mock RSO's example test players: Sam#NA1,
  Alex#EUW, Kim#KR1, Jordan#TEST and Riley#TEST (PUUIDs `00000000-0000-4000-8000-00000000000N`);
- everyone else is "Player N" with tag FAKE;
- stats, rounds, kills, positions and game data (agents, maps, weapons) are unchanged.

They were made from a fetch with the team's HenrikDev key by a script that checked no
original name or ID was left. Never add a raw HenrikDev response here: real players'
data stays out of the repository.

The offline mode (`MATCH_SOURCE=fixtures`, `FixtureSource`) and the tests use them; tests
never call the real API. `rate_limited.json` is HenrikDev's 429 body.
