package com.itmeansbigmountain.whosgrindingclanpanel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.PluginPanel;
import org.junit.Test;

public class WhosGrindingClanPanelPanelFallbackTest
{
	@Test
	public void validWomGainsStayPrimaryAndOfficialSnapshotIsCaptured()
	{
		AtomicInteger officialCalls = new AtomicInteger();
		WhosGrindingClanPanelPanel panel = panel(
			(player, period) -> "<b>Skills</b>:<br>▴ Attack: <b>+100 xp</b>",
			(player, period) -> {
				officialCalls.incrementAndGet();
				return "Fallback baseline saved";
			}
		);

		String result = panel.testFetchConfiguredSummary("Oyama");

		assertTrue(result.contains("Tracker API: Wise Old Man"));
		assertTrue(result.contains("+100 xp"));
		assertFalse(result.contains("Fallback: Official"));
		assertEquals(1, officialCalls.get());
	}

	@Test
	public void womNoPositiveGainsUsesOfficialBaselineAndThenVisibleDelta() throws Exception
	{
		AtomicInteger scan = new AtomicInteger();
		WhosGrindingClanPanelPanel panel = panel(
			(player, period) -> "No recent gains<br>found.",
			(player, period) -> scan.getAndIncrement() == 0
				? "Fallback baseline<br>saved from official<br>OSRS hiscores."
				: "<b>Skills</b>:<br>▴ Attack: <b>+250 xp</b><br><b>Bosses</b>:<br>⚔ Zulrah: <b>+4 kc</b><br><b>Activities</b>:<br>★ LMS: <b>+9 score</b>"
		);

		String first = panel.testFetchConfiguredSummary("Test Player");
		String second = panel.testFetchConfiguredSummary("Test Player");

		assertTrue(first.contains("Fallback baseline"));
		assertTrue(first.contains("Official fallback"));
		assertFalse(first.contains("Tracker API: Wise Old Man"));
		assertTrue(second.contains("+250 xp"));
		assertTrue(second.contains("+4 kc"));
		assertTrue(second.contains("+9 score"));
		showSummary(panel, "Test Player", second);
		String visibleText = visibleText(panel);
		assertTrue(visibleText.contains("+250 xp"));
		assertTrue(visibleText.contains("+4 kc"));
		assertTrue(visibleText.contains("+9 score"));
		Path render = Path.of("build", "reports", "official-hiscores-delta-panel.png");
		Files.createDirectories(render.getParent());
		renderPanel(panel, render);
		assertTrue(Files.size(render) > 0);
	}

	@Test
	public void officialFailureIsTruthfulAndDoesNotFabricateGains()
	{
		WhosGrindingClanPanelPanel panel = panel(
			(player, period) -> { throw new java.io.IOException("WOM unavailable"); },
			(player, period) -> { throw new java.io.IOException("hiscores unavailable"); }
		);

		String result = panel.testFetchConfiguredSummary("Test Player");

		assertTrue(result.contains("Official hiscores unavailable"));
		assertFalse(result.contains("+"));
	}

	private static WhosGrindingClanPanelPanel panel(GrindingSummaryClient wom, GrindingSummaryClient official)
	{
		WhosGrindingClanPanelConfig config = new WhosGrindingClanPanelConfig()
		{
			@Override
			public GainsPeriod gainsPeriod()
			{
				return GainsPeriod.DAY;
			}

			@Override
			public GainDataSource gainDataSource()
			{
				return GainDataSource.TRACKER_APIS;
			}
		};
		SocialTrackerState state = new SocialTrackerState(Collections.emptyList(), 0, 100, Instant.EPOCH, Collections.emptyList(), "Test Player");
		WhosGrindingClanPanelPanel.PanelActions actions = new WhosGrindingClanPanelPanel.PanelActions()
		{
			@Override public void refreshRequested() { }
			@Override public void gainsPeriodChanged(GainsPeriod gainsPeriod) { }
			@Override public void showOfflineFriendsChanged(boolean showOfflineFriends) { }
		};
		return new WhosGrindingClanPanelPanel(config, state, actions, wom, official, Runnable::run);
	}

	@SuppressWarnings("unchecked")
	private static void showSummary(WhosGrindingClanPanelPanel panel, String playerName, String summary) throws Exception
	{
		Field cacheField = WhosGrindingClanPanelPanel.class.getDeclaredField("grindingSummaryCache");
		cacheField.setAccessible(true);
		Map<String, String> cache = (Map<String, String>) cacheField.get(panel);
		cache.put("test player:day:TRACKER_APIS", summary);
		Field selectedField = WhosGrindingClanPanelPanel.class.getDeclaredField("selectedPlayerName");
		selectedField.setAccessible(true);
		selectedField.set(panel, playerName);
		panel.rebuild();
	}

	private static String visibleText(Container container)
	{
		StringBuilder text = new StringBuilder();
		for (Component component : container.getComponents())
		{
			if (component instanceof JLabel)
			{
				text.append(((JLabel) component).getText());
			}
			if (component instanceof Container)
			{
				text.append(visibleText((Container) component));
			}
		}
		return text.toString();
	}

	private static void renderPanel(JPanel panel, Path path) throws Exception
	{
		panel.setSize(new Dimension(PluginPanel.PANEL_WIDTH, 720));
		layoutRecursively(panel);
		BufferedImage image = new BufferedImage(PluginPanel.PANEL_WIDTH, 720, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		panel.paint(graphics);
		graphics.dispose();
		ImageIO.write(image, "png", path.toFile());
	}

	private static void layoutRecursively(Container container)
	{
		container.doLayout();
		for (Component child : container.getComponents())
		{
			if (child instanceof Container)
			{
				layoutRecursively((Container) child);
			}
		}
	}
}
