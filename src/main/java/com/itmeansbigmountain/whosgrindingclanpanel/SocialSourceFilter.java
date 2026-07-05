package com.itmeansbigmountain.whosgrindingclanpanel;

enum SocialSourceFilter
{
	FRIENDS_CHAT("Friends Chat"),
	CLAN("Clan Chat"),
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

	boolean accepts(TrackedMember member)
	{
		switch (this)
		{
			case FRIENDS:
				return member.hasSource(TrackedMemberSource.FRIEND);
			case CLAN:
				return member.hasSource(TrackedMemberSource.CLAN);
			case FRIENDS_CHAT:
				return member.hasSource(TrackedMemberSource.FRIENDS_CHAT);
			default:
				return false;
		}
	}
}
