# Who's Grinding Clan Panel

A merged RuneLite external plugin scaffold for clan activity: it combines the original **Who's Grinding Clan Panel** concept with the **Clan Grind Heatmap** model helpers.

The target product is one clan-focused side panel instead of two separate plugin-hub submissions:

- **Who's grinding now/recently:** summarize clanmates with recent XP/KC/activity gains.
- **When the clan grinds:** bucket recent clan XP events by UTC hour so clan leaders can spot peak grind windows and plan events.

The current implementation is intentionally lightweight and plugin-hub-prep friendly: it registers clean RuneLite metadata, exposes configuration options, includes reusable formatting helpers, and includes pure-Java heatmap bucketing logic ready for a future visible panel.

## Current behavior

- Registers as `Who's Grinding Clan Panel` in RuneLite.
- Shows an optional login chat hint when the plugin is ready.
- Provides configuration for the social activity tracker:
  - `Show login hint` toggles the startup chat message.
  - `Activity window (minutes)` controls how far back the activity summary should look.
  - `Max players shown` controls how many members should be displayed in short summaries.
  - `Heatmap history days` controls the heatmap collection window.
  - `Active hour threshold` controls when an hour is considered fully active.
  - `Track friends list`, `Track clan members`, and `Track friends chat` enable the three social source views.
  - `Max tracked members` caps the local tracking list for memory/API control.
  - `Refresh interval (minutes)` controls automatic rescans while logged in and defaults to 60 minutes.
  - Hidden `ignoredMembers` persistence keeps manually removed members from being re-added immediately.
- Adds a RuneLite sidebar navigation button with a state-driven social tracking panel.
- The side panel starts empty until RuneLite exposes social lists, renders tracked members behind three top tabs (`Friends Chat`, `Clan Chat`, `Friends List`), and includes a compact top-right rescan icon, one-by-one remove buttons, local status messages, and a 24-hour UTC heatmap from tracked update times.
- Includes lightweight Java utility tests for message formatting, bounds fallback, player-name normalization, social tracking merge/remove/cap behavior, and heatmap intensity/bucketing.

## Merged repo decision

`ClanGrindHeatmap` has been folded into this repo because it is best treated as a view/model inside the clan activity panel, not as a separate plugin. The old heatmap repo should be considered archival unless we later decide to submit it independently.

Original source folded in:

```text
../ClanGrindHeatmap/src/main/java/com/itmeansbigmountain/clangrindheatmap/ClanGrindHeatmapModel.java
```

Merged target path:

```text
src/main/java/com/itmeansbigmountain/whosgrindingclanpanel/ClanGrindHeatmapModel.java
```

## Project layout

```text
src/main/java/com/itmeansbigmountain/whosgrindingclanpanel/
  WhosGrindingClanPanelPlugin.java   # RuneLite plugin entry point and toolbar registration
  WhosGrindingClanPanelConfig.java   # RuneLite config options for panel + heatmap settings
  WhosGrindingClanPanelPanel.java    # Sidebar UI scaffold for recent grinders, heatmap, and data status
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
2. Confirm RuneLite opens in developer mode and lists `Who's Grinding Clan Panel`.
3. Log into an account and verify the optional readiness chat message appears.
4. Toggle `Show login hint` off and confirm the login hint no longer appears.
5. Change activity and heatmap config values, then confirm the plugin remains stable across logout/login.
6. Keep notes/screenshots for future Plugin Hub submission once the visible clan activity side panel is implemented.

## API usage notes

No external API calls are wired in yet. The plugin currently avoids live Wise Old Man, TempleOSRS, or RuneLite clan-channel data dependencies so it can compile and test without a live client session.

Future panel work should:

- Read the logged-in player's friends list, clan roster/member list, and friends chat through RuneLite social APIs where available.
- Keep improving source scanner event coverage for joins/leaves/status changes while preserving the same `SocialTrackingService` state boundary.
- Pull clan/player gain data from Wise Old Man and TempleOSRS in the background, with local caching and partial-data states.
- Keep friends list, clan, and friends chat source tags separate even when the same player appears in multiple places.
- Keep network calls and cache refreshes off the RuneLite game thread.
- Show user-visible stale/partial/failure states instead of blocking or silently failing.

## Plugin Hub prep notes

- Package: `com.itmeansbigmountain.whosgrindingclanpanel`
- Main plugin class: `WhosGrindingClanPanelPlugin`
- Display name: `Who's Grinding Clan Panel`
- Tags: `clan`, `grind`, `skills`, `activity`, `heatmap`, `xp`

Before plugin-hub submission, add screenshots or a short GIF after the real clan activity panel/heatmap UI is implemented and manually verified in RuneLite.
