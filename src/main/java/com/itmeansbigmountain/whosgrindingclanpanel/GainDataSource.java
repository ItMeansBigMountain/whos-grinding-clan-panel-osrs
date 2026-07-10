package com.itmeansbigmountain.whosgrindingclanpanel;

enum GainDataSource
{
	TRACKER_APIS("Tracker APIs (WOM)"),
	OFFICIAL_HISCORES("Official Hiscores delta"),
	BOTH_FOR_DEVELOPMENT("Both (development)");

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
