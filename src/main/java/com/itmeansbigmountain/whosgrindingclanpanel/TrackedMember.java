package com.itmeansbigmountain.whosgrindingclanpanel;

import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

final class TrackedMember
{
	private final String displayName;
	private final String key;
	private final EnumSet<TrackedMemberSource> sources;
	private TrackedMemberStatus status;
	private final Instant firstSeen;
	private Instant lastSeen;
	private Instant lastStatusChange;
	private int lastWorld;
	private String activitySummary;

	TrackedMember(String displayName, TrackedMemberSource source, Instant now)
	{
		this.displayName = WhosGrindingClanPanelPlugin.normalizePlayerName(displayName);
		this.key = normalizeKey(displayName);
		this.sources = EnumSet.of(source);
		this.status = TrackedMemberStatus.UNKNOWN;
		this.firstSeen = now;
		this.lastSeen = now;
		this.lastStatusChange = now;
		this.lastWorld = -1;
		this.activitySummary = "Discovered from " + source.label();
	}

	static String normalizeKey(String name)
	{
		return WhosGrindingClanPanelPlugin.normalizePlayerName(name).toLowerCase();
	}

	String displayName()
	{
		return displayName;
	}

	String key()
	{
		return key;
	}

	Set<TrackedMemberSource> sources()
	{
		return Collections.unmodifiableSet(sources);
	}

	boolean hasSource(TrackedMemberSource source)
	{
		return sources.contains(source);
	}

	TrackedMemberStatus status()
	{
		return status;
	}

	Instant firstSeen()
	{
		return firstSeen;
	}

	Instant lastSeen()
	{
		return lastSeen;
	}

	Instant lastStatusChange()
	{
		return lastStatusChange;
	}

	int lastWorld()
	{
		return lastWorld;
	}

	String activitySummary()
	{
		return activitySummary;
	}

	void observe(TrackedMemberSource source, TrackedMemberStatus newStatus, int world, String summary, Instant now)
	{
		sources.add(source);
		lastSeen = now;
		if (newStatus != null && newStatus != status)
		{
			status = newStatus;
			lastStatusChange = now;
		}
		if (world > 0)
		{
			lastWorld = world;
		}
		if (summary != null && !summary.trim().isEmpty())
		{
			activitySummary = summary.trim();
		}
	}

	TrackedMember copy()
	{
		TrackedMember copy = new TrackedMember(displayName, sources.iterator().next(), firstSeen);
		copy.sources.clear();
		copy.sources.addAll(sources);
		copy.status = status;
		copy.lastSeen = lastSeen;
		copy.lastStatusChange = lastStatusChange;
		copy.lastWorld = lastWorld;
		copy.activitySummary = activitySummary;
		return copy;
	}

	@Override
	public boolean equals(Object other)
	{
		if (this == other)
		{
			return true;
		}
		if (!(other instanceof TrackedMember))
		{
			return false;
		}
		TrackedMember that = (TrackedMember) other;
		return key.equals(that.key);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(key);
	}
}
