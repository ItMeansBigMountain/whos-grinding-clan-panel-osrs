package com.itmeansbigmountain.whosgrindingclanpanel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Cursor;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

class WhosGrindingClanPanelPanel extends PluginPanel
{
	interface PanelActions
	{
		void refreshRequested();

		void gainsPeriodChanged(GainsPeriod gainsPeriod);

		void showOfflineFriendsChanged(boolean showOfflineFriends);
	}

	private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss 'UTC'").withZone(ZoneOffset.UTC);
	private static final int PANEL_TEXT_WIDTH = WhosGrindingPanelDimensions.CONTENT_WIDTH;
	private static final int MEMBER_TEXT_WIDTH = WhosGrindingPanelDimensions.MEMBER_TEXT_WIDTH;
	private static final int CONTROL_HEIGHT = WhosGrindingPanelDimensions.CONTROL_HEIGHT;

	private final JPanel content = new JPanel();
	private final WhosGrindingClanPanelConfig config;
	private final PanelActions actions;
	private final WiseOldManGainedClient gainedClient = new WiseOldManGainedClient();
	private final OfficialHiscoresGainedClient hiscoresClient = new OfficialHiscoresGainedClient();
	private final Map<String, String> grindingSummaryCache = new ConcurrentHashMap<>();
	private SocialTrackerState state;
	private SocialSourceFilter filter = SocialSourceFilter.FRIENDS_CHAT;
	private String selectedPlayerName;

	WhosGrindingClanPanelPanel(WhosGrindingClanPanelConfig config, SocialTrackerState state, PanelActions actions)
	{
		super(false);
		this.config = config;
		this.state = state;
		this.actions = actions;

		setLayout(new BorderLayout());
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
		content.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JScrollPane scrollPane = new JScrollPane(content);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		add(scrollPane, BorderLayout.CENTER);

		rebuild();
	}

	void setState(SocialTrackerState state)
	{
		this.state = state;
		rebuild();
	}

	void rebuild()
	{
		content.removeAll();
		content.add(sectionTitle("Who's grinding"));
		addCurrentPlayerHeader();
		content.add(summaryLabel("Tracked " + state.members().size() + "/" + state.maxTrackedMembers()
			+ " • ignored " + state.ignoredCount()));
		content.add(summaryLabel("Last scan: " + TIME_FORMAT.format(state.refreshedAt())));
		content.add(Box.createVerticalStrut(5));
		content.add(sourceSelector());
		content.add(Box.createVerticalStrut(3));
		content.add(lookbackControls());
		content.add(Box.createVerticalStrut(4));

		for (String message : state.messages())
		{
			content.add(statusLabel(message));
		}

		List<TrackedMember> visibleMembers = state.filteredMembers(filter);
		if (visibleMembers.isEmpty())
		{
			content.add(statusLabel("No tracked members for " + filter.label() + ". Press ↻ after logging in."));
		}
		else
		{
			for (TrackedMember member : visibleMembers)
			{
				content.add(memberRow(member));
				if (isSelected(member.displayName()))
				{
					content.add(expandedGrindingCard(member.displayName()));
				}
			}
		}

		revalidate();
		repaint();
	}

	private void addCurrentPlayerHeader()
	{
		String currentPlayerName = state.currentPlayerName();
		if (currentPlayerName == null || currentPlayerName.trim().isEmpty())
		{
			content.add(currentPlayerRow("log in to show your character", false));
			return;
		}
		content.add(currentPlayerRow(currentPlayerName, true));
		if (isSelected(currentPlayerName))
		{
			content.add(expandedGrindingCard(currentPlayerName));
		}
	}

