package com.itmeansbigmountain.whosgrindingclanpanel;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("whosgrindingclanpanel")
public interface WhosGrindingClanPanelConfig extends Config
{
	@ConfigItem(
		keyName = "showLoginHint",
		name = "Show login hint",
		description = "Display a chat message when the plugin is ready after login"
	)
	default boolean showLoginHint()
	{
		return true;
	}

	@ConfigItem(
		keyName = "activityWindowMinutes",
		name = "Activity window (minutes)",
		description = "How far back the clan grinding summary should look when activity data is wired in"
	)
	default int activityWindowMinutes()
	{
		return WhosGrindingClanPanelPlugin.DEFAULT_ACTIVITY_WINDOW_MINUTES;
	}

	@ConfigItem(
		keyName = "maxPlayersShown",
		name = "Max players shown",
		description = "Maximum clanmates to include in the grinding summary"
	)
	default int maxPlayersShown()
	{
		return WhosGrindingClanPanelPlugin.DEFAULT_MAX_PLAYERS_SHOWN;
	}
}
