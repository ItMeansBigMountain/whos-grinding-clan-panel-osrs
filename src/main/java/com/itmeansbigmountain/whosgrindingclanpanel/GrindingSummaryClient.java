package com.itmeansbigmountain.whosgrindingclanpanel;

import java.io.IOException;

@FunctionalInterface
interface GrindingSummaryClient
{
	String fetchGrindingSummary(String playerName, GainsPeriod period) throws IOException;
}