	private JPanel currentPlayerRow(String playerName, boolean loggedIn)
	{
		JPanel row = new JPanel(new BorderLayout(4, 0));
		row.setBackground(ColorScheme.DARK_GRAY_COLOR);
		row.setMaximumSize(new Dimension(PANEL_TEXT_WIDTH, 28));
		row.setPreferredSize(new Dimension(PANEL_TEXT_WIDTH, 28));
		row.setAlignmentX(Component.LEFT_ALIGNMENT);

		JPanel playerBox = new JPanel(new BorderLayout(0, 0));
		playerBox.setBackground(loggedIn && isSelected(playerName) ? ColorScheme.DARK_GRAY_HOVER_COLOR : ColorScheme.DARKER_GRAY_COLOR);
		playerBox.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.DARK_GRAY_COLOR),
			BorderFactory.createEmptyBorder(2, 2, 2, 2)
		));
		playerBox.setMaximumSize(new Dimension(PANEL_TEXT_WIDTH - 30, 28));
		playerBox.setPreferredSize(new Dimension(PANEL_TEXT_WIDTH - 30, 28));
		if (loggedIn)
		{
			playerBox.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		}

		String expanded = loggedIn && isSelected(playerName) ? "▾ " : "▸ ";
		JLabel label = new JLabel("<html><body style='width:" + (MEMBER_TEXT_WIDTH - 30) + "px'>"
			+ "<b>" + expanded + "You: " + escapeHtml(playerName) + "</b>"
			+ (loggedIn ? " <span style='color:#b8b8b8'>what others see</span>" : "") + "</body></html>");
		label.setFont(label.getFont().deriveFont(11f));
		label.setForeground(Color.WHITE);
		label.setToolTipText(loggedIn ? "Click to expand/collapse your grinding details" : "Log in to show your character");

		java.awt.event.MouseAdapter toggleListener = new java.awt.event.MouseAdapter()
		{
			@Override
			public void mouseClicked(java.awt.event.MouseEvent event)
			{
				toggleSelectedPlayer(playerName);
			}
		};
		if (loggedIn)
		{
			playerBox.addMouseListener(toggleListener);
			label.addMouseListener(toggleListener);
		}
		playerBox.add(label, BorderLayout.CENTER);
		row.add(playerBox, BorderLayout.CENTER);
		row.add(refreshButton(), BorderLayout.EAST);
		return row;
	}

	private JPanel sourceSelector()
	{
		JPanel row = new JPanel(new BorderLayout(3, 0));
		row.setBackground(ColorScheme.DARK_GRAY_COLOR);
		row.setMaximumSize(new Dimension(PANEL_TEXT_WIDTH, CONTROL_HEIGHT));
		row.setPreferredSize(new Dimension(PANEL_TEXT_WIDTH, CONTROL_HEIGHT));
		row.setAlignmentX(Component.LEFT_ALIGNMENT);

		JComboBox<SocialSourceFilter> sourceDropdown = new JComboBox<>(SocialSourceFilter.values());
		sourceDropdown.setSelectedItem(filter);
		sourceDropdown.setFont(sourceDropdown.getFont().deriveFont(11f));
		sourceDropdown.setFocusable(false);
		sourceDropdown.setMaximumSize(new Dimension(PANEL_TEXT_WIDTH, CONTROL_HEIGHT));
		sourceDropdown.setPreferredSize(new Dimension(PANEL_TEXT_WIDTH, CONTROL_HEIGHT));
		sourceDropdown.addActionListener(event -> {
			SocialSourceFilter selectedFilter = (SocialSourceFilter) sourceDropdown.getSelectedItem();
			if (selectedFilter != null && selectedFilter != filter)
			{
				filter = selectedFilter;
				rebuild();
			}
		});
		row.add(sourceDropdown, BorderLayout.CENTER);
		return row;
	}

	private JPanel lookbackControls()
	{
		JPanel row = new JPanel(new BorderLayout(3, 0));
		row.setBackground(ColorScheme.DARK_GRAY_COLOR);
		row.setMaximumSize(new Dimension(PANEL_TEXT_WIDTH, CONTROL_HEIGHT));
		row.setPreferredSize(new Dimension(PANEL_TEXT_WIDTH, CONTROL_HEIGHT));
		row.setAlignmentX(Component.LEFT_ALIGNMENT);

		JComboBox<GainsPeriod> gainsPeriodDropdown = new JComboBox<>(GainsPeriod.values());
		gainsPeriodDropdown.setSelectedItem(config.gainsPeriod());
		gainsPeriodDropdown.setFont(gainsPeriodDropdown.getFont().deriveFont(11f));
		gainsPeriodDropdown.setFocusable(false);
		gainsPeriodDropdown.setMaximumSize(new Dimension(PANEL_TEXT_WIDTH - 30, CONTROL_HEIGHT));
		gainsPeriodDropdown.setPreferredSize(new Dimension(PANEL_TEXT_WIDTH - 30, CONTROL_HEIGHT));
		gainsPeriodDropdown.addActionListener(event -> {
			GainsPeriod selectedPeriod = (GainsPeriod) gainsPeriodDropdown.getSelectedItem();
			if (selectedPeriod != null && selectedPeriod != config.gainsPeriod())
			{
				grindingSummaryCache.clear();
				actions.gainsPeriodChanged(selectedPeriod);
			}
		});
		row.add(gainsPeriodDropdown, BorderLayout.CENTER);

		JCheckBox showOfflineCheckbox = new JCheckBox("☾", config.showOfflineFriends());
		showOfflineCheckbox.setToolTipText("Show offline friends");
		showOfflineCheckbox.setBackground(ColorScheme.DARK_GRAY_COLOR);
		showOfflineCheckbox.setForeground(Color.LIGHT_GRAY);
		showOfflineCheckbox.setMargin(new Insets(0, 0, 0, 0));
		showOfflineCheckbox.setFocusable(false);
		showOfflineCheckbox.setMaximumSize(new Dimension(26, CONTROL_HEIGHT));
		showOfflineCheckbox.setPreferredSize(new Dimension(26, CONTROL_HEIGHT));
		showOfflineCheckbox.addActionListener(event -> actions.showOfflineFriendsChanged(showOfflineCheckbox.isSelected()));
		row.add(showOfflineCheckbox, BorderLayout.EAST);
		return row;
	}

	private JButton refreshButton()
	{
		JButton refreshButton = new JButton("↻");
		refreshButton.setToolTipText("Rescan social sources");
		refreshButton.setMargin(new Insets(0, 3, 0, 3));
		refreshButton.setFocusable(false);
		refreshButton.setMaximumSize(new Dimension(26, CONTROL_HEIGHT));
		refreshButton.setPreferredSize(new Dimension(26, CONTROL_HEIGHT));
		refreshButton.addActionListener(event -> {
			grindingSummaryCache.clear();
			actions.refreshRequested();
		});
		return refreshButton;
	}

	private JLabel sectionTitle(String text)
	{
		JLabel label = new JLabel(text);
		label.setForeground(Color.WHITE);
		label.setFont(label.getFont().deriveFont(Font.BOLD, 12f));
		label.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
		return label;
	}

	private JLabel summaryLabel(String text)
	{
		JLabel label = new JLabel("<html><body style='width:" + PANEL_TEXT_WIDTH + "px'>" + escapeHtml(text) + "</body></html>");
		label.setForeground(Color.LIGHT_GRAY);
		label.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
		return label;
	}

	private JLabel statusLabel(String text)
	{
		JLabel label = summaryLabel(text);
		label.setForeground(new Color(211, 151, 43));
		return label;
	}

	private JPanel memberRow(TrackedMember member)
	{
		JPanel row = new JPanel(new BorderLayout(0, 0));
		row.setBackground(isSelected(member.displayName()) ? ColorScheme.DARK_GRAY_HOVER_COLOR : ColorScheme.DARKER_GRAY_COLOR);
		row.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.DARK_GRAY_COLOR),
			BorderFactory.createEmptyBorder(2, 2, 2, 2)
		));
		row.setMaximumSize(new Dimension(PANEL_TEXT_WIDTH, 28));
		row.setPreferredSize(new Dimension(PANEL_TEXT_WIDTH, 28));
		row.setAlignmentX(Component.LEFT_ALIGNMENT);
		row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		String world = member.lastWorld() > 0 ? " W" + member.lastWorld() : "";
		String expanded = isSelected(member.displayName()) ? "▾ " : "▸ ";
		JLabel label = new JLabel("<html><body style='width:" + MEMBER_TEXT_WIDTH + "px'>"
			+ "<b>" + expanded + activityIcon(member) + " " + escapeHtml(member.displayName()) + "</b>"
			+ " <span style='color:#b8b8b8'>" + escapeHtml(member.status().label() + world) + "</span></body></html>");
		label.setFont(label.getFont().deriveFont(11f));
		label.setForeground(Color.WHITE);
		label.setToolTipText("Click to expand/collapse grinding details");

		java.awt.event.MouseAdapter toggleListener = new java.awt.event.MouseAdapter()
		{
			@Override
			public void mouseClicked(java.awt.event.MouseEvent event)
			{
				toggleSelectedPlayer(member.displayName());
			}
		};
		row.addMouseListener(toggleListener);
		label.addMouseListener(toggleListener);
		row.add(label, BorderLayout.CENTER);
		return row;
	}

	private JPanel expandedGrindingCard(String playerName)
	{
		JPanel card = new JPanel();
		card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
		card.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		card.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.DARK_GRAY_COLOR),
			BorderFactory.createEmptyBorder(0, 0, 4, 3)
		));
		ensureGrindingSummaryLoaded(playerName);
		JLabel title = cardLine("<span style='color:#d3972b'><b>Grinding " + escapeHtml(config.gainsPeriod().label()) + "</b></span>", 13f);
		card.add(title);
		int cardHeight = title.getPreferredSize().height;
		for (String line : grindingSummaryFor(playerName).split("<br>"))
		{
			JLabel row = cardLine(line, 12f);
			card.add(row);
			cardHeight += row.getPreferredSize().height;
		}
		cardHeight += 4;
		card.setPreferredSize(new Dimension(PANEL_TEXT_WIDTH, cardHeight));
		card.setMaximumSize(new Dimension(PANEL_TEXT_WIDTH, cardHeight));
		card.setAlignmentX(Component.LEFT_ALIGNMENT);
		return card;
	}

	private boolean isSelected(String playerName)
	{
		return selectedPlayerName != null
			&& TrackedMember.normalizeKey(selectedPlayerName).equals(TrackedMember.normalizeKey(playerName));
	}

	private void toggleSelectedPlayer(String playerName)
	{
		if (isSelected(playerName))
		{
			selectedPlayerName = null;
		}
		else
		{
			selectedPlayerName = playerName;
			ensureGrindingSummaryLoaded(playerName);
		}
		rebuild();
	}

	private void ensureGrindingSummaryLoaded(String playerName)
	{
		String cacheKey = grindingCacheKey(playerName);
		if (!config.enableWiseOldManLookups() && config.gainDataSource() != GainDataSource.OFFICIAL_HISCORES)
		{
			grindingSummaryCache.put(cacheKey, "WOM lookups<br>are disabled<br>in config.");
			return;
		}
		if (grindingSummaryCache.containsKey(cacheKey))
		{
			return;
		}
		grindingSummaryCache.put(cacheKey, "Loading " + config.gainDataSource().toString() + "<br>for " + config.gainsPeriod().label() + "...");
		new SwingWorker<String, Void>()
		{
			@Override
			protected String doInBackground() throws Exception
			{
				return fetchConfiguredSummary(playerName);
			}

			@Override
			protected void done()
			{
				try
				{
					grindingSummaryCache.put(cacheKey, get());
				}
				catch (Exception ex)
				{
					grindingSummaryCache.put(cacheKey, dataSection("Fallback: Official OSRS hiscores", fallbackSourceNote(), fetchOfficialSummary(playerName)));
				}
				rebuild();
			}
		}.execute();
	}

	private String fetchConfiguredSummary(String playerName)
	{
		if (config.gainDataSource() == GainDataSource.OFFICIAL_HISCORES)
		{
			return dataSection("Fallback: Official OSRS hiscores", fallbackSourceNote(), fetchOfficialSummary(playerName));
		}
		return dataSection("Tracker API: Wise Old Man", "Weekly/monthly/yearly tracker period", fetchWomSummary(playerName));
	}

	private String fallbackSourceNote()
	{
		return "Difference between current scan and last plugin scan. Tracker periods come from WOM. Other fallback APIs checked/planned: TempleOSRS, Crystal Math Labs, official OSRS hiscores.";
	}

	private String separator()
	{
		return "<br><span style='color:#5f5f5f'>────────────────────</span><br>";
	}

	private String dataSection(String title, String source, String summary)
	{
		return "<b>" + escapeHtml(title) + "</b>:<br>"
			+ "<span style='color:#b8b8b8'>" + escapeHtml(source) + "</span><br>"
			+ summary;
	}

	private String fetchWomSummary(String playerName)
	{
		try
		{
			return gainedClient.fetchGrindingSummary(playerName, config.gainsPeriod());
		}
		catch (Exception ignored)
		{
			return "WOM gains are<br>not ready yet.";
		}
	}

	private String fetchOfficialSummary(String playerName)
	{
		try
		{
			return hiscoresClient.fetchGrindingSummary(playerName, config.gainsPeriod());
		}
		catch (Exception ignored)
		{
			return "Official hiscores<br>not available yet.";
		}
	}

	private String grindingSummaryFor(String playerName)
	{
		return grindingSummaryCache.getOrDefault(grindingCacheKey(playerName), "Loading " + config.gainDataSource().toString() + "<br>for " + config.gainsPeriod().label() + "...");
	}

	private String grindingCacheKey(String playerName)
	{
		return TrackedMember.normalizeKey(playerName) + ":" + config.gainsPeriod().wiseOldManPeriod() + ":" + config.gainDataSource().name();
	}

	private JLabel cardLine(String html, float fontSize)
	{
		JLabel label = new JLabel("<html><body style='width:" + PANEL_TEXT_WIDTH + "px; margin:0; padding:0'>"
			+ html + "</body></html>");
		label.setForeground(Color.LIGHT_GRAY);
		label.setFont(label.getFont().deriveFont(Font.PLAIN, fontSize));
		label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		label.setMaximumSize(new Dimension(PANEL_TEXT_WIDTH, label.getPreferredSize().height));
		return label;
	}

	private String activityIcon(TrackedMember member)
	{
		if (member.status() == TrackedMemberStatus.ONLINE)
		{
			return "●";
		}
		if (member.hasSource(TrackedMemberSource.FRIENDS_CHAT))
		{
			return "◇";
		}
		return "○";
	}

	private String escapeHtml(String text)
	{
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
}
