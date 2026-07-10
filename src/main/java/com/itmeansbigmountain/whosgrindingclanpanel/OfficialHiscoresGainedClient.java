package com.itmeansbigmountain.whosgrindingclanpanel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

final class OfficialHiscoresGainedClient
{
	private static final String HISCORES_URL = "https://secure.runescape.com/m=hiscore_oldschool/index_lite.ws?player=";
	private static final String[] SKILLS = {
		"Overall", "Attack", "Defence", "Strength", "Hitpoints", "Ranged", "Prayer", "Magic", "Cooking", "Woodcutting",
		"Fletching", "Fishing", "Firemaking", "Crafting", "Smithing", "Mining", "Herblore", "Agility", "Thieving", "Slayer",
		"Farming", "Runecrafting", "Hunter", "Construction", "Sailing"
	};
	private static final String[] ACTIVITIES = {
		"League Points", "Bounty Hunter", "Bounty Hunter Rogue", "Clue Scrolls All", "Clue Scrolls Beginner", "Clue Scrolls Easy",
		"Clue Scrolls Medium", "Clue Scrolls Hard", "Clue Scrolls Elite", "Clue Scrolls Master", "LMS", "PvP Arena",
		"Soul Wars", "Rifts Closed", "Colosseum Glory", "Collections Logged"
	};
	private static final String[] BOSSES = {
		"Abyssal Sire", "Alchemical Hydra", "Amoxliatl", "Araxxor", "Artio", "Barrows Chests", "Brutus", "Bryophyta", "Callisto",
		"Calvar'ion", "Cerb", "CoX", "CM CoX", "Chaos Elemental", "Chaos Fanatic", "Zily", "Corp", "Crazy Archaeologist",
		"Prime", "Rex", "Supreme", "Deranged Archaeologist", "Doom", "Duke", "Bandos", "Mole", "GG", "Hespori", "Kalphite Queen",
		"KBD", "Kraken", "Kree'arra", "K'ril", "Lunar Chests", "Maggot King", "Mimic", "Nex", "NM", "Phosani", "Obor",
		"Phantom Muspah", "Sarach", "Scorpia", "Scurrius", "Shellbane Gryphon", "Skotizo", "Sol Heredit", "Spindel", "Tempoross",
		"Gauntlet", "CG", "Huey", "Levi", "Royal Titans", "Whisperer", "ToB", "HMT", "Thermy", "ToA", "ToA Expert",
		"TzKal-Zuk", "TzTok-Jad", "Vard", "Venenatis", "Vet'ion", "Vorkath", "Wintertodt", "Yama", "Zalc", "Zulrah"
	};
	private static final int MAX_SKILL_LINES = 4;
	private static final int MAX_BOSS_LINES = 3;
	private static final int MAX_ACTIVITY_LINES = 3;

	String fetchGrindingSummary(String playerName, GainsPeriod period) throws IOException
	{
		String normalizedName = WhosGrindingClanPanelPlugin.normalizePlayerName(playerName);
		GainsPeriod safePeriod = period == null ? GainsPeriod.SEVEN_DAYS : period;
		HiscoreValues currentValues = fetchCurrentValues(normalizedName);
		HiscoreSnapshot current = new HiscoreSnapshot(Instant.now().getEpochSecond(), currentValues);
		Path snapshotFile = snapshotFile(normalizedName);
		List<HiscoreSnapshot> snapshots = readSnapshots(snapshotFile);
		long targetTimestamp = current.timestamp - safePeriod.days() * 86400L;
		HiscoreSnapshot baseline = findBaseline(snapshots, targetTimestamp);
		boolean partialPeriod = false;
		if (baseline == null)
		{
			baseline = oldestSnapshot(snapshots);
			partialPeriod = baseline != null;
		}
		writeSnapshots(snapshotFile, snapshots, current);
		if (baseline == null)
		{
			return "Tracking baseline<br>saved. Gains show<br>automatically after<br>this period has<br>elapsed.";
		}
		String summary = summarizeDelta(current.values, baseline.values);
		if (partialPeriod)
		{
			return "Partial since<br>first local snapshot:<br>" + summary;
		}
		return summary;
	}

