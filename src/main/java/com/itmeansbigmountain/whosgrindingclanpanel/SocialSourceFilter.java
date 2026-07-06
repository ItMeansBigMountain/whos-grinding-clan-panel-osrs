package com.itmeansbigmountain.whosgrindingclanpanel;

enum SocialSourceFilter
{
	FRIENDS_CHAT("Friends Chat"),
	FRIENDS("Friends List");

	private final String label;

	SocialSourceFilter(String label)
	{
		this.label = label;
	}

	String label()
	{
		return label;
	}

	@Override
	public String toString()
	{
		return label;
	}

	boolean accepts(TrackedMember member)
	{
		switch (this)
		{
			case FRIENDS:
				return member.hasSource(TrackedMemberSource.FRIEND);
			case FRIENDS_CHAT:
				return member.hasSource(TrackedMemberSource.FRIENDS_CHAT);
			default:
				return false;
		}
	}
}
