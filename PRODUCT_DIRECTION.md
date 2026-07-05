# Who's Grinding Clan Panel — Product Direction

## Vision

Build a RuneLite side-panel plugin that watches social lists over time and turns them into a lightweight clan/friend activity tracker.

The plugin should be able to track members from three configurable sources:

1. **Friends list** — people on the player's RuneLite/OSRS friends list.
2. **Clan chat / clan members** — members from the logged-in player's clan context.
3. **Friends chat** — people currently present in a friends chat channel.

The player can enable any combination of these views. Once a person is discovered from an enabled source, the plugin starts tracking that member from that point forward and updates their activity/status as new information arrives.

## Core user loop

1. Player opens RuneLite and enables the plugin.
2. Plugin scans enabled social sources: friends list, clan, and/or friends chat.
3. Plugin adds discovered members to a local tracking list.
4. Plugin observes online/offline and activity signals over time.
5. Plugin shows a side panel with who is online, recently active, and what they appear to be doing.
6. Player can remove individual people from tracking to keep the list small and avoid unnecessary memory/API usage.
7. Plugin continues tracking remaining members across the session and, eventually, across restarts via local config/cache.

## MVP behavior

The first real product slice should focus on local tracking and status management before external XP/KC APIs.

### MVP source support

- Friends list: enabled/disabled by config.
- Clan members/chat: enabled/disabled by config.
- Friends chat: enabled/disabled by config.

If a source cannot be read from RuneLite APIs yet, the panel should show a clear unsupported/empty state instead of silently failing.

### MVP member tracking

Each tracked member should have:

- normalized display name
- source tags: `FRIEND`, `CLAN`, `FRIENDS_CHAT`
- tracking status: tracked / ignored / removed
- online status when known
- first seen timestamp
- last seen timestamp
- last status change timestamp
- last observed world if available
- last observed activity summary if available

### MVP panel actions

- View selector / source filter: All, Friends, Clan, Friends Chat.
- Remove/untrack button per member.
- Optional clear removed/ignored list later.
- Refresh/rescan button.
- Status labels for loading, empty, unsupported, partial, and stale data.

### Memory/resource control

The plugin should not blindly grow forever.

Rules:

- Only track members discovered from enabled sources.
- Let the user remove individual members from the tracking list.
- Keep removed members ignored until the user re-adds or clears ignored state.
- Cap tracked members with a config option, e.g. 50/100/200.
- Avoid repeated API calls for removed/ignored members.
- Store compact records only; avoid keeping full event histories in memory.

## Future activity intelligence

After local source tracking works, add richer activity updates:

1. **RuneLite-observed status**
   - online/offline
   - world changes if available
   - clan/friends chat join/leave signals

2. **External progress APIs**
   - Wise Old Man for XP/boss gain deltas
   - TempleOSRS as optional fallback or comparison source
   - background refresh with local caching

3. **Activity summaries**
   - recent skill XP gains
   - boss KC gains
   - likely current grind
   - idle/stale detection

4. **Clan heatmap**
   - bucket observed updates by UTC hour
   - show peak social/activity windows
   - use real observed member events instead of placeholder counts

## Proposed implementation phases

### Phase 1 — State model and panel rendering

Replace hardcoded sample rows with real in-memory state classes:

- `TrackedMember`
- `TrackedMemberSource`
- `TrackedMemberStatus`
- `SocialTrackerState`
- `ClanActivityState` or similar aggregate model

Panel renders state instead of static sample data.

### Phase 2 — Tracking service

Add a service that owns the tracked-member map:

- normalize names
- merge the same player across multiple sources
- add/update discovered members
- remove/untrack a member
- cap list size
- expose immutable snapshots to the panel

Possible class names:

- `SocialTrackingService`
- `TrackedMemberRepository`
- `SocialSourceScanner`

### Phase 3 — RuneLite source scanners

Wire each source independently:

- `FriendsListScanner`
- `ClanMemberScanner`
- `FriendsChatScanner`

Each scanner should fail gracefully if its API is unavailable or the player is logged out.

### Phase 4 — Persistence

Persist compact tracking preferences/state:

- removed/ignored names
- enabled sources
- max tracked members
- maybe tracked names and first-seen timestamps

Avoid persisting large event histories in config. Use compact config strings or a small local cache if RuneLite permits it.

### Phase 5 — External activity enrichment

Add background API refresh after local tracking is stable:

- refresh only tracked members
- skip removed/ignored members
- debounce/batch requests
- show stale/failure states
- never block the RuneLite client thread

## Current repo gap against this vision

The repo currently has a clean scaffold, sidebar, config, sample rows, and heatmap helper tests. Missing pieces are:

- real tracked-member model
- tracking service
- source toggles for friends/clan/friends-chat views
- RuneLite scanners for those sources
- remove/untrack member UI
- local persistence of ignored/removed members
- live online/offline updates
- external XP/KC enrichment
- real heatmap inputs

## Product principle

Start with reliable local social tracking first. External XP/KC intelligence should enrich the tracker later, not be required for the plugin to feel useful.
