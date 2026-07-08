package com.itmeansbigmountain.whosgrindingclanpanel;

enum GainsPeriod
{
	DAY("Day", 1, "day"),
	SEVEN_DAYS("7 days", 7, "week"),
	THIRTY_DAYS("30 days", 30, "month"),
	YEAR("365 days", 365, "year");

	private final String label;
	private final int days;
	private final String wiseOldManPeriod;

	GainsPeriod(String label, int days, String wiseOldManPeriod)
	{
		this.label = label;
		this.days = days;
		this.wiseOldManPeriod = wiseOldManPeriod;
	}

	String label()
	{
		return label;
	}

	int days()
	{
		return days;
	}

	String wiseOldManPeriod()
	{
		return wiseOldManPeriod;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
