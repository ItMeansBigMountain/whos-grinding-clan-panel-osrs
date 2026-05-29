# Who's Grinding Clan Panel

A RuneLite external plugin scaffold for showing which clanmates are currently grinding. The current implementation is intentionally lightweight and plugin-hub-prep friendly: it registers clean RuneLite metadata, exposes the first configuration options, and provides reusable formatting helpers for the future clan activity panel.

## Current behavior

- Registers as `Who's Grinding Clan Panel` in RuneLite.
- Shows an optional login chat hint when the plugin is ready.
- Provides configuration for the intended clan activity summary:
  - `Show login hint` toggles the startup chat message.
  - `Activity window (minutes)` controls how far back a future clan activity summary should look.
  - `Max players shown` controls how many clanmates should be displayed in the summary.
- Includes lightweight Java utility tests for message formatting, bounds fallback, and player-name normalization.

## Project layout

```text
src/main/java/com/itmeansbigmountain/whosgrindingclanpanel/
  WhosGrindingClanPanelPlugin.java   # RuneLite plugin entry point and formatting helpers
  WhosGrindingClanPanelConfig.java   # RuneLite config options
src/test/java/com/itmeansbigmountain/whosgrindingclanpanel/
  WhosGrindingClanPanelPluginTest.java        # RuneLite developer-mode launcher
  WhosGrindingClanPanelPluginUtilityTest.java # JUnit smoke tests
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
5. Change `Activity window (minutes)` and `Max players shown`, then confirm the plugin remains stable across logout/login.

## API usage notes

No external API calls are wired in yet. The plugin currently avoids live Wise Old Man, TempleOSRS, or RuneLite clan-channel data dependencies so it can compile and test without a live client session. Future panel work should use background execution and caching for any network calls, then keep the RuneLite game thread limited to UI updates.

## Plugin Hub prep notes

- Package: `com.itmeansbigmountain.whosgrindingclanpanel`
- Main plugin class: `WhosGrindingClanPanelPlugin`
- Display name: `Who's Grinding Clan Panel`
- Tags: `clan`, `grind`, `skills`, `activity`

Before plugin-hub submission, add screenshots or a short GIF after the real clan activity panel is implemented and manually verified in RuneLite.
