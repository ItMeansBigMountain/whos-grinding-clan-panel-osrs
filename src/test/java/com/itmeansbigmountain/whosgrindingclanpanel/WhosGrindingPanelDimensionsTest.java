package com.itmeansbigmountain.whosgrindingclanpanel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import net.runelite.client.ui.PluginPanel;
import org.junit.Test;

public class WhosGrindingPanelDimensionsTest
{
	@Test
	public void derivesSafeContentWidthFromRuneLitePluginPanelConstants()
	{
		assertEquals(225, PluginPanel.PANEL_WIDTH);
		assertEquals(17, PluginPanel.SCROLLBAR_WIDTH);
		assertEquals(6, PluginPanel.BORDER_OFFSET);
		assertEquals(178, WhosGrindingPanelDimensions.CONTENT_WIDTH);
	}

	@Test
	public void reservesSpaceForCompactRowActionsWithoutHorizontalScrolling()
	{
		assertTrue(WhosGrindingPanelDimensions.MEMBER_TEXT_WIDTH > 120);
		assertTrue(WhosGrindingPanelDimensions.MEMBER_TEXT_WIDTH + WhosGrindingPanelDimensions.ROW_ACTION_WIDTH
			+ WhosGrindingPanelDimensions.ROW_GAP + WhosGrindingPanelDimensions.ROW_HORIZONTAL_PADDING
			<= WhosGrindingPanelDimensions.CONTENT_WIDTH);
	}
}
