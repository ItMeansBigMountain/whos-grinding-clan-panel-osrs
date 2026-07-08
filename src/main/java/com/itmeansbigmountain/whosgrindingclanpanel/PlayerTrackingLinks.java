package com.itmeansbigmountain.whosgrindingclanpanel;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

final class PlayerTrackingLinks
{
	private static final String WISE_OLD_MAN_BASE_URL = "https://wiseoldman.net/players/";

	private PlayerTrackingLinks()
	{
		// Utility class.
	}

	static String wiseOldManGainedUrl(String playerName, GainsPeriod gainsPeriod)
	{
		GainsPeriod period = gainsPeriod == null ? GainsPeriod.SEVEN_DAYS : gainsPeriod;
		return WISE_OLD_MAN_BASE_URL + urlEncode(WhosGrindingClanPanelPlugin.normalizePlayerName(playerName))
			+ "/gained?period=" + period.wiseOldManPeriod();
	}

	private static String urlEncode(String value)
	{
		try
		{
			return URLEncoder.encode(value, "UTF-8");
		}
		catch (UnsupportedEncodingException ex)
		{
			throw new IllegalStateException("UTF-8 is required by the JVM", ex);
		}
	}
}
