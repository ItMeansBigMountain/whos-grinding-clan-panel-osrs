package com.itmeansbigmountain.whosgrindingclanpanel;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

final class OfficialHiscoresGainedClient implements GrindingSummaryClient
{
	interface HiscoresFetcher
	{
		HiscoreValues fetch(String playerName) throws IOException;
	}

	interface SnapshotReplacer
	{
		void replace(Path temporary, Path target) throws IOException;
	}

	private static final String HISCORES_URL = "https://secure.runescape.com/m=hiscore_oldschool/index_lite.ws?player=";
	private static final DateTimeFormatter SNAPSHOT_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'").withZone(ZoneOffset.UTC);
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
	private final Path snapshotDirectory;
	private final Clock clock;
	private final HiscoresFetcher fetcher;
	private final SnapshotReplacer snapshotReplacer;


	OfficialHiscoresGainedClient(Path snapshotDirectory, Clock clock, HiscoresFetcher fetcher)
	{
		this(snapshotDirectory, clock, fetcher, OfficialHiscoresGainedClient::replaceAtomically);
	}

	OfficialHiscoresGainedClient(Path snapshotDirectory, Clock clock, HiscoresFetcher fetcher, SnapshotReplacer snapshotReplacer)
	{
		this.snapshotDirectory = snapshotDirectory;
		this.clock = clock;
		this.fetcher = fetcher;
		this.snapshotReplacer = snapshotReplacer;
	}

	@Override
	public synchronized String fetchGrindingSummary(String playerName, GainsPeriod period) throws IOException
	{
		String normalizedName = WhosGrindingClanPanelPlugin.normalizePlayerName(playerName);
		HiscoreValues currentValues = fetcher.fetch(normalizedName);
		HiscoreSnapshot current = new HiscoreSnapshot(clock.instant().getEpochSecond(), currentValues);
		Path snapshotFile = snapshotFileFor(normalizedName);
		HiscoreSnapshot baseline = latestSnapshot(readSnapshots(snapshotFile));
		writeSnapshot(snapshotFile, current);
		if (baseline == null)
		{
			return "Scan saved.<br>Scan again later<br>to see new gains.";
		}
		return "Difference since<br>previous plugin scan:<br>Previous snapshot:<br>"
			+ SNAPSHOT_TIME.format(Instant.ofEpochSecond(baseline.timestamp)) + "<br>"
			+ summarizeDelta(current.values, baseline.values);
	}

	static String summarizeDelta(HiscoreValues current, HiscoreValues baseline)
	{
		List<String> sections = new ArrayList<>();
		addSection(sections, "Skills", gainedLines(current.skills, baseline.skills, "XP", "xp", "▴"));
		addSection(sections, "Bosses", gainedLines(current.bosses, baseline.bosses, "KC", "kc", "⚔"));
		addSection(sections, "Activities", gainedLines(current.activities, baseline.activities, "Score", "score", "★"));
		if (sections.isEmpty())
		{
			return "No new gains<br>since the last scan.<br>Scan again after<br>XP, KC, or score changes.";
		}
		return String.join("<br>", sections);
	}

	private static List<GainedLine> gainedLines(Map<String, Long> current, Map<String, Long> baseline, String label, String suffix, String icon)
	{
		return current.entrySet().stream()
			.filter(entry -> !"Overall".equals(entry.getKey()))
			.map(entry -> new GainedLine(entry.getKey(), Math.max(0, entry.getValue() - baseline.getOrDefault(entry.getKey(), entry.getValue())), label, suffix, icon))
			.filter(line -> line.gained > 0)
			.sorted(Comparator.comparingLong((GainedLine line) -> line.gained).reversed())
			.collect(Collectors.toList());
	}

	private static void addSection(List<String> sections, String title, List<GainedLine> lines)
	{
		if (!lines.isEmpty())
		{
			sections.add("<b>" + title + "</b>:<br>" + lines.stream().map(GainedLine::format).collect(Collectors.joining("<br>")));
		}
	}

