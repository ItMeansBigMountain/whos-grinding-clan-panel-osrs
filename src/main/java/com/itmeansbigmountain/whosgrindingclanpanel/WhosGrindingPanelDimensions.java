package com.itmeansbigmountain.whosgrindingclanpanel;

import net.runelite.client.ui.PluginPanel;

final class WhosGrindingPanelDimensions
{
	static final int CONTENT_PADDING = 0;
	static final int EXTRA_SAFETY_WIDTH = 18;
	static final int CONTENT_WIDTH = PluginPanel.PANEL_WIDTH
		- PluginPanel.SCROLLBAR_WIDTH
		- (PluginPanel.BORDER_OFFSET * 2)
		- (CONTENT_PADDING * 2)
		- EXTRA_SAFETY_WIDTH;
	static final int CONTROL_HEIGHT = 22;
	static final int ROW_ACTION_WIDTH = 0;
	static final int ROW_GAP = 0;
	static final int ROW_HORIZONTAL_PADDING = 4;
	static final int MEMBER_TEXT_WIDTH = CONTENT_WIDTH - ROW_HORIZONTAL_PADDING;

	private WhosGrindingPanelDimensions()
	{
		// Constants only.
	}
}