	static String summarizeDelta(HiscoreValues current, HiscoreValues baseline)
	{
		List<String> sections = new ArrayList<>();
		addSection(sections, "Skills", gainedLines(current.skills, baseline.skills, "XP", "xp", "▴", MAX_SKILL_LINES), MAX_SKILL_LINES);
		addSection(sections, "Bosses", gainedLines(current.bosses, baseline.bosses, "KC", "kc", "⚔", MAX_BOSS_LINES), MAX_BOSS_LINES);
		addSection(sections, "Activities", gainedLines(current.activities, baseline.activities, "Score", "score", "★", MAX_ACTIVITY_LINES), MAX_ACTIVITY_LINES);
		if (sections.isEmpty())
		{
			return "No official<br>hiscores gains<br>found for this<br>saved period.<br>Try again after<br>more changes.";
		}
		return String.join("<br>", sections);
	}

	private static List<GainedLine> gainedLines(Map<String, Long> current, Map<String, Long> baseline, String label, String suffix, String icon, int limit)
	{
		return current.entrySet().stream()
			.filter(entry -> !"Overall".equals(entry.getKey()))
			.map(entry -> new GainedLine(entry.getKey(), Math.max(0, entry.getValue() - baseline.getOrDefault(entry.getKey(), entry.getValue())), label, suffix, icon))
			.filter(line -> line.gained > 0)
			.sorted(Comparator.comparingLong((GainedLine line) -> line.gained).reversed())
			.limit(limit)
			.collect(Collectors.toList());
	}

	private static void addSection(List<String> sections, String title, List<GainedLine> lines, int maxLines)
	{
		if (!lines.isEmpty())
		{
			sections.add("<b>" + title + "</b>:<br>" + lines.stream().limit(maxLines).map(GainedLine::format).collect(Collectors.joining("<br>")));
		}
	}

