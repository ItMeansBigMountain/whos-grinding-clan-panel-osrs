package com.itmeansbigmountain.whosgrindingclanpanel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import org.junit.Test;

public class SocialSourceFilterTest
{
	@Test
	public void exposesSeparateFiltersForFriendsChatClanChatAndFriendsList()
	{
		assertEquals("Friends Chat", SocialSourceFilter.FRIENDS_CHAT.label());
		assertEquals("Clan Chat", SocialSourceFilter.CLAN_CHAT.label());
		assertEquals("Friends List", SocialSourceFilter.FRIENDS.label());
	}

	@Test
	public void clanChatFilterAcceptsClanMembersOnly()
	{
		TrackedMember clanMember = new TrackedMember("Oyama", TrackedMemberSource.CLAN, Instant.parse("2026-07-08T00:00:00Z"));

		assertTrue(SocialSourceFilter.CLAN_CHAT.accepts(clanMember));
	}
}
