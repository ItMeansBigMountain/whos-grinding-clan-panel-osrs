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
		description = "Time window used for Wise Old Man and official hiscores local-snapshot gained summaries",
		position = 11
	)
	default GainsPeriod gainsPeriod()
	{
		return GainsPeriod.SEVEN_DAYS;
	}

	@ConfigItem(
		keyName = "gainDataSource",
		name = "Gain data source",
		description = "Choose tracker API gains, official hiscores local deltas, or both side-by-side for development comparison",
		position = 12
	)
	default GainDataSource gainDataSource()
	{
		return GainDataSource.TRACKER_APIS;
	}

	@ConfigItem(
		keyName = "enableWiseOldManLookups",
		name = "Enable WOM lookups",
		description = "Loads selected-player gained summaries from Wise Old Man. This sends the selected player name to wiseoldman.net.",
		position = 13
	)
	default boolean enableWiseOldManLookups()
	{
		return true;
	}

	@ConfigItem(
		keyName = "ignoredMembers",
		name = "Ignored members",
		description = "Newline-separated normalized names removed from tracking",
		position = 14,
		hidden = true
	)
	default String ignoredMembers()
	{
		return "";
	}
}
