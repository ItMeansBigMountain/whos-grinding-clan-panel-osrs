package com.itmeansbigmountain.whosgrindingclanpanel;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class SocialTrackingService
{
	private final Clock clock;
	private final Map<String, TrackedMember> trackedMembers = new LinkedHashMap<>();
	private final Set<String> ignoredMemberKeys = new LinkedHashSet<>();
	private Instant refreshedAt;
	private List<String> messages = new ArrayList<>();
	private String currentPlayerName;

	SocialTrackingService()
	{
		this(Clock.systemUTC());
	}

	SocialTrackingService(Clock clock)
	{
		this.clock = clock;
		this.refreshedAt = clock.instant();
	}

	void loadIgnoredMembers(String serializedIgnoredMembers)
	{
		ignoredMemberKeys.clear();
		if (serializedIgnoredMembers == null || serializedIgnoredMembers.trim().isEmpty())
		{
			return;
		}
		for (String rawName : serializedIgnoredMembers.split("\\n"))
		{
			String key = TrackedMember.normalizeKey(rawName);
			if (!key.equals("unknown clanmate"))
			{
				ignoredMemberKeys.add(key);
			}
		}
	}

	String serializeIgnoredMembers()
	{
		return String.join("\n", ignoredMemberKeys);
	}

	void setCurrentPlayerName(String rawName)
	{
		String normalized = WhosGrindingClanPanelPlugin.normalizePlayerName(rawName);
		currentPlayerName = TrackedMember.normalizeKey(normalized).equals("unknown clanmate") ? null : normalized;
	}

	void rescan(List<SocialSourceSnapshot> snapshots, int maxTrackedMembers)
	{
		Instant now = clock.instant();
		List<String> newMessages = new ArrayList<>();
		for (SocialSourceSnapshot snapshot : snapshots)
		{
			if (!snapshot.supported())
			{
				newMessages.add(snapshot.source().label() + ": " + snapshot.message());
				continue;
			}
			for (SocialMemberSnapshot member : snapshot.members())
			{
				trackMember(member.name(), snapshot.source(), member.status(), member.world(), member.summary(), now, maxTrackedMembers);
			}
		}
		if (trackedMembers.isEmpty() && newMessages.isEmpty())
		{
			newMessages.add("No members discovered yet. Enable a source and press Rescan after logging in.");
		}
		messages = newMessages;
		refreshedAt = now;
	}

	boolean trackMember(String rawName, TrackedMemberSource source, TrackedMemberStatus status, int world, String summary, Instant now, int maxTrackedMembers)
	{
		String displayName = WhosGrindingClanPanelPlugin.normalizePlayerName(rawName);
		String key = TrackedMember.normalizeKey(displayName);
		if (key.equals("unknown clanmate") || ignoredMemberKeys.contains(key))
		{
			return false;
		}
		TrackedMember existing = trackedMembers.get(key);
		if (existing != null)
		{
			existing.observe(source, status, world, summary, now);
			return true;
		}
		if (trackedMembers.size() >= Math.max(1, maxTrackedMembers))
		{
			return false;
		}
		TrackedMember member = new TrackedMember(displayName, source, now);
		member.observe(source, status, world, summary, now);
		trackedMembers.put(key, member);
		return true;
	}

	boolean removeMember(String rawName)
	{
		String key = TrackedMember.normalizeKey(rawName);
		TrackedMember removed = trackedMembers.remove(key);
		if (removed != null)
		{
			ignoredMemberKeys.add(key);
			refreshedAt = clock.instant();
			return true;
		}
		return false;
	}

	void clearIgnoredMembers()
	{
		ignoredMemberKeys.clear();
	}

	SocialTrackerState snapshot(int maxTrackedMembers)
	{
		List<TrackedMember> copies = new ArrayList<>();
		for (TrackedMember member : trackedMembers.values())
		{
			copies.add(member.copy());
		}
		copies.sort(Comparator
			.comparing((TrackedMember member) -> member.status() == TrackedMemberStatus.ONLINE ? 0 : member.status() == TrackedMemberStatus.UNKNOWN ? 1 : 2)
			.thenComparing(TrackedMember::displayName));
		return new SocialTrackerState(copies, ignoredMemberKeys.size(), maxTrackedMembers, refreshedAt, messages, currentPlayerName);
	}

	static List<SocialSourceSnapshot> seedSnapshots(boolean trackFriends, boolean trackClan, boolean trackFriendsChat)
	{
		List<SocialSourceSnapshot> snapshots = new ArrayList<>();
		if (trackFriends)
		{
			snapshots.add(SocialSourceSnapshot.unsupported(TrackedMemberSource.FRIEND, "Friends list scanner is waiting for live RuneLite API wiring."));
		}
		if (trackClan)
		{
			snapshots.add(SocialSourceSnapshot.unsupported(TrackedMemberSource.CLAN, "Clan chat scanner is waiting for live RuneLite API wiring."));
		}
		if (trackFriendsChat)
		{
			snapshots.add(SocialSourceSnapshot.unsupported(TrackedMemberSource.FRIENDS_CHAT, "Friends chat scanner is waiting for live RuneLite API wiring."));
		}
		if (snapshots.isEmpty())
		{
			snapshots.add(SocialSourceSnapshot.unsupported(TrackedMemberSource.FRIEND, "All tracking sources are disabled in config."));
		}
		return snapshots;
	}
}
