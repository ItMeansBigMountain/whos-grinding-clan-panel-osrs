package com.itmeansbigmountain.whosgrindingclanpanel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class SocialSourceSnapshot
{
	private final TrackedMemberSource source;
	private final boolean supported;
	private final String message;
	private final List<SocialMemberSnapshot> members;

	SocialSourceSnapshot(TrackedMemberSource source, boolean supported, String message, List<SocialMemberSnapshot> members)
	{
		this.source = source;
		this.supported = supported;
		this.message = message;
		this.members = Collections.unmodifiableList(new ArrayList<>(members));
	}

	static SocialSourceSnapshot unsupported(TrackedMemberSource source, String message)
	{
		return new SocialSourceSnapshot(source, false, message, Collections.emptyList());
	}

	static SocialSourceSnapshot members(TrackedMemberSource source, List<String> memberNames)
	{
		List<SocialMemberSnapshot> memberSnapshots = new ArrayList<>();
		for (String memberName : memberNames)
		{
			memberSnapshots.add(new SocialMemberSnapshot(memberName, TrackedMemberStatus.UNKNOWN, -1, "Seen in " + source.label()));
		}
		return new SocialSourceSnapshot(source, true, "", memberSnapshots);
	}

	static SocialSourceSnapshot observedMembers(TrackedMemberSource source, List<SocialMemberSnapshot> members)
	{
		return new SocialSourceSnapshot(source, true, "", members);
	}

	TrackedMemberSource source()
	{
		return source;
	}

	boolean supported()
	{
		return supported;
	}

	String message()
	{
		return message;
	}

	List<SocialMemberSnapshot> members()
	{
		return members;
	}
}
