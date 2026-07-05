package com.itmeansbigmountain.whosgrindingclanpanel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class SocialSourceSnapshot
{
	private final TrackedMemberSource source;
	private final boolean supported;
	private final String message;
	private final List<String> memberNames;

	SocialSourceSnapshot(TrackedMemberSource source, boolean supported, String message, List<String> memberNames)
	{
		this.source = source;
		this.supported = supported;
		this.message = message;
		this.memberNames = Collections.unmodifiableList(new ArrayList<>(memberNames));
	}

	static SocialSourceSnapshot unsupported(TrackedMemberSource source, String message)
	{
		return new SocialSourceSnapshot(source, false, message, Collections.emptyList());
	}

	static SocialSourceSnapshot members(TrackedMemberSource source, List<String> memberNames)
	{
		return new SocialSourceSnapshot(source, true, "", memberNames);
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

	List<String> memberNames()
	{
		return memberNames;
	}
}
