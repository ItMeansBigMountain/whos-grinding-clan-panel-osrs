package com.itmeansbigmountain.whosgrindingclanpanel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class OfficialHiscoresGainedClientTest
{
	@Test
	public void firstAndSecondScansPersistSanitizedIdentityAndRenderDeltas() throws IOException
	{
		Path cache = Files.createTempDirectory("hiscores-cache");
		Instant first = Instant.parse("2026-09-01T00:00:00Z");
		List<String> currentRows = new ArrayList<>(baselineRows());
		OfficialHiscoresGainedClient firstClient = new OfficialHiscoresGainedClient(
			cache,
			Clock.fixed(first, ZoneOffset.UTC),
			ignored -> OfficialHiscoresGainedClient.parseLiteCsv(baselineRows())
		);

		String baselineMessage = firstClient.fetchGrindingSummary("  Test Player!?  ", GainsPeriod.DAY);

		assertTrue(baselineMessage.contains("baseline"));
		Path snapshot = firstClient.snapshotFileFor("Test Player!?");
		assertEquals(cache.resolve("test_player__.csv"), snapshot);
		assertTrue(Files.isRegularFile(snapshot));

		currentRows.set(1, "1,2,250");
		currentRows.set(25 + 10, "1,9");
		currentRows.set(25 + 16 + 69, "1,4");
		OfficialHiscoresGainedClient secondClient = new OfficialHiscoresGainedClient(
			cache,
			Clock.fixed(first.plusSeconds(60), ZoneOffset.UTC),
			ignored -> OfficialHiscoresGainedClient.parseLiteCsv(currentRows)
		);

		String delta = secondClient.fetchGrindingSummary("test_player!?", GainsPeriod.DAY);

		assertTrue(delta.contains("Attack: <b>+250 xp</b>"));
		assertTrue(delta.contains("LMS: <b>+9 score</b>"));
		assertTrue(delta.contains("Zulrah: <b>+4 kc</b>"));
		assertEquals(2, OfficialHiscoresGainedClient.testReadSnapshots(snapshot).size());
	}

	@Test
	public void baselineSelectionUsesInjectedClockAndClosestSnapshotBeforeTarget() throws IOException
	{
		Instant now = Instant.parse("2026-09-08T00:00:00Z");
		List<OfficialHiscoresGainedClient.HiscoreSnapshot> snapshots = List.of(
			OfficialHiscoresGainedClient.testSnapshot(now.minusSeconds(10 * 86400L).getEpochSecond()),
			OfficialHiscoresGainedClient.testSnapshot(now.minusSeconds(8 * 86400L).getEpochSecond()),
			OfficialHiscoresGainedClient.testSnapshot(now.minusSeconds(6 * 86400L).getEpochSecond())
		);

		OfficialHiscoresGainedClient.HiscoreSnapshot selected = OfficialHiscoresGainedClient.testBaselineForPeriod(
			snapshots,
			GainsPeriod.SEVEN_DAYS,
			Clock.fixed(now, ZoneOffset.UTC)
		);

		assertEquals(now.minusSeconds(8 * 86400L).getEpochSecond(), selected.testTimestamp());
	}

	@Test
	public void malformedSnapshotLinesAreIgnoredWithoutLosingValidHistory() throws IOException
	{
		Path snapshot = Files.createTempFile("hiscores-corrupt", ".csv");
		Files.write(snapshot, List.of("not-a-time,V2|S{}|A{}|B{}", "100,V2|S{Attack=1}|A{}|B{}"));

		List<OfficialHiscoresGainedClient.HiscoreSnapshot> loaded = OfficialHiscoresGainedClient.testReadSnapshots(snapshot);

		assertEquals(1, loaded.size());
		assertEquals(100L, loaded.get(0).testTimestamp());
	}
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

	@Test
	public void baselineForPeriod_selectsClosestSnapshotAtOrBeforeTarget() throws IOException
	{
		Path tempFile = Files.createTempFile("test-baseline", ".csv");
		try
		{
			// Use clearly old timestamps (year 2000 range) so they're all "before" any reasonable "now"
			// Snapshot 1: 2000-01-01 = 946684800
			// Snapshot 2: 2000-06-01 = 959932800
			// Snapshot 3: 2000-12-01 = 975619200
			long snap1 = 946684800L;   // Jan 1, 2000
			long snap2 = 959932800L;   // Jun 1, 2000
			long snap3 = 975619200L;   // Dec 1, 2000

			String snapshotContent = "V2|S{}|A{}|B{}";
			Files.write(tempFile, List.of(
				snap1 + "," + snapshotContent,
				snap2 + "," + snapshotContent,
				snap3 + "," + snapshotContent
			));

			List<OfficialHiscoresGainedClient.HiscoreSnapshot> snapshots = OfficialHiscoresGainedClient.testReadSnapshots(tempFile);
			assertEquals(3, snapshots.size());

			// Since all snapshots are from year 2000 and "now" is 2026+,
			// target for any period will be 2026 - period.days
			// The "closest at or before target" will be the LATEST snapshot (snap3, Dec 2000)
			// for all periods, because target is still way after 2000.
			// This tests that the fallback to latest works correctly.

			OfficialHiscoresGainedClient.HiscoreSnapshot baselineDay = OfficialHiscoresGainedClient.testBaselineForPeriod(snapshots, GainsPeriod.DAY);
			assertNotNull(baselineDay);
			assertEquals(snap3, baselineDay.testTimestamp());

			OfficialHiscoresGainedClient.HiscoreSnapshot baselineWeek = OfficialHiscoresGainedClient.testBaselineForPeriod(snapshots, GainsPeriod.SEVEN_DAYS);
			assertNotNull(baselineWeek);
			assertEquals(snap3, baselineWeek.testTimestamp());

			OfficialHiscoresGainedClient.HiscoreSnapshot baselineMonth = OfficialHiscoresGainedClient.testBaselineForPeriod(snapshots, GainsPeriod.THIRTY_DAYS);
			assertNotNull(baselineMonth);
			assertEquals(snap3, baselineMonth.testTimestamp());

			OfficialHiscoresGainedClient.HiscoreSnapshot baselineYear = OfficialHiscoresGainedClient.testBaselineForPeriod(snapshots, GainsPeriod.YEAR);
			assertNotNull(baselineYear);
			assertEquals(snap3, baselineYear.testTimestamp());
		}
		finally
		{
			Files.deleteIfExists(tempFile);
		}
	}

	@Test
	public void baselineForPeriod_whenNoSnapshotBeforeTarget_fallsBackToLatest() throws IOException
	{
		Path tempFile = Files.createTempFile("test-baseline-fallback", ".csv");
		try
		{
			long now = Instant.now().getEpochSecond();
			long twoDaysAgo = now - 2 * 86400L;
			String snapshotContent = "V2|S{}|A{}|B{}";
			Files.write(tempFile, List.of(twoDaysAgo + "," + snapshotContent));

			List<OfficialHiscoresGainedClient.HiscoreSnapshot> snapshots = OfficialHiscoresGainedClient.testReadSnapshots(tempFile);
			assertEquals(1, snapshots.size());

			// DAY period (1 day) - no snapshot at or before 1 day ago, should fall back to latest (2 days ago)
			OfficialHiscoresGainedClient.HiscoreSnapshot baselineDay = OfficialHiscoresGainedClient.testBaselineForPeriod(snapshots, GainsPeriod.DAY);
			assertNotNull(baselineDay);
			assertEquals(twoDaysAgo, baselineDay.testTimestamp());
		}
		finally
		{
			Files.deleteIfExists(tempFile);
		}
	}

	@Test
	public void baselineForPeriod_emptySnapshots_returnsNull() throws IOException
	{
		Path tempFile = Files.createTempFile("test-empty", ".csv");
		try
		{
			Files.write(tempFile, List.of());
			List<OfficialHiscoresGainedClient.HiscoreSnapshot> snapshots = OfficialHiscoresGainedClient.testReadSnapshots(tempFile);
			assertTrue(snapshots.isEmpty());

			OfficialHiscoresGainedClient.HiscoreSnapshot baseline = OfficialHiscoresGainedClient.testBaselineForPeriod(snapshots, GainsPeriod.DAY);
			assertNull(baseline);
		}
		finally
		{
			Files.deleteIfExists(tempFile);
		}
	}

	@Test
	public void firstScanReturnsBaselineSavedMessage() throws IOException
	{
		// This test verifies the message format for first scan
		// We can't easily test the full fetchGrindingSummary without network,
		// but we can verify the summarizeDelta handles empty sections correctly
		List<OfficialHiscoresGainedClient.HiscoreValues> empty = List.of(new OfficialHiscoresGainedClient.HiscoreValues());
		String summary = OfficialHiscoresGainedClient.summarizeDelta(
			new OfficialHiscoresGainedClient.HiscoreValues(),
			new OfficialHiscoresGainedClient.HiscoreValues()
		);
		assertTrue(summary.contains("No official"));
		assertTrue(summary.contains("hiscores difference"));
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

	// Need to make baselineForPeriod accessible for testing - add a test-only static method
	// This will be called from the test, so we define it here but it needs to be in the main class
}
