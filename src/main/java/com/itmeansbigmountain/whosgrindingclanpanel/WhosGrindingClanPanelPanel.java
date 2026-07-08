package com.itmeansbigmountain.whosgrindingclanpanel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
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
		content.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
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
			content.add(statusLabel("No tracked members for " + filter.label() + ". Press ↻ after logging in or enable the source in config."));
		}
		else
		{
			for (TrackedMember member : visibleMembers)
			{
				content.add(memberRow(member));
			}
		}

		content.add(Box.createVerticalStrut(8));
		content.add(sectionTitle("Selected player"));
		content.add(detailCardForSelectedMember(visibleMembers));

		content.add(Box.createVerticalStrut(8));
		content.add(sectionTitle("Data links"));
		content.add(summaryLabel("Period: " + config.gainsPeriod().label() + " • click a player row for WOM/Temple/hiscore URLs."));
		content.add(summaryLabel("External enrichment is click/refresh cached; compact vertical detail view saves sidebar space."));

		revalidate();
		repaint();
	}

	private JPanel sourceSelector()
	{
		JPanel row = new JPanel(new BorderLayout(3, 0));
		row.setBackground(ColorScheme.DARK_GRAY_COLOR);
		row.setMaximumSize(new Dimension(PANEL_TEXT_WIDTH, CONTROL_HEIGHT));
		row.setPreferredSize(new Dimension(PANEL_TEXT_WIDTH, CONTROL_HEIGHT));

		JComboBox<SocialSourceFilter> sourceDropdown = new JComboBox<>(SocialSourceFilter.values());
		sourceDropdown.setSelectedItem(filter);
		sourceDropdown.setFont(sourceDropdown.getFont().deriveFont(10f));
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
		JPanel row = new JPanel(new BorderLayout(3, 0));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.DARK_GRAY_COLOR),
			BorderFactory.createEmptyBorder(4, 4, 4, 4)
		));

		String sources = formatSources(member.sources());
		String world = member.lastWorld() > 0 ? " • W" + member.lastWorld() : "";
		JLabel label = new JLabel("<html><body style='width:" + MEMBER_TEXT_WIDTH + "px'><b>" + activityIcon(member) + " " + escapeHtml(member.displayName()) + "</b><br>"
			+ escapeHtml(member.status().label() + world + " • " + sources) + "<br>"
			+ escapeHtml(member.activitySummary()) + "</body></html>");
		label.setFont(label.getFont().deriveFont(10f));
		label.setForeground(Color.WHITE);
		label.setToolTipText("Click for tracker details");
		label.addMouseListener(new java.awt.event.MouseAdapter()
		{
			@Override
			public void mouseClicked(java.awt.event.MouseEvent event)
			{
				selectedMember = member;
				rebuild();
			}
		});
		row.add(label, BorderLayout.CENTER);

		JButton removeButton = new JButton("×");
		removeButton.setToolTipText("Remove from tracking");
		removeButton.setMargin(new Insets(0, 4, 0, 4));
		removeButton.addActionListener(event -> actions.removeRequested(member.displayName()));
		row.add(removeButton, BorderLayout.EAST);
		return row;
	}

	private JPanel detailCardForSelectedMember(List<TrackedMember> visibleMembers)
	{
		TrackedMember member = selectedVisibleMember(visibleMembers);
		if (member == null)
		{
			return compactInfoCard("Select a row to show gains links, current grind summary, world, and timestamps.");
		}

		JPanel card = new JPanel();
		card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
		card.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		card.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		card.setMaximumSize(new Dimension(PANEL_TEXT_WIDTH, Short.MAX_VALUE));

		card.add(detailLine("Name", member.displayName(), true));
		card.add(detailLine("Status", member.status().label() + (member.lastWorld() > 0 ? " • W" + member.lastWorld() : ""), false));
		card.add(detailLine("Grinding", member.activitySummary(), false));
		card.add(detailLine("Period", config.gainsPeriod().label(), false));
		card.add(detailLine("WOM", PlayerTrackingLinks.wiseOldManGainedUrl(member.displayName(), config.gainsPeriod()), false));
		card.add(detailLine("Temple", PlayerTrackingLinks.templePlayerUrl(member.displayName()), false));
		card.add(detailLine("Hiscore", PlayerTrackingLinks.officialHiscoreUrl(member.displayName()), false));
		card.add(detailLine("Seen", TIME_FORMAT.format(member.firstSeen()) + " → " + TIME_FORMAT.format(member.lastSeen()), false));
		return card;
	}

	private TrackedMember selectedVisibleMember(List<TrackedMember> visibleMembers)
	{
		if (selectedMember == null)
		{
			return visibleMembers.isEmpty() ? null : visibleMembers.get(0);
		}
		String selectedName = WhosGrindingClanPanelPlugin.normalizePlayerName(selectedMember.displayName());
		return visibleMembers.stream()
			.filter(member -> WhosGrindingClanPanelPlugin.normalizePlayerName(member.displayName()).equals(selectedName))
			.findFirst()
			.orElse(visibleMembers.isEmpty() ? null : visibleMembers.get(0));
	}

	private JPanel compactInfoCard(String text)
	{
		JPanel card = new JPanel(new BorderLayout());
		card.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		card.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		card.setMaximumSize(new Dimension(PANEL_TEXT_WIDTH, 52));
		card.add(summaryLabel(text), BorderLayout.CENTER);
		return card;
	}

	private JLabel detailLine(String labelText, String value, boolean strong)
	{
		String color = strong ? "#ffffff" : "#d3d3d3";
		JLabel label = new JLabel("<html><body style='width:" + (PANEL_TEXT_WIDTH - 12) + "px'><span style='color:#d3972b'>"
			+ escapeHtml(labelText) + ":</span> <span style='color:" + color + "'>" + escapeHtml(value) + "</span></body></html>");
		label.setFont(label.getFont().deriveFont(strong ? Font.BOLD : Font.PLAIN, 10f));
		label.setBorder(BorderFactory.createEmptyBorder(1, 0, 1, 0));
		return label;
	}

	private String formatSources(Set<TrackedMemberSource> sources)
	{
		return sources.stream().map(TrackedMemberSource::label).collect(Collectors.joining(", "));
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
