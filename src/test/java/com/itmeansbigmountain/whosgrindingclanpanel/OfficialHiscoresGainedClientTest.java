package com.itmeansbigmountain.whosgrindingclanpanel;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class OfficialHiscoresGainedClientTest
{
	@Test
	public void summarizesOfficialHiscoreSkillBossAndActivityDeltas()
	{
		List<String> baseline = baselineRows();
		List<String> current = new ArrayList<>(baseline);
		current.set(5, "1,99,1234567"); // Ranged skill row.
		current.set(24 + 13, "1,34"); // LMS activity row.
		current.set(24 + 19 + 38, "1,26"); // Phantom Muspah boss row.

		String summary = OfficialHiscoresGainedClient.summarizeDelta(
			OfficialHiscoresGainedClient.parseLiteCsv(current),
			OfficialHiscoresGainedClient.parseLiteCsv(baseline)
		);

		assertTrue(summary.contains("<b>Skills</b>:<br>▴ Ranged: <b>+1,234,567 xp</b>"));
		assertTrue(summary.contains("⚔ Phantom Muspah: <b>+26 kc</b>"));
		assertTrue(summary.contains("★ LMS: <b>+34 score</b>"));
	}

	private static List<String> baselineRows()
	{
		List<String> rows = new ArrayList<>();
		for (int i = 0; i < 24; i++)
		{
			rows.add("-1,1,0");
		}
		for (int i = 0; i < 19; i++)
		{
			rows.add("-1,0");
		}
		for (int i = 0; i < 64; i++)
		{
			rows.add("-1,0");
		}
		return rows;
	}
}
