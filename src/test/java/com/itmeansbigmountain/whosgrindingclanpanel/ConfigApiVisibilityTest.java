package com.itmeansbigmountain.whosgrindingclanpanel;

import static org.junit.Assert.assertTrue;

import java.lang.reflect.Modifier;
import org.junit.Test;

public class ConfigApiVisibilityTest
{
	@Test
	public void configEnumTypesArePublicForRuneliteProxyAccess()
	{
		assertTrue("RuneLite config proxy must access enum return type", Modifier.isPublic(GainsPeriod.class.getModifiers()));
	}
}
