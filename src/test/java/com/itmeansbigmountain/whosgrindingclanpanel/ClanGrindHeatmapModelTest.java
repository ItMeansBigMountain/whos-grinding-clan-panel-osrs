package com.itmeansbigmountain.whosgrindingclanpanel;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class ClanGrindHeatmapModelTest
{
	@Test
	public void modelBucketsGainEventsByUtcHour()
	{
		List<Instant> events = Arrays.asList(
			Instant.parse("2026-05-29T00:15:00Z"),
			Instant.parse("2026-05-29T00:45:00Z"),
			Instant.parse("2026-05-29T13:05:00Z")
		);

		List<Integer> buckets = ClanGrindHeatmapModel.hourlyBuckets(events);

		assertEquals(24, buckets.size());
		assertEquals(Integer.valueOf(2), buckets.get(0));
		assertEquals(Integer.valueOf(1), buckets.get(13));
		assertEquals(Integer.valueOf(0), buckets.get(23));
	}

	@Test
	public void modelCalculatesCappedIntensity()
	{
		assertArrayEquals(new int[] {0, 33, 100, 100}, new int[] {
			ClanGrindHeatmapModel.intensity(0, 3),
			ClanGrindHeatmapModel.intensity(1, 3),
			ClanGrindHeatmapModel.intensity(3, 3),
			ClanGrindHeatmapModel.intensity(10, 3)
		});
	}

	@Test(expected = IllegalArgumentException.class)
	public void modelRejectsInvalidThreshold()
	{
		ClanGrindHeatmapModel.intensity(1, 0);
	}
}
