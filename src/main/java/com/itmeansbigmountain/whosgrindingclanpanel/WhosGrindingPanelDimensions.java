package com.itmeansbigmountain.whosgrindingclanpanel;

import net.runelite.client.ui.PluginPanel;

final class WhosGrindingPanelDimensions
{
	static final int CONTENT_PADDING = 4;
	static final int CONTENT_WIDTH = PluginPanel.PANEL_WIDTH
		- PluginPanel.SCROLLBAR_WIDTH
		- (PluginPanel.BORDER_OFFSET * 2)
		- (CONTENT_PADDING * 2);
	static final int CONTROL_HEIGHT = 24;
	static final int ROW_ACTION_WIDTH = 26;
	static final int ROW_GAP = 3;
	static final int ROW_HORIZONTAL_PADDING = 8;
	static final int MEMBER_TEXT_WIDTH = CONTENT_WIDTH - ROW_ACTION_WIDTH - ROW_GAP - ROW_HORIZONTAL_PADDING;

	private WhosGrindingPanelDimensions()
	{
		// Constants only.
	}
}
