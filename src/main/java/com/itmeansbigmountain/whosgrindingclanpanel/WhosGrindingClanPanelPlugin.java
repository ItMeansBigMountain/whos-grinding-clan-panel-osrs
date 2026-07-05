package com.itmeansbigmountain.whosgrindingclanpanel;

import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

@Slf4j
@PluginDescriptor(
	name = WhosGrindingClanPanelPlugin.PLUGIN_NAME,
	description = "Tracks friends, clan, and friends chat members for live social activity updates.",
	tags = {"clan", "friends", "grind", "activity", "heatmap", "xp"}
)
public class WhosGrindingClanPanelPlugin extends Plugin
{
	static final String PLUGIN_NAME = "Who's Grinding Clan Panel";
	static final String CONFIG_GROUP = "whosgrindingclanpanel";
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

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ConfigManager configManager;

	private WhosGrindingClanPanelPanel panel;
	private NavigationButton navButton;
	private SocialTrackingService trackingService;

	@Override
	protected void startUp()
	{
		trackingService = new SocialTrackingService();
		trackingService.loadIgnoredMembers(config.ignoredMembers());
		rescanSocialSources();

		panel = new WhosGrindingClanPanelPanel(config, trackingService.snapshot(config.maxTrackedMembers()), new WhosGrindingClanPanelPanel.PanelActions()
		{
			@Override
			public void refreshRequested()
			{
				rescanSocialSources();
				refreshPanel();
			}

			@Override
			public void removeRequested(String memberName)
			{
				if (trackingService.removeMember(memberName))
				{
					configManager.setConfiguration(CONFIG_GROUP, "ignoredMembers", trackingService.serializeIgnoredMembers());
					refreshPanel();
				}
			}
		});
		navButton = NavigationButton.builder()
			.tooltip(PLUGIN_NAME)
			.icon(buildNavigationIcon())
			.priority(5)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
		log.debug("{} started", PLUGIN_NAME);
	}

	@Override
	protected void shutDown()
	{
		if (navButton != null)
		{
			clientToolbar.removeNavigation(navButton);
			navButton = null;
		}
		panel = null;
		trackingService = null;
		log.debug("{} stopped", PLUGIN_NAME);
	}

	private BufferedImage buildNavigationIcon()
	{
		BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setColor(new Color(41, 41, 41));
		graphics.fillRect(0, 0, 16, 16);
		graphics.setColor(new Color(73, 181, 90));
		graphics.fillRect(2, 10, 3, 4);
		graphics.fillRect(7, 6, 3, 8);
		graphics.fillRect(12, 3, 3, 11);
		graphics.dispose();
		return image;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN && trackingService != null)
		{
			rescanSocialSources();
			refreshPanel();
		}

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

	@Subscribe
	public void onConfigChanged(ConfigChanged configChanged)
	{
		if (!CONFIG_GROUP.equals(configChanged.getGroup()) || trackingService == null)
		{
			return;
		}
		trackingService.loadIgnoredMembers(config.ignoredMembers());
		rescanSocialSources();
		refreshPanel();
	}

	private void rescanSocialSources()
	{
		trackingService.rescan(
			SocialTrackingService.seedSnapshots(config.trackFriendsList(), config.trackClanMembers(), config.trackFriendsChat()),
			config.maxTrackedMembers()
		);
	}

	private void refreshPanel()
	{
		if (panel != null)
		{
			panel.setState(trackingService.snapshot(config.maxTrackedMembers()));
		}
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

		return PLUGIN_NAME + " is ready. It is tracking up to "
			+ safeMaxPlayers
			+ " visible rows from your configured social sources over a "
			+ safeWindow
			+ " minute activity window.";
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
