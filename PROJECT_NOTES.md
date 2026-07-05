# Who's Grinding Panel — Working Notes

## Product direction

- This plugin is **Who’s Grinding Panel**, not clan-only.
- It must function as a standalone RuneLite plugin; do not require users to install SmartHiscoreLookup or any other plugin dependency.
- Code patterns may mirror SmartHiscoreLookup / AccountLegacyCard, but features needed for this plugin should live in this repo.
- Panel should fit default RuneLite sidebar width. Avoid wide tabs/buttons; explicitly constrain Swing controls under `BoxLayout`.

## Working RuneLite social API findings

Verified from the local `runelite-api-1.12.31.1.jar` with `javap`:

```java
Client#getFriendContainer()
FriendContainer extends NameableContainer<Friend>
NameableContainer#getMembers()
Friend extends ChatPlayer
ChatPlayer#getWorld()
Nameable#getName()
Nameable#getPrevName()
```

Working friends-list scan pattern:

```java
FriendContainer friendContainer = client.getFriendContainer();
if (friendContainer != null && friendContainer.getMembers() != null)
{
    for (Friend friend : friendContainer.getMembers())
    {
        if (friend != null && (config.showOfflineFriends() || friend.getWorld() > 0))
        {
            String name = friend.getName();
            int world = friend.getWorld();
        }
    }
}
```

Verified friends-chat API:

```java
Client#getFriendsChatManager()
FriendsChatManager extends NameableContainer<FriendsChatMember>
FriendsChatMember#getWorld()
FriendsChatMember#getRank()
```

Working friends-chat scan pattern:

```java
FriendsChatManager friendsChatManager = client.getFriendsChatManager();
if (friendsChatManager != null && friendsChatManager.getMembers() != null)
{
    for (FriendsChatMember member : friendsChatManager.getMembers())
    {
        if (member != null)
        {
            String name = member.getName();
            int world = member.getWorld();
        }
    }
}
```

Verified clan-channel API:

```java
Client#getClanChannel()
Client#getGuestClanChannel()
Client#getClanChannel(int)
Client#getClanSettings()
Client#getGuestClanSettings()
Client#getClanSettings(int)
ClanChannel#getMembers()
ClanChannelMember#getName()
ClanChannelMember#getWorld()
ClanSettings#getMembers()
ClanMember#getName()
```

Current code uses `client.getClanChannel()`. If live clan scan returns empty, next working candidates to try are:

```java
ClanChannel clanChannel = client.getClanChannel();
ClanChannel guestClanChannel = client.getGuestClanChannel();
ClanSettings clanSettings = client.getClanSettings();
ClanSettings guestClanSettings = client.getGuestClanSettings();
```

Relevant events available in this RuneLite API jar:

```java
ClanChannelChanged#getClanChannel()
ClanChannelChanged#getClanId()
ClanChannelChanged#isGuest()
ClanMemberJoined#getClanChannel()
ClanMemberJoined#getClanMember()
ClanMemberLeft#getClanChannel()
ClanMemberLeft#getClanMember()
FriendsChatChanged#isJoined()
FriendsChatMemberJoined#getMember()
FriendsChatMemberLeft#getMember()
RemovedFriend#getNameable()
```

## Current implemented behavior

- Manual rescan button: `↻`.
- Automatic scan on login and interval.
- Configurable refresh interval, default 60 minutes.
- Configurable `Show offline friends`, default false.
- Friends list hides offline friends unless `Show offline friends` is checked.
- Each row shows a quick icon:
  - `●` online
  - `◇` social/channel source
  - `○` offline/unknown
- Clicking a row opens local tracker details.

## Detail-view direction

The detail card should eventually include functionality similar to AccountLegacyCard / SmartHiscoreLookup, but implemented locally in this repo so the plugin remains standalone. Keep code style and data shapes similar across repos, but do not add plugin-to-plugin dependencies yet.

Potential detail fields:

- display name
- previous name if RuneLite exposes `Nameable#getPrevName()`
- current status/world/source
- first/last seen
- last status change
- hiscore lookup URL
- latest available hiscore/Wise Old Man/TempleOSRS summary once local clients are added
- recent activity classification: skilling, bossing, PvP, idle/unknown

## Build verification

Use Java 11:

```bash
export JAVA_HOME=/opt/data/jdks/current-java11
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew clean test assemble --no-daemon --console=plain
```
