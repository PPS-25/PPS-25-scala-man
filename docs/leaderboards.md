# Leaderboards

## Scope

Each leaderboard belongs to one map and one game category: Classic, Timed, or Survival. Results
from different categories are never combined, even when they were achieved on the same map.

## Ranking

All categories rank completed matches by score in descending order. Ties use the existing result
ordering: the earlier result comes first, followed by player name. This keeps the ranking criterion
consistent with the score tracked and persisted by the domain model.

## UI and storage

The menu selection of map and game mode is also the leaderboard filter. Opening the leaderboard
shows only that map/category pair and identifies both in its title.

Classic leaderboards retain their existing `<map>.csv` storage file. Timed and Survival use
`<map>-timed.csv` and `<map>-survival.csv`, respectively.
