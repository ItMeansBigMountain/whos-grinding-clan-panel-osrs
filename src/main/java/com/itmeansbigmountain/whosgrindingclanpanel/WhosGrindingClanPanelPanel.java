package com.itmeansbigmountain.whosgrindingclanpanel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.Arrays;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

class WhosGrindingClanPanelPanel extends PluginPanel
{
	private static final Color HIGH_INTENSITY = new Color(73, 181, 90);
	private static final Color MID_INTENSITY = new Color(211, 151, 43);
	private static final Color LOW_INTENSITY = new Color(94, 94, 94);

	private final JPanel content = new JPanel();
	private final WhosGrindingClanPanelConfig config;

	WhosGrindingClanPanelPanel(WhosGrindingClanPanelConfig config)
	{
		super(false);
		this.config = config;

		setLayout(new BorderLayout());
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		content.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JScrollPane scrollPane = new JScrollPane(content);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		add(scrollPane, BorderLayout.CENTER);

		rebuild();
	}

	void rebuild()
	{
		content.removeAll();
		content.add(sectionTitle("Who's grinding"));
		content.add(summaryLabel("Showing up to " + config.maxPlayersShown()
			+ " clanmates over the last " + config.activityWindowMinutes() + " minutes."));
		content.add(Box.createVerticalStrut(8));

		for (String row : sampleActivityRows(config.maxPlayersShown()))
		{
			content.add(activityRow(row));
		}

		content.add(Box.createVerticalStrut(14));
		content.add(sectionTitle("Clan grind heatmap"));
		content.add(summaryLabel(config.heatmapHistoryDays() + " day window • "
			+ config.activeHourThreshold() + "+ events = hot hour"));
		content.add(Box.createVerticalStrut(8));
		content.add(heatmapGrid());

		content.add(Box.createVerticalStrut(14));
		content.add(sectionTitle("Data status"));
		content.add(summaryLabel("Live clan roster, Wise Old Man, and TempleOSRS sync are not wired yet."));
		content.add(summaryLabel("This panel is ready for local RuneLite UI testing with placeholder rows."));

		revalidate();
		repaint();
	}

	private JLabel sectionTitle(String text)
	{
		JLabel label = new JLabel(text);
		label.setForeground(Color.WHITE);
		label.setFont(label.getFont().deriveFont(Font.BOLD, 16f));
		label.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
		return label;
	}

	private JLabel summaryLabel(String text)
	{
		JLabel label = new JLabel("<html><body style='width:190px'>" + text + "</body></html>");
		label.setForeground(Color.LIGHT_GRAY);
		label.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
		return label;
	}

	private JPanel activityRow(String text)
	{
		JPanel row = new JPanel(new BorderLayout());
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.DARK_GRAY_COLOR),
			BorderFactory.createEmptyBorder(6, 8, 6, 8)
		));

		JLabel label = new JLabel(text);
		label.setForeground(Color.WHITE);
		row.add(label, BorderLayout.CENTER);
		return row;
	}

	private JPanel heatmapGrid()
	{
		JPanel grid = new JPanel(new GridLayout(4, 6, 4, 4));
		grid.setBackground(ColorScheme.DARK_GRAY_COLOR);
		List<Integer> sampleCounts = Arrays.asList(
			0, 1, 0, 0, 2, 3,
			4, 6, 5, 2, 1, 0,
			0, 1, 3, 5, 7, 6,
			4, 3, 2, 1, 0, 0
		);

		for (int hour = 0; hour < ClanGrindHeatmapModel.HOURS_PER_DAY; hour++)
		{
			int intensity = ClanGrindHeatmapModel.intensity(sampleCounts.get(hour), config.activeHourThreshold());
			JLabel cell = new JLabel(String.format("%02d", hour), SwingConstants.CENTER);
			cell.setOpaque(true);
			cell.setForeground(Color.WHITE);
			cell.setToolTipText(String.format("%02d:00 UTC • %d%% intensity", hour, intensity));
			cell.setBackground(colorForIntensity(intensity));
			cell.setPreferredSize(new Dimension(30, 24));
			grid.add(cell);
		}

		return grid;
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

	private List<String> sampleActivityRows(int maxRows)
	{
		List<String> rows = Arrays.asList(
			WhosGrindingClanPanelPlugin.formatActivityLine("Oyama", "Mining amethyst", 12500),
			WhosGrindingClanPanelPlugin.formatActivityLine("Big Mountain", "Slayer task", 9800),
			WhosGrindingClanPanelPlugin.formatActivityLine("Clan Mate", "Vorkath KC", 4),
			WhosGrindingClanPanelPlugin.formatActivityLine("Skiller Alt", "Agility laps", 6200)
		);
		return rows.subList(0, Math.min(Math.max(maxRows, 0), rows.size()));
	}
}
