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
		"Farming", "Runecrafting", "Hunter", "Construction"
	};
	private static final int MAX_SKILL_LINES = 4;

	String fetchGrindingSummary(String playerName, GainsPeriod period) throws IOException
	{
		String normalizedName = WhosGrindingClanPanelPlugin.normalizePlayerName(playerName);
		GainsPeriod safePeriod = period == null ? GainsPeriod.SEVEN_DAYS : period;
		Map<String, Long> currentXp = fetchCurrentXp(normalizedName);
		HiscoreSnapshot current = new HiscoreSnapshot(Instant.now().getEpochSecond(), currentXp);
		Path snapshotFile = snapshotFile(normalizedName);
		List<HiscoreSnapshot> snapshots = readSnapshots(snapshotFile);
		HiscoreSnapshot baseline = findBaseline(snapshots, current.timestamp - safePeriod.days() * 86400L);
		writeSnapshots(snapshotFile, snapshots, current);
		if (baseline == null)
		{
			return "Official hiscores<br>baseline saved.<br>XP gains will<br>show after the<br>next scan for<br>this period.";
		}
		List<GainedSkill> gainedSkills = new ArrayList<>();
		for (Map.Entry<String, Long> entry : current.xp.entrySet())
		{
			long previous = baseline.xp.getOrDefault(entry.getKey(), entry.getValue());
			long gained = Math.max(0, entry.getValue() - previous);
			if (gained > 0 && !"Overall".equals(entry.getKey()))
			{
				gainedSkills.add(new GainedSkill(entry.getKey(), gained));
			}
		}
		List<GainedSkill> topSkills = gainedSkills.stream()
			.sorted(Comparator.comparingLong((GainedSkill skill) -> skill.gained).reversed())
			.limit(MAX_SKILL_LINES)
			.collect(Collectors.toList());
		if (topSkills.isEmpty())
		{
			return "No hiscores XP<br>gains found for<br>this saved period.<br>Try again after<br>more XP changes.";
		}
		List<String> lines = new ArrayList<>();
		lines.add("<b>Skills</b>:");
		for (GainedSkill skill : topSkills)
		{
			lines.add("▴ " + skill.name + ": <b>+" + formatNumber(skill.gained) + " xp</b> (XP)");
		}
		return String.join("<br>", lines);
	}

	private static Map<String, Long> fetchCurrentXp(String playerName) throws IOException
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
		Map<String, Long> xp = new HashMap<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)))
		{
			for (int index = 0; index < SKILLS.length; index++)
			{
				String line = reader.readLine();
				if (line == null)
				{
					break;
				}
				String[] parts = line.split(",");
				if (parts.length >= 3)
				{
					xp.put(SKILLS[index], Long.parseLong(parts[2]));
				}
			}
		}
		return xp;
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
			Map<String, Long> xp = new HashMap<>();
			for (String skillPart : parts[1].split(";"))
			{
				String[] skill = skillPart.split("=", 2);
				if (skill.length == 2)
				{
					xp.put(skill[0], Long.parseLong(skill[1]));
				}
			}
			snapshots.add(new HiscoreSnapshot(Long.parseLong(parts[0]), xp));
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

	private static final class HiscoreSnapshot
	{
		private final long timestamp;
		private final Map<String, Long> xp;

		private HiscoreSnapshot(long timestamp, Map<String, Long> xp)
		{
			this.timestamp = timestamp;
			this.xp = xp;
		}

		private String serialize()
		{
			return timestamp + "," + xp.entrySet().stream()
				.sorted(Map.Entry.comparingByKey())
				.map(entry -> entry.getKey() + "=" + entry.getValue())
				.collect(Collectors.joining(";"));
		}
	}

	private static final class GainedSkill
	{
		private final String name;
		private final long gained;

		private GainedSkill(String name, long gained)
		{
			this.name = name;
			this.gained = gained;
		}
	}
}
