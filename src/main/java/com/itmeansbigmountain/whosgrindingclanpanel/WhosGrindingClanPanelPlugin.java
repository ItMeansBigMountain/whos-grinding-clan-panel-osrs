package com.itmeansbigmountain.whosgrindingclanpanel;

import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.Friend;
import net.runelite.api.FriendContainer;
import net.runelite.api.FriendsChatManager;
import net.runelite.api.FriendsChatMember;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanChannelMember;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
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
	description = "Shows compact WOM gains for friends, friends chat, and clan members.",
	tags = {"friends", "grind", "activity", "skills", "xp"}
)
public class WhosGrindingClanPanelPlugin extends Plugin
{
	static final String PLUGIN_NAME = "Who's Grinding Panel";
	static final String CONFIG_GROUP = "whosgrindingclanpanel";
	static final int DEFAULT_ACTIVITY_WINDOW_MINUTES = 30;
	static final int DEFAULT_MAX_PLAYERS_SHOWN = 8;
	static final int MIN_ACTIVITY_WINDOW_MINUTES = 5;
	static final int MAX_ACTIVITY_WINDOW_MINUTES = 240;
	static final int MIN_PLAYERS_SHOWN = 1;
	static final int MAX_PLAYERS_SHOWN = 25;
	@Inject
	private Client client;

	@Inject
	private WhosGrindingClanPanelConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ClientToolbar clientToolbar;

	private WhosGrindingClanPanelPanel panel;
	private NavigationButton navButton;
	private SocialTrackingService trackingService;
	private Instant lastAutomaticRefresh;

	@Override
	protected void startUp()
	{
		trackingService = new SocialTrackingService();
		trackingService.loadIgnoredMembers(config.ignoredMembers());
		rescanSocialSources("startup");

		panel = new WhosGrindingClanPanelPanel(config, trackingService.snapshot(config.maxTrackedMembers()), new WhosGrindingClanPanelPanel.PanelActions()
		{
			@Override
			public void refreshRequested()
			{
				rescanSocialSources("manual refresh");
				refreshPanel();
			}

			@Override
			public void gainsPeriodChanged(GainsPeriod gainsPeriod)
			{
				configManager.setConfiguration(CONFIG_GROUP, "gainsPeriod", gainsPeriod);
				refreshPanel();
			}

			@Override
			public void showOfflineFriendsChanged(boolean showOfflineFriends)
			{
				configManager.setConfiguration(CONFIG_GROUP, "showOfflineFriends", showOfflineFriends);
				if (!showOfflineFriends)
				{
					trackingService.removeOfflineFriends();
					refreshPanel();
					return;
				}
				rescanSocialSources("show offline friends changed");
				refreshPanel();
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
		lastAutomaticRefresh = null;
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
			rescanSocialSources("login");
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
	public void onGameTick(GameTick gameTick)
	{
		if (client.getGameState() != GameState.LOGGED_IN || trackingService == null)
		{
			return;
		}

		Instant now = Instant.now();
		int refreshMinutes = Math.max(1, config.refreshIntervalMinutes());
		if (lastAutomaticRefresh == null || Duration.between(lastAutomaticRefresh, now).toMinutes() >= refreshMinutes)
		{
			rescanSocialSources("scheduled " + refreshMinutes + " minute refresh");
			refreshPanel();
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged configChanged)
	{
		if (!CONFIG_GROUP.equals(configChanged.getGroup()) || trackingService == null)
		{
			return;
		}
		trackingService.loadIgnoredMembers(config.ignoredMembers());
		rescanSocialSources("config change");
		refreshPanel();
	}

	private void rescanSocialSources(String reason)
	{
		updateCurrentPlayerName();
		trackingService.rescan(
			buildSocialSnapshots(),
			config.maxTrackedMembers()
		);
		if (!config.showOfflineFriends())
		{
			trackingService.removeOfflineFriends();
		}
		lastAutomaticRefresh = Instant.now();
		log.debug("{} rescanned social sources: {}", PLUGIN_NAME, reason);
	}

	private void updateCurrentPlayerName()
	{
		if (trackingService == null)
		{
			return;
		}
		Player localPlayer = client.getLocalPlayer();
		trackingService.setCurrentPlayerName(localPlayer == null ? null : localPlayer.getName());
	}

	private List<SocialSourceSnapshot> buildSocialSnapshots()
	{
		List<SocialSourceSnapshot> snapshots = new ArrayList<>();
		if (config.trackFriendsList())
		{
			snapshots.add(scanFriendsList());
		}
		if (config.trackFriendsChat())
		{
			snapshots.add(scanFriendsChat());
		}
		if (config.trackClanChat())
		{
			snapshots.add(scanClanChat());
		}
		if (snapshots.isEmpty())
		{
			snapshots.add(SocialSourceSnapshot.unsupported(TrackedMemberSource.FRIEND, "All tracking sources are disabled in config."));
		}
		return snapshots;
	}

	private SocialSourceSnapshot scanFriendsList()
	{
		FriendContainer friendContainer = client.getFriendContainer();
		if (friendContainer == null || friendContainer.getMembers() == null)
		{
			return SocialSourceSnapshot.unsupported(TrackedMemberSource.FRIEND, "Friends list is not available yet.");
		}

		List<SocialMemberSnapshot> members = new ArrayList<>();
		for (Friend friend : friendContainer.getMembers())
		{
			if (friend != null && (config.showOfflineFriends() || friend.getWorld() > 0))
			{
				members.add(SocialMemberSnapshot.of(friend.getName(), friend.getWorld(), sourceSummary("Friend", friend.getWorld())));
			}
		}
		return SocialSourceSnapshot.observedMembers(TrackedMemberSource.FRIEND, members);
	}

	private SocialSourceSnapshot scanFriendsChat()
	{
		FriendsChatManager friendsChatManager = client.getFriendsChatManager();
		if (friendsChatManager == null || friendsChatManager.getMembers() == null)
		{
			return SocialSourceSnapshot.unsupported(TrackedMemberSource.FRIENDS_CHAT, "Friends chat is not available yet.");
		}

		List<SocialMemberSnapshot> members = new ArrayList<>();
		for (FriendsChatMember friendsChatMember : friendsChatManager.getMembers())
		{
			if (friendsChatMember != null)
			{
				members.add(SocialMemberSnapshot.of(friendsChatMember.getName(), friendsChatMember.getWorld(), sourceSummary("Friends chat", friendsChatMember.getWorld())));
			}
		}
		return SocialSourceSnapshot.observedMembers(TrackedMemberSource.FRIENDS_CHAT, members);
	}

	private SocialSourceSnapshot scanClanChat()
	{
		ClanChannel clanChannel = client.getClanChannel();
		if (clanChannel == null || clanChannel.getMembers() == null)
		{
			return SocialSourceSnapshot.unsupported(TrackedMemberSource.CLAN, "Clan chat is not available yet.");
		}

		List<SocialMemberSnapshot> members = new ArrayList<>();
		for (ClanChannelMember clanMember : clanChannel.getMembers())
		{
			if (clanMember != null)
			{
				members.add(SocialMemberSnapshot.of(clanMember.getName(), clanMember.getWorld(), sourceSummary("Clan chat", clanMember.getWorld())));
			}
		}
		return SocialSourceSnapshot.observedMembers(TrackedMemberSource.CLAN, members);
	}

	private String sourceSummary(String sourceName, int world)
	{
		return world > 0 ? sourceName + " • world " + world : sourceName + " • offline";
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
			+ " visible rows from your friends and friends chat sources over a "
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
			return "Unknown player";
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
