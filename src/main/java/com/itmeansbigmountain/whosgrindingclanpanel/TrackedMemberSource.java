package com.itmeansbigmountain.whosgrindingclanpanel;

enum TrackedMemberSource
{
	FRIEND("Friend"),
	CLAN("Clan"),
	FRIENDS_CHAT("Friends chat");

	private final String label;

	TrackedMemberSource(String label)
	{
		this.label = label;
	}

	String label()
	{
		return label;
	}
}
