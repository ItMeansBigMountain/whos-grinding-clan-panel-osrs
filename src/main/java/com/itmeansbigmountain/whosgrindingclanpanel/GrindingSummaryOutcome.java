package com.itmeansbigmountain.whosgrindingclanpanel;

final class GrindingSummaryOutcome
{
	enum Status
	{
		POSITIVE_GAINS,
		NO_POSITIVE_GAINS,
		NOT_TRACKED,
		RATE_LIMITED,
		HTTP_ERROR,
		PARSE_ERROR
	}

	private final Status status;
	private final String summary;

	GrindingSummaryOutcome(Status status, String summary)
	{
		this.status = status;
		this.summary = summary;
	}

	Status status()
	{
		return status;
	}

	String summary()
	{
		return summary;
	}

	boolean hasPositiveGains()
	{
		return status == Status.POSITIVE_GAINS;
	}
}
