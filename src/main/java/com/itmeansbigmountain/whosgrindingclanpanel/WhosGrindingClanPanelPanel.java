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

		void removeRequested(String memberName);
	}

	private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss 'UTC'").withZone(ZoneOffset.UTC);
	private static final int PANEL_TEXT_WIDTH = WhosGrindingPanelDimensions.CONTENT_WIDTH;
	private static final int MEMBER_TEXT_WIDTH = WhosGrindingPanelDimensions.MEMBER_TEXT_WIDTH;
	private static final int CONTROL_HEIGHT = WhosGrindingPanelDimensions.CONTROL_HEIGHT;

	private final JPanel content = new JPanel();
	private final WhosGrindingClanPanelConfig config;
	private final PanelActions actions;
	private final WiseOldManGainedClient gainedClient = new WiseOldManGainedClient();
	private final Map<String, String> grindingSummaryCache = new ConcurrentHashMap<>();
	private SocialTrackerState state;
	private SocialSourceFilter filter = SocialSourceFilter.FRIENDS_CHAT;
	private TrackedMember selectedMember;

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
		content.add(summaryLabel("Tracked " + state.members().size() + "/" + state.maxTrackedMembers()
			+ " • ignored " + state.ignoredCount()));
		content.add(summaryLabel("Last scan: " + TIME_FORMAT.format(state.refreshedAt())));
		content.add(Box.createVerticalStrut(5));
		content.add(sourceSelector());
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
				if (isSelected(member))
				{
					content.add(expandedGrindingCard(member));
				}
			}
		}

		revalidate();
		repaint();
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
		sourceDropdown.setMaximumSize(new Dimension(PANEL_TEXT_WIDTH - 30, CONTROL_HEIGHT));
		sourceDropdown.setPreferredSize(new Dimension(PANEL_TEXT_WIDTH - 30, CONTROL_HEIGHT));
		sourceDropdown.addActionListener(event -> {
			SocialSourceFilter selectedFilter = (SocialSourceFilter) sourceDropdown.getSelectedItem();
			if (selectedFilter != null && selectedFilter != filter)
			{
				filter = selectedFilter;
				rebuild();
			}
		});
		row.add(sourceDropdown, BorderLayout.CENTER);

		JButton refreshButton = new JButton("↻");
		refreshButton.setToolTipText("Rescan social sources");
		refreshButton.setMargin(new Insets(0, 3, 0, 3));
		refreshButton.setFocusable(false);
		refreshButton.setMaximumSize(new Dimension(26, CONTROL_HEIGHT));
		refreshButton.setPreferredSize(new Dimension(26, CONTROL_HEIGHT));
		refreshButton.addActionListener(event -> actions.refreshRequested());
		row.add(refreshButton, BorderLayout.EAST);
		return row;
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
		row.setBackground(isSelected(member) ? ColorScheme.DARK_GRAY_HOVER_COLOR : ColorScheme.DARKER_GRAY_COLOR);
		row.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.DARK_GRAY_COLOR),
			BorderFactory.createEmptyBorder(2, 2, 2, 2)
		));
		row.setMaximumSize(new Dimension(PANEL_TEXT_WIDTH, 28));
		row.setPreferredSize(new Dimension(PANEL_TEXT_WIDTH, 28));
		row.setAlignmentX(Component.LEFT_ALIGNMENT);
		row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		String world = member.lastWorld() > 0 ? " W" + member.lastWorld() : "";
		String expanded = isSelected(member) ? "▾ " : "▸ ";
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
				if (isSelected(member))
				{
					selectedMember = null;
				}
				else
				{
					selectedMember = member;
					ensureGrindingSummaryLoaded(member);
				}
				rebuild();
			}
		};
		row.addMouseListener(toggleListener);
		label.addMouseListener(toggleListener);
		row.add(label, BorderLayout.CENTER);
		return row;
	}

	private JPanel expandedGrindingCard(TrackedMember member)
	{
		JPanel card = new JPanel();
		card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
		card.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		card.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.DARK_GRAY_COLOR),
			BorderFactory.createEmptyBorder(0, 0, 4, 3)
		));
		ensureGrindingSummaryLoaded(member);
		JLabel title = cardLine("<span style='color:#d3972b'><b>Grinding " + escapeHtml(config.gainsPeriod().label()) + "</b></span>", 12f);
		card.add(title);
		int cardHeight = title.getPreferredSize().height;
		for (String line : grindingSummaryFor(member).split("<br>"))
		{
			JLabel row = cardLine(line, 11f);
			card.add(row);
			cardHeight += row.getPreferredSize().height;
		}
		cardHeight += 4;
		card.setPreferredSize(new Dimension(PANEL_TEXT_WIDTH, cardHeight));
		card.setMaximumSize(new Dimension(PANEL_TEXT_WIDTH, cardHeight));
		card.setAlignmentX(Component.LEFT_ALIGNMENT);
		return card;
	}

	private boolean isSelected(TrackedMember member)
	{
		return selectedMember != null
			&& TrackedMember.normalizeKey(selectedMember.displayName()).equals(TrackedMember.normalizeKey(member.displayName()));
	}

	private void ensureGrindingSummaryLoaded(TrackedMember member)
	{
		String cacheKey = grindingCacheKey(member);
		if (!config.enableWiseOldManLookups())
		{
			grindingSummaryCache.put(cacheKey, "Wise Old Man lookups are disabled in config.");
			return;
		}
		if (grindingSummaryCache.containsKey(cacheKey))
		{
			return;
		}
		grindingSummaryCache.put(cacheKey, "Loading Wise Old Man gains for " + config.gainsPeriod().label() + "...");
		new SwingWorker<String, Void>()
		{
			@Override
			protected String doInBackground() throws Exception
			{
				return gainedClient.fetchGrindingSummary(member.displayName(), config.gainsPeriod());
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
					grindingSummaryCache.put(cacheKey, "Could not load Wise Old Man gains. Try refresh or check the player on Wise Old Man.");
				}
				rebuild();
			}
		}.execute();
	}

	private String grindingSummaryFor(TrackedMember member)
	{
		return grindingSummaryCache.getOrDefault(grindingCacheKey(member), "Loading Wise Old Man gains for " + config.gainsPeriod().label() + "...");
	}

	private String grindingCacheKey(TrackedMember member)
	{
		return TrackedMember.normalizeKey(member.displayName()) + ":" + config.gainsPeriod().wiseOldManPeriod();
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
