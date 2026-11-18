package com.itmeansbigmountain.whosgrindingclanpanel;

import java.io.IOException;

@FunctionalInterface
interface GrindingSummaryClient
{
	String fetchGrindingSummary(String playerName, GainsPeriod period) throws IOException;

	default GrindingSummaryOutcome fetchOutcome(String playerName, GainsPeriod period) throws IOException
	{
		String summary = fetchGrindingSummary(playerName, period);
		GrindingSummaryOutcome.Status status = summary != null && !summary.startsWith("No recent gains") && !summary.startsWith("Player not on")
			? GrindingSummaryOutcome.Status.POSITIVE_GAINS
			: GrindingSummaryOutcome.Status.NO_POSITIVE_GAINS;
		return new GrindingSummaryOutcome(status, summary);
	}
}
