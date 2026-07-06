# Who's Grinding Panel

A RuneLite external plugin for quickly seeing which friends and friends-chat players are online, recently seen, or worth checking in on from a compact sidebar panel.

The product direction is a general social/activity panel, not a clan-only tool:

- **Who's grinding now/recently:** summarize friends and friends-chat members with online/offline state, world, and later XP/KC/activity gains.
- **At-a-glance status:** use compact icons and short summaries so the RuneLite sidebar stays readable at default width.
- **Click-to-detail tracking:** clicking a player shows tracker-style details locally, with future enrichment from hiscores, Wise Old Man, TempleOSRS, and account-intel patterns from SmartHiscoreLookup.

The current implementation is intentionally lightweight and plugin-hub-prep friendly: it registers clean RuneLite metadata, exposes configuration options, includes reusable formatting helpers, uses live RuneLite friends/friends-chat scanners where available, and includes pure-Java heatmap bucketing logic ready for future activity enrichment.

## Current behavior

- Registers as `Who's Grinding Panel` in RuneLite.
- Shows an optional login chat hint when the plugin is ready.
- Provides configuration for the social activity tracker:
  - `Show login hint` toggles the startup chat message.
  - `Activity window (minutes)` controls how far back the activity summary should look.
  - `Max players shown` controls how many players should be displayed in short summaries.
  - `Heatmap history days` controls the activity heatmap collection window.
  - `Active hour threshold` controls when an hour is considered fully active.
  - `Track friends list` discovers and tracks players from your friends list.
  - `Track friends chat` discovers and tracks players from your active friends chat.
  - `Show offline friends` includes offline friends in the friends-list source when enabled.
  - `Max tracked members` caps the local tracking list for memory/API control.
  - `Refresh interval (minutes)` controls automatic rescans while logged in and defaults to 60 minutes.
  - Hidden `ignoredMembers` persistence keeps manually removed members from being re-added immediately.
- Adds a RuneLite sidebar navigation button with a state-driven social tracking panel.
- The side panel starts empty until RuneLite exposes social lists, renders tracked members behind a compact source dropdown (`Friends Chat`, `Friends List`), and includes a visible top-right rescan icon, one-by-one remove buttons, local status messages, compact activity icons, click-to-detail tracker dialogs, and a 24-hour UTC activity heatmap from tracked update times.
- Includes lightweight Java utility tests for message formatting, bounds fallback, player-name normalization, social tracking merge/remove/cap behavior, and heatmap intensity/bucketing.

## Project layout

```text
src/main/java/com/itmeansbigmountain/whosgrindingclanpanel/
  WhosGrindingClanPanelPlugin.java   # RuneLite plugin entry point and toolbar registration
  WhosGrindingClanPanelConfig.java   # RuneLite config options for panel + heatmap settings
  WhosGrindingClanPanelPanel.java    # Sidebar UI scaffold for recent grinders, heatmap, and detail dialogs
  ClanGrindHeatmapModel.java         # Pure Java heatmap bucketing/intensity helpers
src/test/java/com/itmeansbigmountain/whosgrindingclanpanel/
  WhosGrindingClanPanelPluginTest.java        # RuneLite developer-mode launcher
  WhosGrindingClanPanelPluginUtilityTest.java # JUnit smoke tests for panel helpers
  ClanGrindHeatmapModelTest.java              # JUnit smoke tests for heatmap helpers
runelite-plugin.properties           # Plugin Hub metadata
plugin.json                          # Local metadata descriptor
build.gradle                         # Java 11 RuneLite build
```

## Requirements

- Java 11. In this workspace use:

```bash
export JAVA_HOME=/opt/data/jdks/current-java11
export PATH="$JAVA_HOME/bin:$PATH"
```

## Build and test

From the repository root:

```bash
./gradlew test --no-daemon -q
./gradlew assemble --no-daemon -q
```

To launch RuneLite in developer mode with this external plugin loaded:

```bash
./gradlew run --no-daemon
```

## Manual RuneLite testing checklist

1. Start the plugin with `./gradlew run --no-daemon`.
2. Confirm RuneLite opens in developer mode and lists `Who's Grinding Panel`.
3. Log into an account and verify the optional readiness chat message appears.
4. Toggle `Show login hint` off and confirm the login hint no longer appears.
5. Toggle `Show offline friends` on/off and confirm offline friends appear/disappear in the friends-list source.
6. Confirm the source dropdown only shows `Friends Chat` and `Friends List`.
7. Confirm the `↻` button is visible at default sidebar width.
8. Click a player row and confirm tracker details open.
9. Change activity and heatmap config values, then confirm the plugin remains stable across logout/login.
10. Keep notes/screenshots for future Plugin Hub submission once hiscore/activity enrichment is implemented.

## API usage notes

No external API calls are wired in yet. The plugin currently avoids live Wise Old Man or TempleOSRS network dependencies so it can compile and test without a live client session.

Current RuneLite source scanners:

- Friends list via `client.getFriendContainer()`.
- Friends chat via `client.getFriendsChatManager()`.

Future panel work should:

- Improve source scanner event coverage for joins/leaves/status changes while preserving the same `SocialTrackingService` state boundary.
- Pull player gain data from Wise Old Man and TempleOSRS in the background, with local caching and partial-data states.
- Keep friends list and friends chat source tags separate even when the same player appears in both places.
- Keep network calls and cache refreshes off the RuneLite game thread.
- Show user-visible stale/partial/failure states instead of blocking or silently failing.
- Reuse account-intel UX ideas from SmartHiscoreLookup without creating a runtime dependency.

## Plugin Hub prep notes

- Package: `com.itmeansbigmountain.whosgrindingclanpanel`
- Main plugin class: `WhosGrindingClanPanelPlugin`
- Display name: `Who's Grinding Panel`
- Tags: `friends`, `grind`, `skills`, `activity`, `xp`

Before plugin-hub submission, add screenshots or a short GIF after the activity panel is manually verified in RuneLite.
