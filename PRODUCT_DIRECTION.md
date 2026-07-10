# Who's Grinding Panel — Product Direction

## Vision

Build a compact RuneLite side-panel plugin that watches social lists and shows what people have been grinding recently without wasting sidebar space.

The plugin tracks members from three configurable sources:

1. **Friends list** — people on the player's OSRS friends list.
2. **Friends chat** — people currently present in a friends chat channel.
3. **Clan chat / clan channel** — members from the logged-in player's clan context.

Once a person is discovered from an enabled source, the plugin keeps a compact local tracking record and can enrich that player on click with Wise Old Man gained data plus official OSRS hiscores local-snapshot deltas.

## Current shipped direction

The current product slice is no longer just a scaffold. It includes:

- Real RuneLite social scanning for friends list, friends chat, and clan chat where the client exposes those sources.
- Config toggles for those sources.
- Compact source-filtered sidebar rows.
- Inline expandable/collapsible player cards.
- A current-player row pinned at the top across all source tabs, so the logged-in user can inspect their own public WOM grinding view.
- Wise Old Man click-to-fetch gained summaries for the selected gains period.
- Official OSRS hiscores local-snapshot tracking shown as its own section for skills, boss KC, and activity score deltas.
- WOM start/update fallback for untracked players.
- Grinding-only card details: skills XP, boss KC, and activity/minigame scores.
- One stat per line in the expanded card.
- Bold gained values (`xp`, `kc`, `score`).
- Approved narrow sidebar width standard matching the top dropdown/title block, with a tiny right pad and no large gutters.

## Core user loop

1. Player opens RuneLite and enables the plugin.
2. Plugin scans enabled social sources: friends list, friends chat, and/or clan chat.
3. Plugin shows discovered members in a compact sidebar list.
4. Player clicks a row.
5. The row expands inline and loads recent WOM gained data plus official hiscores tracked deltas for the configured period.
6. Player sees what that person has been grinding: skills, bosses, and activities/minigames, separated by data source.
7. Player clicks the row again to collapse it.

## UI standards

- Fit RuneLite's default sidebar width.
- Align content to the approved top dropdown/title width.
- Keep rows/cards compact and content-sized.
- No trailing text/buttons.
- No blank vertical filler.
- No large right gutter; preserve only a tiny right safety pad.
- Avoid giant HTML blobs for complex cards. Use individual left-aligned rows.
- Render every card item line-by-line:
  - each skill XP gain
  - each boss KC gain
  - each activity/minigame score gain
- Use friendly labels for common WOM keys:
  - `chambers_of_xeric` -> `CoX`
  - `tombs_of_amascut` -> `ToA`
  - `theatre_of_blood` -> `ToB`
  - `last_man_standing` -> `LMS`
  - `bounty_hunter_hunter` -> `Bounty Hunter`
  - `bounty_hunter_rogue` -> `Bounty Hunter Rogue`

## External API policy

- Do not poll every visible member every tick.
- Use click-to-fetch for selected players.
- Cache by player + gains period.
- Keep network calls off the RuneLite game thread.
- Expose config disclosure: selected player names are sent to Wise Old Man when WOM lookups are enabled.
- If WOM cannot find gains, start/update tracking via `POST /v2/players/{name}` and retry. Always track official OSRS hiscores for inspected players as a separate source. Official hiscores only expose current totals, so first use may only save a baseline; later checks can show skill XP, boss KC, and activity/minigame score deltas for the selected period. If the baseline is too recent for the selected period, show best-available gains clearly labeled as partial.

## Remaining polish before Plugin Hub submission

- Final live RuneLite screenshot/GIF on the target Windows machine.
- Confirm card spacing with a real client render, not just generated mocks.
- Confirm empty/no-gain states are readable in the actual sidebar.
- Decide whether to keep legacy ignored-member hidden config compatibility or remove it in a breaking-cleanup pass.
- Review Plugin Hub rules for third-party network disclosures before submission.
