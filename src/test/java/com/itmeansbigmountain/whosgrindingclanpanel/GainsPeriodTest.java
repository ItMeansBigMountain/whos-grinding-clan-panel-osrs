package com.itmeansbigmountain.whosgrindingclanpanel;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class GainsPeriodTest
{
	@Test
	public void exposesUserFriendlyDayWindows()
	{
		assertEquals(1, GainsPeriod.DAY.days());
		assertEquals("Day", GainsPeriod.DAY.label());
		assertEquals("day", GainsPeriod.DAY.wiseOldManPeriod());

		assertEquals(7, GainsPeriod.SEVEN_DAYS.days());
		assertEquals("7 days", GainsPeriod.SEVEN_DAYS.label());
		assertEquals("week", GainsPeriod.SEVEN_DAYS.wiseOldManPeriod());

		assertEquals(30, GainsPeriod.THIRTY_DAYS.days());
		assertEquals("30 days", GainsPeriod.THIRTY_DAYS.label());
		assertEquals("month", GainsPeriod.THIRTY_DAYS.wiseOldManPeriod());

		assertEquals(365, GainsPeriod.YEAR.days());
		assertEquals("365 days", GainsPeriod.YEAR.label());
		assertEquals("year", GainsPeriod.YEAR.wiseOldManPeriod());
	}

	@Test
	public void rendersLabelsInConfigDropdown()
	{
		assertEquals("7 days", GainsPeriod.SEVEN_DAYS.toString());
	}
}
