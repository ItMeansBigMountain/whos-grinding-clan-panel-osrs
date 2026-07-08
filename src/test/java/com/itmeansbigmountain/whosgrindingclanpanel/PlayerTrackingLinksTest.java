package com.itmeansbigmountain.whosgrindingclanpanel;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PlayerTrackingLinksTest
{
	@Test
	public void buildsWiseOldManGainedLinksForConfiguredPeriod()
	{
		assertEquals(
			"https://wiseoldman.net/players/Oyama/gained?period=week",
			PlayerTrackingLinks.wiseOldManGainedUrl(" Oyama ", GainsPeriod.SEVEN_DAYS)
		);
	}

	@Test
	public void encodesNamesWithSpaces()
	{
		assertEquals(
			"https://wiseoldman.net/players/Skiller+Alt/gained?period=month",
			PlayerTrackingLinks.wiseOldManGainedUrl("Skiller Alt", GainsPeriod.THIRTY_DAYS)
		);
	}

	@Test
	public void buildsTemplePlayerLinks()
	{
		assertEquals(
			"https://templeosrs.com/player/overview.php?player=Skiller+Alt",
			PlayerTrackingLinks.templePlayerUrl("Skiller Alt")
		);
	}

	@Test
	public void buildsOfficialHiscoreLinks()
	{
		assertEquals(
			"https://secure.runescape.com/m=hiscore_oldschool/overall?table=0&user=Oyama",
			PlayerTrackingLinks.officialHiscoreUrl("Oyama")
		);
	}
}
