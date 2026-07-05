package com.itmeansbigmountain.whosgrindingclanpanel;

final class SocialMemberSnapshot
{
	private final String name;
	private final TrackedMemberStatus status;
	private final int world;
	private final String summary;

	SocialMemberSnapshot(String name, TrackedMemberStatus status, int world, String summary)
	{
		this.name = name;
		this.status = status;
		this.world = world;
		this.summary = summary;
	}

	static SocialMemberSnapshot of(String name, int world, String summary)
	{
		TrackedMemberStatus status = world > 0 ? TrackedMemberStatus.ONLINE : TrackedMemberStatus.OFFLINE;
		return new SocialMemberSnapshot(name, status, world, summary);
	}

	String name()
	{
		return name;
	}

	TrackedMemberStatus status()
	{
		return status;
	}

	int world()
	{
		return world;
	}

	String summary()
	{
		return summary;
	}
}
