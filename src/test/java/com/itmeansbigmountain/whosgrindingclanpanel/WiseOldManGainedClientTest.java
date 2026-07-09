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
			+ "\"bosses\":{\"chambers_of_xeric\":{\"kills\":{\"gained\":70}},\"tombs_of_amascut\":{\"kills\":{\"gained\":55}},\"theatre_of_blood\":{\"kills\":{\"gained\":40}},\"zulrah\":{\"kills\":{\"gained\":12}}},"
			+ "\"activities\":{\"clue_scrolls_hard\":{\"score\":{\"gained\":3}},\"last_man_standing\":{\"score\":{\"gained\":34}},\"bounty_hunter_hunter\":{\"score\":{\"gained\":2}},\"soul_wars\":{\"score\":{\"gained\":20}}}"
			+ "}}";

		String summary = WiseOldManGainedClient.summarizeGains(json);

		assertTrue(summary.contains("<b>Skills</b>:<br>▴ Ranged: <b>+420,000 xp</b> (XP)<br>▴ Slayer: <b>+180,000 xp</b> (XP)"));
		assertTrue(summary.contains("Ranged: <b>+420,000 xp</b> (XP)"));
		assertTrue(summary.contains("Slayer: <b>+180,000 xp</b> (XP)"));
		assertTrue(summary.contains("CoX: <b>+70 kc</b> (KC)"));
		assertTrue(summary.contains("ToA: <b>+55 kc</b> (KC)"));
		assertTrue(summary.contains("ToB: <b>+40 kc</b> (KC)"));
		assertTrue(summary.contains("<b>Bosses</b>:<br>⚔ CoX: <b>+70 kc</b> (KC)<br>⚔ ToA: <b>+55 kc</b> (KC)<br>⚔ ToB: <b>+40 kc</b> (KC)"));
		assertTrue(summary.contains("<b>Activities</b>:<br>★ LMS: <b>+34 score</b> (Score)<br>★ SW: <b>+20 score</b> (Score)<br>★ Clue Scrolls Hard: <b>+3 score</b> (Score)"));
	}

	@Test
	public void reportsNoGainsWhenAllTrackedValuesAreZero()
	{
		String json = "{\"data\":{\"skills\":{\"ranged\":{\"experience\":{\"gained\":0}}},\"bosses\":{},\"activities\":{}}}";

		assertEquals("No recent gains<br>found. WOM tracking<br>was started/updated if<br>needed. Try 30/365<br>days or check<br>again after XP/KC<br>changes.", WiseOldManGainedClient.summarizeGains(json));
	}
}
