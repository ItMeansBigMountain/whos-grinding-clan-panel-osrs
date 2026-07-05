package com.itmeansbigmountain.whosgrindingclanpanel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
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
import javax.swing.SwingConstants;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

class WhosGrindingClanPanelPanel extends PluginPanel
{
	interface PanelActions
	{
		void refreshRequested();

		void removeRequested(String memberName);
	}

	private static final Color HIGH_INTENSITY = new Color(73, 181, 90);
	private static final Color MID_INTENSITY = new Color(211, 151, 43);
	private static final Color LOW_INTENSITY = new Color(94, 94, 94);
	private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss 'UTC'").withZone(ZoneOffset.UTC);
	private static final int PANEL_TEXT_WIDTH = 154;
	private static final int MEMBER_TEXT_WIDTH = 96;

	private final JPanel content = new JPanel();
	private final WhosGrindingClanPanelConfig config;
	private final PanelActions actions;
	private SocialTrackerState state;
	private SocialSourceFilter filter = SocialSourceFilter.FRIENDS_CHAT;

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
			content.add(statusLabel("No tracked members for " + filter.label() + ". Press Rescan after logging in or enable more sources."));
		}
		else
		{
			for (TrackedMember member : visibleMembers)
			{
				content.add(memberRow(member));
			}
		}

		content.add(Box.createVerticalStrut(10));
		content.add(sectionTitle("Clan grind heatmap"));
		content.add(summaryLabel(config.heatmapHistoryDays() + " day window • "
			+ config.activeHourThreshold() + "+ events = hot hour"));
		content.add(Box.createVerticalStrut(5));
		content.add(heatmapGrid());

		content.add(Box.createVerticalStrut(10));
		content.add(sectionTitle("Data status"));
		content.add(summaryLabel("Local social tracking is wired. RuneLite live source scanners and Wise Old Man/TempleOSRS enrichment are next."));

		revalidate();
		repaint();
	}

	private JPanel sourceSelector()
	{
		JPanel row = new JPanel(new BorderLayout(3, 0));
		row.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JComboBox<SocialSourceFilter> sourceDropdown = new JComboBox<>(SocialSourceFilter.values());
		sourceDropdown.setSelectedItem(filter);
		sourceDropdown.setFont(sourceDropdown.getFont().deriveFont(11f));
		sourceDropdown.setFocusable(false);
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
		refreshButton.setMargin(new Insets(0, 5, 0, 5));
		refreshButton.setFocusable(false);
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
		JLabel label = new JLabel("<html><body style='width:" + MEMBER_TEXT_WIDTH + "px'><b>" + escapeHtml(member.displayName()) + "</b><br>"
			+ escapeHtml(member.status().label() + world + " • " + sources) + "<br>"
			+ escapeHtml(member.activitySummary()) + "</body></html>");
		label.setForeground(Color.WHITE);
		row.add(label, BorderLayout.CENTER);

		JButton removeButton = new JButton("×");
		removeButton.setToolTipText("Remove from tracking");
		removeButton.setMargin(new Insets(0, 4, 0, 4));
		removeButton.addActionListener(event -> actions.removeRequested(member.displayName()));
		row.add(removeButton, BorderLayout.EAST);
		return row;
	}

	private JPanel heatmapGrid()
	{
		JPanel grid = new JPanel(new GridLayout(4, 6, 1, 1));
		grid.setBackground(ColorScheme.DARK_GRAY_COLOR);
		List<Integer> counts = heatmapCountsFromTrackedMembers();

		for (int hour = 0; hour < ClanGrindHeatmapModel.HOURS_PER_DAY; hour++)
		{
			int intensity = ClanGrindHeatmapModel.intensity(counts.get(hour), config.activeHourThreshold());
			JLabel cell = new JLabel(String.format("%02d", hour), SwingConstants.CENTER);
			cell.setOpaque(true);
			cell.setForeground(Color.WHITE);
			cell.setToolTipText(String.format("%02d:00 UTC • %d tracked updates • %d%% intensity", hour, counts.get(hour), intensity));
			cell.setBackground(colorForIntensity(intensity));
			cell.setFont(cell.getFont().deriveFont(9f));
			cell.setPreferredSize(new Dimension(23, 20));
			grid.add(cell);
		}

		return grid;
	}

	private List<Integer> heatmapCountsFromTrackedMembers()
	{
		if (state.members().isEmpty())
		{
			return Arrays.asList(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		}
		return ClanGrindHeatmapModel.hourlyBuckets(state.members().stream().map(TrackedMember::lastSeen).collect(Collectors.toList()));
	}

	private Color colorForIntensity(int intensity)
	{
		if (intensity >= 100)
		{
			return HIGH_INTENSITY;
		}
		if (intensity >= 50)
		{
			return MID_INTENSITY;
		}
		return LOW_INTENSITY;
	}

	private String formatSources(Set<TrackedMemberSource> sources)
	{
		return sources.stream().map(TrackedMemberSource::label).collect(Collectors.joining(", "));
	}

	private String escapeHtml(String text)
	{
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
}
