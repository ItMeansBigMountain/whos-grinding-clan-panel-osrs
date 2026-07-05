package com.itmeansbigmountain.whosgrindingclanpanel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

public class SocialTrackingServiceTest
{
	private final Clock fixedClock = Clock.fixed(Instant.parse("2026-07-05T12:00:00Z"), ZoneOffset.UTC);

	@Test
	public void rescanMergesSameMemberAcrossSources()
	{
		SocialTrackingService service = new SocialTrackingService(fixedClock);

		service.rescan(Arrays.asList(
			SocialSourceSnapshot.members(TrackedMemberSource.FRIEND, Collections.singletonList(" Oyama ")),
			SocialSourceSnapshot.members(TrackedMemberSource.CLAN, Collections.singletonList("Oyama"))
		), 10);

		SocialTrackerState state = service.snapshot(10);
		assertEquals(1, state.members().size());
		assertTrue(state.members().get(0).hasSource(TrackedMemberSource.FRIEND));
		assertTrue(state.members().get(0).hasSource(TrackedMemberSource.CLAN));
	}

	@Test
	public void removeMemberIgnoresFutureScans()
	{
		SocialTrackingService service = new SocialTrackingService(fixedClock);
		service.rescan(Collections.singletonList(
			SocialSourceSnapshot.members(TrackedMemberSource.FRIEND, Collections.singletonList("Skiller Alt"))
		), 10);

		assertTrue(service.removeMember("skiller alt"));
		service.rescan(Collections.singletonList(
			SocialSourceSnapshot.members(TrackedMemberSource.FRIEND, Collections.singletonList("Skiller Alt"))
		), 10);

		assertEquals(0, service.snapshot(10).members().size());
		assertEquals(1, service.snapshot(10).ignoredCount());
		assertFalse(service.serializeIgnoredMembers().isEmpty());
	}

	@Test
	public void maxTrackedMembersCapsNewEntries()
	{
		SocialTrackingService service = new SocialTrackingService(fixedClock);
		service.rescan(Collections.singletonList(
			SocialSourceSnapshot.members(TrackedMemberSource.CLAN, Arrays.asList("One", "Two", "Three"))
		), 2);

		assertEquals(2, service.snapshot(2).members().size());
	}

	@Test
	public void defaultSourceSnapshotsDoNotSeedFakeMembers()
	{
		SocialTrackingService service = new SocialTrackingService(fixedClock);

		service.rescan(SocialTrackingService.seedSnapshots(true, true, true), 100);

		SocialTrackerState state = service.snapshot(100);
		assertEquals(0, state.members().size());
		assertEquals(3, state.messages().size());
	}
}
