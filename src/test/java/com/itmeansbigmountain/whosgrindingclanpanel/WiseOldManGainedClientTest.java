package com.itmeansbigmountain.whosgrindingclanpanel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class WiseOldManGainedClientTest
{
	@Test
	public void summarizesTopPeriodGainsAcrossSkillsBossesAndActivities()
	{
		String json = "{\"data\":{" 
			+ "\"skills\":{"
			+ "\"overall\":{\"experience\":{\"gained\":999999}},"
			+ "\"ranged\":{\"experience\":{\"gained\":420000}},"
			+ "\"slayer\":{\"experience\":{\"gained\":180000}}"
			+ "},"
			+ "\"bosses\":{\"zulrah\":{\"kills\":{\"gained\":43}},\"scurrius\":{\"kills\":{\"gained\":12}}},"
			+ "\"activities\":{\"clue_scrolls_hard\":{\"score\":{\"gained\":3}},\"last_man_standing\":{\"score\":{\"gained\":34}},\"bounty_hunter_hunter\":{\"score\":{\"gained\":2}}}"
			+ "}}";

		String summary = WiseOldManGainedClient.summarizeGains(json);

		assertTrue(summary.contains("Ranged: +420,000 xp (XP)"));
		assertTrue(summary.contains("Slayer: +180,000 xp (XP)"));
		assertTrue(summary.contains("Zulrah: +43 kc (KC)"));
		assertTrue(summary.contains("<b>Bosses</b>:<br>Zulrah: +43 kc (KC)<br>Scurrius: +12 kc (KC)"));
		assertTrue(summary.contains("<b>Activities</b>:<br>LMS: +34 score (Score)<br>Clue Scrolls Hard: +3 score (Score)<br>Bounty Hunter: +2 score (Score)"));
	}

	@Test
	public void reportsNoGainsWhenAllTrackedValuesAreZero()
	{
		String json = "{\"data\":{\"skills\":{\"ranged\":{\"experience\":{\"gained\":0}}},\"bosses\":{},\"activities\":{}}}";

		assertEquals("No tracked XP/KC/score gains found for this period.", WiseOldManGainedClient.summarizeGains(json));
	}
}
