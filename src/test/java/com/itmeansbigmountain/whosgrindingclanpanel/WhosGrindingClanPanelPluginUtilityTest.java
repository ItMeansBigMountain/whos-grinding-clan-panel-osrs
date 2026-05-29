package com.itmeansbigmountain.whosgrindingclanpanel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class WhosGrindingClanPanelPluginUtilityTest
{
	@Test
	public void buildLoginHintUsesConfiguredBounds()
	{
		String hint = WhosGrindingClanPanelPlugin.buildLoginHint(45, 12);

		assertTrue(hint.contains("up to 12 clanmates"));
		assertTrue(hint.contains("last 45 minutes"));
	}

	@Test
	public void buildLoginHintFallsBackWhenOutOfRange()
	{
		String hint = WhosGrindingClanPanelPlugin.buildLoginHint(1, 99);

		assertTrue(hint.contains("up to " + WhosGrindingClanPanelPlugin.DEFAULT_MAX_PLAYERS_SHOWN + " clanmates"));
		assertTrue(hint.contains("last " + WhosGrindingClanPanelPlugin.DEFAULT_ACTIVITY_WINDOW_MINUTES + " minutes"));
	}

	@Test
	public void formatActivityLineNormalizesInputs()
	{
		String line = WhosGrindingClanPanelPlugin.formatActivityLine("  Oyama\u00A0  Alt  ", "  Mining   amethyst ", 12500);

		assertEquals("Oyama Alt - Mining amethyst (12500 xp gained)", line);
	}

	@Test
	public void formatActivityLineHandlesBlankInputs()
	{
		String line = WhosGrindingClanPanelPlugin.formatActivityLine(" ", "", -50);

		assertEquals("Unknown clanmate - training (0 xp gained)", line);
	}
}
