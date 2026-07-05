package com.itmeansbigmountain.whosgrindingclanpanel;

enum TrackedMemberStatus
{
	ONLINE("Online"),
	OFFLINE("Offline"),
	UNKNOWN("Unknown");

	private final String label;

	TrackedMemberStatus(String label)
	{
		this.label = label;
	}

	String label()
	{
		return label;
	}
}