	static HiscoreValues fetchCurrentValues(OkHttpClient httpClient, String playerName) throws IOException
	{
		Request request = new Request.Builder()
			.url(HISCORES_URL + PlayerTrackingLinks.urlEncode(playerName))
			.header("User-Agent", "WhosGrindingPanel RuneLite plugin")
			.build();
		try (Response response = httpClient.newCall(request).execute())
		{
			if (!response.isSuccessful())
			{
				throw new IOException("Official hiscores returned HTTP " + response.code());
			}
			if (response.body() == null)
			{
				throw new IOException("Official hiscores returned an empty response");
			}
			return parseLiteCsvChecked(java.util.Arrays.asList(response.body().string().split("\\R")));
		}
	}

	static HiscoreValues parseLiteCsvChecked(List<String> rows) throws IOException
	{
		int expectedRows = SKILLS.length + ACTIVITIES.length + BOSSES.length;
		if (rows.size() < expectedRows)
		{
			throw new IOException("Official hiscores schema incomplete: expected at least " + expectedRows + " rows, got " + rows.size());
		}
		int row = 0;
		for (int i = 0; i < SKILLS.length; i++, row++)
		{
			validateLiveRow(rows.get(row), 2, row);
		}
		for (int i = 0; i < ACTIVITIES.length + BOSSES.length; i++, row++)
		{
			validateLiveRow(rows.get(row), 1, row);
		}
		return parseLiteCsv(rows);
	}

	private static void validateLiveRow(String row, int valueIndex, int rowNumber) throws IOException
	{
		String[] parts = row.split(",");
		if (parts.length <= valueIndex)
		{
			throw new IOException("Official hiscores schema malformed at row " + rowNumber);
		}
		try
		{
			Long.parseLong(parts[valueIndex]);
		}
		catch (NumberFormatException ex)
		{
			throw new IOException("Official hiscores schema malformed at row " + rowNumber, ex);
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

	Path snapshotFileFor(String playerName)
	{
		String safeName = TrackedMember.normalizeKey(playerName).replaceAll("[^a-z0-9_-]", "_");
		return snapshotDirectory.resolve(safeName + ".csv");
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
			try
			{
				HiscoreValues values = HiscoreValues.deserialize(parts[1]);
				if (values != null)
				{
					snapshots.add(new HiscoreSnapshot(Long.parseLong(parts[0]), values));
				}
			}
			catch (NumberFormatException ignored)
			{
				// A partial/corrupt line must not hide otherwise valid history.
			}
		}
		return snapshots;
	}

	private static HiscoreSnapshot latestSnapshot(List<HiscoreSnapshot> snapshots)
	{
		return snapshots.stream().max(Comparator.comparingLong(snapshot -> snapshot.timestamp)).orElse(null);
	}

	static HiscoreSnapshot testBaselineForPeriod(List<HiscoreSnapshot> snapshots, GainsPeriod ignored)
	{
		return latestSnapshot(snapshots);
	}

	static HiscoreSnapshot testBaselineForPeriod(List<HiscoreSnapshot> snapshots, GainsPeriod ignored, Clock ignoredClock)
	{
		return latestSnapshot(snapshots);
	}

	private void writeSnapshot(Path path, HiscoreSnapshot current) throws IOException
	{
		Files.createDirectories(path.getParent());
		Path temporary = Files.createTempFile(path.getParent(), path.getFileName().toString(), ".tmp");
		try
		{
			Files.write(temporary, java.util.Collections.singletonList(current.serialize()), StandardCharsets.UTF_8);
			snapshotReplacer.replace(temporary, path);
		}
		finally
		{
			Files.deleteIfExists(temporary);
		}
	}

	private static void replaceAtomically(Path temporary, Path target) throws IOException
	{
		try
		{
			Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
		}
		catch (java.nio.file.AtomicMoveNotSupportedException ignored)
		{
			Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
		}
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
			if (!value.startsWith("V2|S{") || !value.contains("}|A{") || !value.contains("}|B{") || !value.endsWith("}"))
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

	static final class HiscoreSnapshot
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

		// Package-private for testing
		long testTimestamp()
		{
			return timestamp;
		}
	}

	// Package-private for testing
	static List<HiscoreSnapshot> testReadSnapshots(Path path) throws IOException
	{
		return readSnapshots(path);
	}

	static HiscoreSnapshot testSnapshot(long timestamp)
	{
		return new HiscoreSnapshot(timestamp, new HiscoreValues());
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
