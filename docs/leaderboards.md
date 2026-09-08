# Leaderboards

## Scope

Each leaderboard belongs to one map and one game category: Classic, Timed, or Survival. Results
from different categories are never combined, even when they were achieved on the same map.

## Ranking

All categories rank completed matches by score in descending order. Ties use the existing result
ordering: the earlier result comes first, followed by player name. This keeps the ranking criterion
consistent with the score tracked and persisted by the domain model.

## UI and storage

The leaderboard opens on the map and mode selected in the menu. Its own map and mode selectors
allow switching to any other map/category pair without closing the window.

Classic leaderboards retain their existing `<map>.csv` storage file. Timed and Survival use
`<map>-timed.csv` and `<map>-survival.csv`, respectively.
