package com.itmeansbigmountain.whosgrindingclanpanel;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Pure-Java helpers for bucketing clan XP gain events before a RuneLite panel renderer is added.
 */
final class ClanGrindHeatmapModel
{
	static final int HOURS_PER_DAY = 24;

	private ClanGrindHeatmapModel()
	{
	}

	static int hourOfDayUtc(Instant instant)
	{
		return ZonedDateTime.ofInstant(instant, ZoneOffset.UTC).getHour();
	}

	static int intensity(int eventCount, int activeHourThreshold)
	{
		if (activeHourThreshold <= 0)
		{
			throw new IllegalArgumentException("activeHourThreshold must be positive");
		}

		if (eventCount <= 0)
		{
			return 0;
		}

		return Math.min(100, (eventCount * 100) / activeHourThreshold);
	}

	static List<Integer> hourlyBuckets(List<Instant> gainEvents)
	{
		List<Integer> buckets = new ArrayList<>(Collections.nCopies(HOURS_PER_DAY, 0));
		for (Instant gainEvent : gainEvents)
		{
			int hour = hourOfDayUtc(gainEvent);
			buckets.set(hour, buckets.get(hour) + 1);
		}
		return buckets;
	}
}
