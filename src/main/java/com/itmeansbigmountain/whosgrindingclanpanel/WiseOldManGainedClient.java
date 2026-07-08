package com.itmeansbigmountain.whosgrindingclanpanel;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

final class WiseOldManGainedClient
{
	private static final String API_BASE_URL = "https://api.wiseoldman.net/v2/players/";
	private static final int MAX_LINES = 5;

	String fetchGrindingSummary(String playerName, GainsPeriod period) throws IOException
	{
		String url = API_BASE_URL
			+ PlayerTrackingLinks.urlEncode(WhosGrindingClanPanelPlugin.normalizePlayerName(playerName))
			+ "/gained?period=" + (period == null ? GainsPeriod.SEVEN_DAYS : period).wiseOldManPeriod();
		HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
		connection.setConnectTimeout(3500);
		connection.setReadTimeout(5000);
		connection.setRequestProperty("User-Agent", "WhosGrindingPanel RuneLite plugin");
		connection.setRequestProperty("Accept", "application/json");

		int responseCode = connection.getResponseCode();
		if (responseCode != 200)
		{
			throw new IOException("Wise Old Man returned HTTP " + responseCode);
		}
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)))
		{
			StringBuilder body = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null)
			{
				body.append(line);
			}
			return summarizeGains(body.toString());
		}
	}

	static String summarizeGains(String json)
	{
		JsonObject root = JsonParser.parseString(json).getAsJsonObject();
		JsonObject data = root.getAsJsonObject("data");
		if (data == null)
		{
			return "No Wise Old Man gained data for this period.";
		}

		List<GainedLine> lines = new ArrayList<>();
		collectSection(lines, data.getAsJsonObject("skills"), "XP", "experience", "xp", true);
		collectSection(lines, data.getAsJsonObject("bosses"), "KC", "kills", "kc", false);
		collectSection(lines, data.getAsJsonObject("activities"), "Score", "score", "score", false);

		List<GainedLine> positiveLines = lines.stream()
			.filter(line -> line.gained > 0)
			.sorted(Comparator.comparingLong((GainedLine line) -> line.gained).reversed())
			.limit(MAX_LINES)
			.collect(Collectors.toList());

		if (positiveLines.isEmpty())
		{
			return "No tracked XP/KC/score gains found for this period.";
		}
		return positiveLines.stream().map(GainedLine::format).collect(Collectors.joining("<br>"));
	}

	private static void collectSection(List<GainedLine> lines, JsonObject section, String label, String valueObject, String suffix, boolean skipOverall)
	{
		if (section == null)
		{
			return;
		}
		for (String metric : section.keySet())
		{
			if (skipOverall && "overall".equals(metric))
			{
				continue;
			}
			JsonObject metricObject = section.getAsJsonObject(metric);
			JsonObject gainedObject = metricObject == null ? null : metricObject.getAsJsonObject(valueObject);
			long gained = gainedLong(gainedObject);
			if (gained > 0)
			{
				lines.add(new GainedLine(prettyMetric(metric), label, gained, suffix));
			}
		}
	}

	private static long gainedLong(JsonObject object)
	{
		if (object == null)
		{
			return 0;
		}
		JsonElement gained = object.get("gained");
		return gained == null || gained.isJsonNull() ? 0 : Math.max(0, gained.getAsLong());
	}

	private static String prettyMetric(String metric)
	{
		String[] words = metric.replace('_', ' ').split(" ");
		StringBuilder label = new StringBuilder();
		for (String word : words)
		{
			if (word.isEmpty())
			{
				continue;
			}
			if (label.length() > 0)
			{
				label.append(' ');
			}
			label.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
		}
		return label.toString();
	}

	private static String formatNumber(long value)
	{
		return String.format(Locale.US, "%,d", value);
	}

	private static final class GainedLine
	{
		private final String metric;
		private final String label;
		private final long gained;
		private final String suffix;

		private GainedLine(String metric, String label, long gained, String suffix)
		{
			this.metric = metric;
			this.label = label;
			this.gained = gained;
			this.suffix = suffix;
		}

		private String format()
		{
			return metric + ": +" + formatNumber(gained) + " " + suffix + " (" + label + ")";
		}
	}
}