	private static HiscoreValues fetchCurrentValues(String playerName) throws IOException
	{
		HttpURLConnection connection = (HttpURLConnection) new URL(HISCORES_URL + PlayerTrackingLinks.urlEncode(playerName)).openConnection();
		connection.setRequestMethod("GET");
		connection.setConnectTimeout(3500);
		connection.setReadTimeout(5000);
		connection.setRequestProperty("User-Agent", "WhosGrindingPanel RuneLite plugin");
		int responseCode = connection.getResponseCode();
		if (responseCode != 200)
		{
			throw new IOException("Official hiscores returned HTTP " + responseCode);
		}
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)))
		{
			return parseLiteCsv(reader.lines().collect(Collectors.toList()));
		}
	}

	static HiscoreValues parseLiteCsv(List<String> rows)
	{
		HiscoreValues values = new HiscoreValues();
		int row = 0;
		row = parseSection(rows, row, SKILLS, values.skills, 2);
		row = parseSection(rows, row, ACTIVITIES, values.activities, 1);
		parseSection(rows, row, BOSSES, values.bosses, 1);
		return values;
	}

	private static int parseSection(List<String> rows, int start, String[] names, Map<String, Long> target, int valueIndex)
	{
		int row = start;
		for (String name : names)
		{
			if (row >= rows.size())
			{
				break;
			}
			String[] parts = rows.get(row).split(",");
			if (parts.length > valueIndex)
			{
				try
				{
					long value = Long.parseLong(parts[valueIndex]);
					if (value >= 0)
					{
						target.put(name, value);
					}
				}
				catch (NumberFormatException ignored)
				{
					// Ignore malformed official rows rather than breaking the whole fallback.
				}
			}
			row++;
		}
		return row;
	}

	private static Path snapshotFile(String playerName)
	{
		String safeName = TrackedMember.normalizeKey(playerName).replaceAll("[^a-z0-9_-]", "_");
		return Paths.get(System.getProperty("user.home"), ".runelite", "whos-grinding-hiscores", safeName + ".csv");
	}

	private static List<HiscoreSnapshot> readSnapshots(Path path) throws IOException
	{
		List<HiscoreSnapshot> snapshots = new ArrayList<>();
		if (!Files.exists(path))
		{
			return snapshots;
		}
		for (String line : Files.readAllLines(path, StandardCharsets.UTF_8))
		{
			String[] parts = line.split(",", 2);
			if (parts.length != 2)
			{
				continue;
			}
			HiscoreValues values = HiscoreValues.deserialize(parts[1]);
			if (values != null)
			{
				snapshots.add(new HiscoreSnapshot(Long.parseLong(parts[0]), values));
			}
		}
		return snapshots;
	}

	private static HiscoreSnapshot findBaseline(List<HiscoreSnapshot> snapshots, long targetTimestamp)
	{
		HiscoreSnapshot best = null;
		for (HiscoreSnapshot snapshot : snapshots)
		{
			if (snapshot.timestamp <= targetTimestamp && (best == null || snapshot.timestamp > best.timestamp))
			{
				best = snapshot;
			}
		}
		if (best != null)
		{
			return best;
		}
		return null;
	}

	private static HiscoreSnapshot oldestSnapshot(List<HiscoreSnapshot> snapshots)
	{
		return snapshots.stream().min(Comparator.comparingLong(snapshot -> snapshot.timestamp)).orElse(null);
	}

	private static void writeSnapshots(Path path, List<HiscoreSnapshot> snapshots, HiscoreSnapshot current) throws IOException
	{
		Files.createDirectories(path.getParent());
		long keepAfter = current.timestamp - 370L * 86400L;
		List<String> lines = new ArrayList<>();
		for (HiscoreSnapshot snapshot : snapshots)
		{
			if (snapshot.timestamp >= keepAfter)
			{
				lines.add(snapshot.serialize());
			}
		}
		lines.add(current.serialize());
		Files.write(path, lines, StandardCharsets.UTF_8);
	}

	private static String formatNumber(long value)
	{
		return String.format(Locale.US, "%,d", value);
	}

	static final class HiscoreValues
	{
		private final Map<String, Long> skills = new HashMap<>();
		private final Map<String, Long> activities = new HashMap<>();
		private final Map<String, Long> bosses = new HashMap<>();

		private String serialize()
		{
			return "V2|S{" + serializeMap(skills) + "}|A{" + serializeMap(activities) + "}|B{" + serializeMap(bosses) + "}";
		}

		private static HiscoreValues deserialize(String value)
		{
			if (!value.startsWith("V2|S{"))
			{
				// Older snapshots used outdated hiscore row offsets and can create false boss/activity deltas.
				return null;
			}
			HiscoreValues values = new HiscoreValues();
			deserializeMap(extract(value, "S{"), values.skills);
			deserializeMap(extract(value, "A{"), values.activities);
			deserializeMap(extract(value, "B{"), values.bosses);
			return values;
		}

		private static String extract(String value, String prefix)
		{
			int start = value.indexOf(prefix);
			if (start < 0)
			{
				return "";
			}
			start += prefix.length();
			int end = value.indexOf('}', start);
			return end < 0 ? "" : value.substring(start, end);
		}
	}

	private static String serializeMap(Map<String, Long> values)
	{
		return values.entrySet().stream()
			.sorted(Map.Entry.comparingByKey())
			.map(entry -> entry.getKey() + "=" + entry.getValue())
			.collect(Collectors.joining(";"));
	}

	private static void deserializeMap(String text, Map<String, Long> target)
	{
		for (String part : text.split(";"))
		{
			String[] item = part.split("=", 2);
			if (item.length == 2)
			{
				try
				{
					target.put(item[0], Long.parseLong(item[1]));
				}
				catch (NumberFormatException ignored)
				{
					// Keep loading the rest of the snapshot.
				}
			}
		}
	}

	private static final class HiscoreSnapshot
	{
		private final long timestamp;
		private final HiscoreValues values;

		private HiscoreSnapshot(long timestamp, HiscoreValues values)
		{
			this.timestamp = timestamp;
			this.values = values;
		}

		private String serialize()
		{
			return timestamp + "," + values.serialize();
		}
	}

	private static final class GainedLine
	{
		private final String name;
		private final long gained;
		private final String label;
		private final String suffix;
		private final String icon;

		private GainedLine(String name, long gained, String label, String suffix, String icon)
		{
			this.name = name;
			this.gained = gained;
			this.label = label;
			this.suffix = suffix;
			this.icon = icon;
		}

		private String format()
		{
			return icon + " " + name + ": <b>+" + formatNumber(gained) + " " + suffix + "</b>";
		}
	}
}
