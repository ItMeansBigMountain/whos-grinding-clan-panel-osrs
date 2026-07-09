# Who's Grinding Panel

A RuneLite external plugin for quickly seeing what friends, friends-chat players, and clan members have been grinding recently. The panel stays compact at RuneLite's default sidebar width and expands a clicked player inline to show Wise Old Man gains.

## Current behavior

- Registers as `Who's Grinding Panel` in RuneLite.
- Shows an optional login chat hint when the plugin is ready.
- Scans enabled RuneLite social sources:
  - Friends list
  - Friends chat
  - Clan chat / clan channel
- Displays players behind a compact source dropdown: `Friends Chat`, `Friends List`, `Clan Chat`, and aggregate filters.
- Keeps rows dense and aligned to the approved sidebar width: no trailing controls, no wide cards, and only a tiny right pad.
- Shows the current logged-in player at the top of the panel on every source tab so the user can see what others can see for their own account.
- Click a player row to expand/collapse a grinding-only card directly under that row.
- The expanded card fetches Wise Old Man gained data in the background for the configured period and groups results as:
  - Skills — each XP gain on its own line
  - Bosses — each KC gain on its own line
  - Activities — each score/minigame gain on its own line
- Gained values (`xp`, `kc`, `score`) are bolded; common OSRS slang labels are cleaned up, e.g. `chambers_of_xeric` -> `CoX`, `tombs_of_amascut` -> `ToA`, `theatre_of_blood` -> `ToB`, and `last_man_standing` -> `LMS`.
- If WOM cannot find useful gains, the plugin attempts to start/update tracking for the player and shows a concise wrapped fallback suggesting a longer period or trying again after XP/KC changes.

## Configuration

- `Show login hint` toggles the startup chat message.
- `Activity window (minutes)` controls the login hint summary wording.
- `Max players shown` controls login hint wording and tracking summaries.
- `Track friends list` discovers and tracks players from your friends list.
- `Track friends chat` discovers and tracks players from your active friends chat.
- `Track clan chat` discovers and tracks players from your active clan channel.
- `Show offline friends` includes offline friends in the friends-list source when enabled.
- `Max tracked members` caps the local tracking list for memory/API control.
- `Refresh interval (minutes)` controls automatic rescans while logged in and defaults to 60 minutes.
- `Gains period` controls the WOM gained window: day, 7 days, 30 days, or 365 days.
- `Enable WOM lookups` controls whether selected-player names are sent to Wise Old Man for gained summaries.
- Hidden `ignoredMembers` persistence remains for compatibility with older versions.

## Project layout

```text
src/main/java/com/itmeansbigmountain/whosgrindingclanpanel/
  WhosGrindingClanPanelPlugin.java   # RuneLite plugin entry point, social source scans, toolbar registration
  WhosGrindingClanPanelConfig.java   # RuneLite config options
  WhosGrindingClanPanelPanel.java    # Compact sidebar UI with expandable player rows
  WiseOldManGainedClient.java        # WOM gained API client and summary formatting
  SocialTrackingService.java         # tracked-member merge/cap/ignore service
src/test/java/com/itmeansbigmountain/whosgrindingclanpanel/
  *Test.java                         # JUnit coverage for formatting, dimensions, configs, tracking, and WOM summaries
runelite-plugin.properties           # Plugin Hub metadata
plugin.json                          # Local metadata descriptor
build.gradle                         # Java 11 RuneLite build
```

## Requirements

Java 11. In this workspace use:

```bash
export JAVA_HOME=/opt/data/jdks/current-java11
export PATH="$JAVA_HOME/bin:$PATH"
```

## Build and test

From the repository root:

```bash
./gradlew clean test assemble --no-daemon --console=plain
```

To launch RuneLite in developer mode with this external plugin loaded:

```bash
./gradlew run --no-daemon --console=plain
```

On Windows for this repo:

```bat
gradlew.bat run --no-daemon --console=plain
```

## Manual RuneLite testing checklist

1. Start the plugin with `gradlew.bat run --no-daemon --console=plain` on Windows or `./gradlew run --no-daemon --console=plain` on Linux.
2. Confirm RuneLite opens in developer mode and lists `Who's Grinding Panel`.
3. Log into an account and verify the optional readiness chat message appears.
4. Confirm Friends List, Friends Chat, and Clan Chat filters show appropriate members or clear empty/unsupported messages.
5. Confirm the `↻` button is visible at default sidebar width.
6. Click a player row and confirm the inline card expands; click again and confirm it collapses.
7. Confirm the logged-in player appears at the top on every source tab and can be expanded to show the user's own WOM details.
8. For a known player such as `oyama`, confirm WOM data shows skills, boss KC, and activities line-by-line with bold gained values and common acronyms (`CoX`, `ToA`, `ToB`, `LMS`, etc.) where applicable.
9. Confirm card width matches the approved top dropdown/title width, has a tiny right pad, and has no blank vertical filler.
10. Toggle `Enable WOM lookups` off and confirm the card explains lookups are disabled.
11. Switch gains period to day/7 days/30 days/365 days and confirm the card refreshes using the selected window.

## API usage notes

Wise Old Man calls are click-to-fetch and cached by player + period. The plugin does not poll every visible member every tick. If a selected player is not tracked or gained data is unavailable, it attempts to start/update WOM tracking with `POST /v2/players/{name}` and retries the gained endpoint.

Keep network calls and cache refreshes off the RuneLite game thread. Show user-visible loading/empty/failure states instead of blocking or silently failing.

## Plugin Hub prep notes

- Package: `com.itmeansbigmountain.whosgrindingclanpanel`
- Main plugin class: `WhosGrindingClanPanelPlugin`
- Display name: `Who's Grinding Panel`
- Tags: `friends`, `grind`, `skills`, `activity`, `xp`
- Before submission, add final screenshots/GIF from RuneLite after a live-account sidebar verification.
