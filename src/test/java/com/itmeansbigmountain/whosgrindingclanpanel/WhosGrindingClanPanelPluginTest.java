package com.itmeansbigmountain.whosgrindingclanpanel;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class WhosGrindingClanPanelPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(WhosGrindingClanPanelPlugin.class);
		RuneLite.main(args);
	}
}