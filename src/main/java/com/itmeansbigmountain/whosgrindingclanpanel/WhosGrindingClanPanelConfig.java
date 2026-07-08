package com.itmeansbigmountain.whosgrindingclanpanel;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("whosgrindingclanpanel")
public interface WhosGrindingClanPanelConfig extends Config
{
	@ConfigItem(
		keyName = "showLoginHint",
		name = "Show login hint",
		description = "Display a chat message when the plugin is ready after login",
		position = 0
	)
	default boolean showLoginHint()
	{
		return true;
	}

	@Range(min = WhosGrindingClanPanelPlugin.MIN_ACTIVITY_WINDOW_MINUTES, max = WhosGrindingClanPanelPlugin.MAX_ACTIVITY_WINDOW_MINUTES)
	@ConfigItem(
		keyName = "activityWindowMinutes",
		name = "Activity window (minutes)",
		description = "How far back the friends activity summary should look when activity data is wired in",
		position = 1
	)
	default int activityWindowMinutes()
	{
		return WhosGrindingClanPanelPlugin.DEFAULT_ACTIVITY_WINDOW_MINUTES;
	}

	@Range(min = WhosGrindingClanPanelPlugin.MIN_PLAYERS_SHOWN, max = WhosGrindingClanPanelPlugin.MAX_PLAYERS_SHOWN)
	@ConfigItem(
		keyName = "maxPlayersShown",
		name = "Max players shown",
		description = "Maximum players to include in the grinding summary",
		position = 2
	)
	default int maxPlayersShown()
	{
		return WhosGrindingClanPanelPlugin.DEFAULT_MAX_PLAYERS_SHOWN;
	}

	@Range(min = 1, max = 30)
	@ConfigItem(
		keyName = "heatmapHistoryDays",
		name = "Heatmap history days",
		description = "Number of days future activity heatmap collection should include",
		position = 3
	)
	default int heatmapHistoryDays()
	{
		return 7;
	}

	@Range(min = 1, max = 24)
	@ConfigItem(
		keyName = "activeHourThreshold",
		name = "Active hour threshold",
		description = "Minimum number of XP gain events in an hour before that hour is marked active",
		position = 4
	)
	default int activeHourThreshold()
	{
		return 3;
	}

	@ConfigItem(
		keyName = "trackFriendsList",
		name = "Track friends list",
		description = "Discover and track players from your friends list",
		position = 5
	)
	default boolean trackFriendsList()
	{
		return true;
	}

	@ConfigItem(
		keyName = "trackFriendsChat",
		name = "Track friends chat",
		description = "Discover and track players from your active friends chat",
		position = 6
	)
	default boolean trackFriendsChat()
	{
		return true;
	}

	@ConfigItem(
		keyName = "trackClanChat",
		name = "Track clan chat",
		description = "Discover and track players from your active clan channel",
		position = 7
	)
	default boolean trackClanChat()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showOfflineFriends",
		name = "Show offline friends",
		description = "Include offline friends from the friends list in the tracked panel",
		position = 8
	)
	default boolean showOfflineFriends()
	{
		return false;
	}

	@Range(min = 10, max = 250)
	@ConfigItem(
		keyName = "maxTrackedMembers",
		name = "Max tracked members",
		description = "Caps the local social tracking list to control memory and API usage",
		position = 9
	)
	default int maxTrackedMembers()
	{
		return 100;
	}

	@Range(min = 1, max = 1440)
	@ConfigItem(
		keyName = "refreshIntervalMinutes",
		name = "Refresh interval (minutes)",
		description = "How often to automatically rescan enabled social sources while logged in",
		position = 10
	)
	default int refreshIntervalMinutes()
	{
		return 60;
	}

	@ConfigItem(
		keyName = "gainsPeriod",
		name = "Gains period",
		description = "Time window used for currently grinding summaries and player detail links",
		position = 11
	)
	default GainsPeriod gainsPeriod()
	{
		return GainsPeriod.SEVEN_DAYS;
	}

	@ConfigItem(
		keyName = "ignoredMembers",
		name = "Ignored members",
		description = "Newline-separated normalized names removed from tracking",
		position = 12,
		hidden = true
	)
	default String ignoredMembers()
	{
		return "";
	}
}
