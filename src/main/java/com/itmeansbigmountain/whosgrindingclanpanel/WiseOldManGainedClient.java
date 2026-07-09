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
	private static final int MAX_SKILL_LINES = 4;
	private static final int MAX_BOSS_LINES = 3;
	private static final int MAX_ACTIVITY_LINES = 3;

	String fetchGrindingSummary(String playerName, GainsPeriod period) throws IOException
	{
		String normalizedName = WhosGrindingClanPanelPlugin.normalizePlayerName(playerName);
		GainsPeriod safePeriod = period == null ? GainsPeriod.SEVEN_DAYS : period;
		HttpResult gainedResult = request("GET", gainedUrl(normalizedName, safePeriod));
		if (gainedResult.responseCode != 200)
		{
			// If the player is not tracked yet, this starts/updates tracking on Wise Old Man, then retries gained.
			HttpResult updateResult = request("POST", API_BASE_URL + PlayerTrackingLinks.urlEncode(normalizedName));
			if (updateResult.responseCode != 200 && updateResult.responseCode != 201)
			{
				throw new IOException("Wise Old Man update returned HTTP " + updateResult.responseCode);
			}
			gainedResult = request("GET", gainedUrl(normalizedName, safePeriod));
		}
		if (gainedResult.responseCode != 200)
		{
			throw new IOException("Wise Old Man gained returned HTTP " + gainedResult.responseCode);
		}
		return summarizeGains(gainedResult.body);
	}

	private static String gainedUrl(String normalizedName, GainsPeriod period)
	{
		return API_BASE_URL + PlayerTrackingLinks.urlEncode(normalizedName) + "/gained?period=" + period.wiseOldManPeriod();
	}

	private static HttpResult request(String method, String url) throws IOException
	{
		HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
		connection.setRequestMethod(method);
		connection.setConnectTimeout(3500);
		connection.setReadTimeout(5000);
		connection.setRequestProperty("User-Agent", "WhosGrindingPanel RuneLite plugin");
		connection.setRequestProperty("Accept", "application/json");
		int responseCode = connection.getResponseCode();
		BufferedReader reader = new BufferedReader(new InputStreamReader(
			responseCode >= 200 && responseCode < 400 ? connection.getInputStream() : connection.getErrorStream(),
			StandardCharsets.UTF_8));
		try (BufferedReader closeableReader = reader)
		{
			StringBuilder body = new StringBuilder();
			String line;
			while ((line = closeableReader.readLine()) != null)
			{
				body.append(line);
			}
			return new HttpResult(responseCode, body.toString());
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

		List<GainedLine> skills = new ArrayList<>();
		List<GainedLine> bosses = new ArrayList<>();
		List<GainedLine> activities = new ArrayList<>();
		collectSection(skills, data.getAsJsonObject("skills"), "XP", "experience", "xp", true);
		collectSection(bosses, data.getAsJsonObject("bosses"), "KC", "kills", "kc", false);
		collectSection(activities, data.getAsJsonObject("activities"), "Score", "score", "score", false);

		List<String> sections = new ArrayList<>();
		addSection(sections, "Skills", skills, MAX_SKILL_LINES);
		addSection(sections, "Bosses", bosses, MAX_BOSS_LINES);
		addSection(sections, "Activities", activities, MAX_ACTIVITY_LINES);

		if (sections.isEmpty())
		{
			return "No tracked XP/KC/score gains found for this period.";
		}
		return String.join("<br>", sections);
	}

	private static void addSection(List<String> sections, String title, List<GainedLine> lines, int maxLines)
	{
		List<GainedLine> positiveLines = lines.stream()
			.filter(line -> line.gained > 0)
			.sorted(Comparator.comparingLong((GainedLine line) -> line.gained).reversed())
			.limit(maxLines)
			.collect(Collectors.toList());
		if (!positiveLines.isEmpty())
		{
			sections.add("<b>" + title + "</b>: " + positiveLines.stream().map(GainedLine::format).collect(Collectors.joining("; ")));
		}
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

	private static final class HttpResult
	{
		private final int responseCode;
		private final String body;

		private HttpResult(int responseCode, String body)
		{
			this.responseCode = responseCode;
			this.body = body;
		}
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
