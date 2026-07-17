package com.itmeansbigmountain.whosgrindingclanpanel;

public enum GainDataSource
{
	TRACKER_APIS("Tracker APIs (WOM)"),
	OFFICIAL_HISCORES("Official Hiscores delta");

	private final String label;

	GainDataSource(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
