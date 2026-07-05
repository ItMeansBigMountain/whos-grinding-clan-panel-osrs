package com.itmeansbigmountain.whosgrindingclanpanel;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class SocialTrackerState
{
	private final List<TrackedMember> members;
	private final int ignoredCount;
	private final int maxTrackedMembers;
	private final Instant refreshedAt;
	private final List<String> messages;

	SocialTrackerState(List<TrackedMember> members, int ignoredCount, int maxTrackedMembers, Instant refreshedAt, List<String> messages)
	{
		this.members = Collections.unmodifiableList(new ArrayList<>(members));
		this.ignoredCount = ignoredCount;
		this.maxTrackedMembers = maxTrackedMembers;
		this.refreshedAt = refreshedAt;
		this.messages = Collections.unmodifiableList(new ArrayList<>(messages));
	}

	List<TrackedMember> members()
	{
		return members;
	}

	int ignoredCount()
	{
		return ignoredCount;
	}

	int maxTrackedMembers()
	{
		return maxTrackedMembers;
	}

	Instant refreshedAt()
	{
		return refreshedAt;
	}

	List<String> messages()
	{
		return messages;
	}

	List<TrackedMember> filteredMembers(SocialSourceFilter filter)
	{
		List<TrackedMember> filtered = new ArrayList<>();
		for (TrackedMember member : members)
		{
			if (filter.accepts(member))
			{
				filtered.add(member);
			}
		}
		return filtered;
	}
}
