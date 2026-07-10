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
		current.set(11, "1,75,1500"); // Fishing skill row past old top-4 cap.
		current.set(3, "1,2,2"); // Strength skill row with tiny gain.
		current.set(1, "1,2,1"); // Attack skill row with tiny gain.
		current.set(19, "1,75,180000"); // Slayer skill row.
		current.set(25 + 10, "1,34"); // LMS activity row.
		current.set(25 + 7, "1,3"); // Hard clues activity row.
		current.set(25 + 12, "1,20"); // Soul Wars activity row.
		current.set(25 + 1, "1,2"); // Bounty Hunter activity row past old top-3 cap.
		current.set(25 + 16 + 40, "1,26"); // Phantom Muspah boss row.
		current.set(25 + 16 + 11, "1,70"); // CoX boss row.
		current.set(25 + 16 + 58, "1,55"); // ToA boss row.
		current.set(25 + 16 + 69, "1,12"); // Zulrah boss row past old top-3 cap.

		String summary = OfficialHiscoresGainedClient.summarizeDelta(
			OfficialHiscoresGainedClient.parseLiteCsv(current),
			OfficialHiscoresGainedClient.parseLiteCsv(baseline)
		);

		assertTrue(summary.contains("<b>Skills</b>:<br>▴ Ranged: <b>+1,234,567 xp</b>"));
		assertTrue(summary.contains("▴ Fishing: <b>+1,500 xp</b>"));
		assertTrue(summary.contains("▴ Strength: <b>+2 xp</b>"));
		assertTrue(summary.contains("▴ Attack: <b>+1 xp</b>"));
		assertTrue(summary.contains("⚔ Phantom Muspah: <b>+26 kc</b>"));
		assertTrue(summary.contains("⚔ Zulrah: <b>+12 kc</b>"));
		assertTrue(summary.contains("★ LMS: <b>+34 score</b>"));
		assertTrue(summary.contains("★ Bounty Hunter: <b>+2 score</b>"));
	}

	private static List<String> baselineRows()
	{
		List<String> rows = new ArrayList<>();
		for (int i = 0; i < 25; i++)
		{
			rows.add("-1,1,0");
		}
		for (int i = 0; i < 16; i++)
		{
			rows.add("-1,0");
		}
		for (int i = 0; i < 70; i++)
		{
			rows.add("-1,0");
		}
		return rows;
	}
}
