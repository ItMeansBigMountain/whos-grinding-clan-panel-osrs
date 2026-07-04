package com.itmeansbigmountain.whosgrindingclanpanel;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = WhosGrindingClanPanelPlugin.PLUGIN_NAME,
	description = "Shows recent clan grinders and heatmaps clan XP activity windows for event planning.",
	tags = {"clan", "grind", "skills", "activity", "heatmap", "xp"}
)
public class WhosGrindingClanPanelPlugin extends Plugin
{
	static final String PLUGIN_NAME = "Who's Grinding Clan Panel";
	static final int DEFAULT_ACTIVITY_WINDOW_MINUTES = 30;
	static final int DEFAULT_MAX_PLAYERS_SHOWN = 8;
	static final int MIN_ACTIVITY_WINDOW_MINUTES = 5;
	static final int MAX_ACTIVITY_WINDOW_MINUTES = 240;
	static final int MIN_PLAYERS_SHOWN = 1;
	static final int MAX_PLAYERS_SHOWN = 25;
	static final int DEFAULT_HEATMAP_HISTORY_DAYS = 7;
	static final int DEFAULT_ACTIVE_HOUR_THRESHOLD = 3;

	@Inject
	private Client client;

	@Inject
	private WhosGrindingClanPanelConfig config;

	@Override
	protected void startUp()
	{
		log.debug("{} started", PLUGIN_NAME);
	}

	@Override
	protected void shutDown()
	{
		log.debug("{} stopped", PLUGIN_NAME);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() != GameState.LOGGED_IN || !config.showLoginHint())
		{
			return;
		}

		client.addChatMessage(
			ChatMessageType.GAMEMESSAGE,
			"",
			buildLoginHint(config.activityWindowMinutes(), config.maxPlayersShown()),
			null
		);
	}

	static String buildLoginHint(int activityWindowMinutes, int maxPlayersShown)
	{
		int safeWindow = clamp(
			activityWindowMinutes,
			MIN_ACTIVITY_WINDOW_MINUTES,
			MAX_ACTIVITY_WINDOW_MINUTES,
			DEFAULT_ACTIVITY_WINDOW_MINUTES
		);
		int safeMaxPlayers = clamp(
			maxPlayersShown,
			MIN_PLAYERS_SHOWN,
			MAX_PLAYERS_SHOWN,
			DEFAULT_MAX_PLAYERS_SHOWN
		);

		return PLUGIN_NAME + " is ready. It will highlight up to "
			+ safeMaxPlayers
			+ " clanmates with gains in the last "
			+ safeWindow
			+ " minutes and prepare a "
			+ DEFAULT_HEATMAP_HISTORY_DAYS
			+ " day grind heatmap once clan activity data is available.";
	}

	static String formatActivityLine(String playerName, String activityLabel, long gainedXp)
	{
		String safeName = normalizePlayerName(playerName);
		String safeActivity = normalizeActivityLabel(activityLabel);
		long safeXp = Math.max(0, gainedXp);

		return safeName + " - " + safeActivity + " (" + safeXp + " xp gained)";
	}

	static String normalizePlayerName(String playerName)
	{
		if (playerName == null || playerName.trim().isEmpty())
		{
			return "Unknown clanmate";
		}

		return playerName
			.replace('\u00A0', ' ')
			.trim()
			.replaceAll("\\s+", " ");
	}

	private static String normalizeActivityLabel(String activityLabel)
	{
		if (activityLabel == null || activityLabel.trim().isEmpty())
		{
			return "training";
		}

		return activityLabel.trim().replaceAll("\\s+", " ");
	}

	private static int clamp(int value, int min, int max, int fallback)
	{
		if (value < min || value > max)
		{
			return fallback;
		}

		return value;
	}

	@Provides
	WhosGrindingClanPanelConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(WhosGrindingClanPanelConfig.class);
	}
}
