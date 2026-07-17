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

	String fetchGrindingSummary(String playerName, GainsPeriod period) throws IOException
	{
		String normalizedName = WhosGrindingClanPanelPlugin.normalizePlayerName(playerName);
		GainsPeriod safePeriod = period == null ? GainsPeriod.SEVEN_DAYS : period;
		HttpResult gainedResult = request("GET", gainedUrl(normalizedName, safePeriod));
		if (gainedResult.responseCode != 200 || (gainedResult.responseCode == 200 && summarizeGains(gainedResult.body).startsWith("No recent gains")))
		{
			// If the player is not tracked yet, or has stale/no gains, ask WOM to create/update then retry gained.
			HttpResult updateResult = request("POST", API_BASE_URL + PlayerTrackingLinks.urlEncode(normalizedName));
			if (updateResult.responseCode != 200 && updateResult.responseCode != 201)
			{
				return "Player not on<br>WOM yet. Saved<br>official baseline<br>and will keep<br>checking trackers.";
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
		JsonObject root = new JsonParser().parse(json).getAsJsonObject();
		JsonObject data = root.getAsJsonObject("data");
		if (data == null)
		{
			return "No recent gains<br>found. WOM tracking<br>was started/updated if<br>needed. Try 30/365<br>days or check<br>again after XP/KC<br>changes.";
		}

		List<GainedLine> skills = new ArrayList<>();
		List<GainedLine> bosses = new ArrayList<>();
		List<GainedLine> activities = new ArrayList<>();
		collectSection(skills, data.getAsJsonObject("skills"), "XP", "experience", "xp", true);
		collectSection(bosses, data.getAsJsonObject("bosses"), "KC", "kills", "kc", false);
		collectSection(activities, data.getAsJsonObject("activities"), "Score", "score", "score", false);

		List<String> sections = new ArrayList<>();
		addSection(sections, "Skills", skills);
		addSection(sections, "Bosses", bosses);
		addSection(sections, "Activities", activities);

		if (sections.isEmpty())
		{
			return "No recent gains<br>found. WOM tracking<br>was started/updated if<br>needed. Try 30/365<br>days or check<br>again after XP/KC<br>changes.";
		}
		return String.join("<br>", sections);
	}

	private static void addSection(List<String> sections, String title, List<GainedLine> lines)
	{
		List<GainedLine> positiveLines = lines.stream()
			.filter(line -> line.gained > 0)
			.sorted(Comparator.comparingLong((GainedLine line) -> line.gained).reversed())
			.collect(Collectors.toList());
		if (!positiveLines.isEmpty())
		{
			boolean onePerLine = true;
			String separator = onePerLine ? "<br>" : "; ";
			String headingSeparator = onePerLine ? ":<br>" : ": ";
			sections.add("<b>" + title + "</b>" + headingSeparator
				+ positiveLines.stream().map(GainedLine::format).collect(Collectors.joining(separator)));
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
		switch (metric)
		{
			case "chambers_of_xeric":
				return "CoX";
			case "chambers_of_xeric_challenge_mode":
				return "CM CoX";
			case "theatre_of_blood":
				return "ToB";
			case "theatre_of_blood_hard_mode":
				return "HMT";
			case "tombs_of_amascut":
				return "ToA";
			case "tombs_of_amascut_expert":
				return "ToA Expert";
			case "corrupted_gauntlet":
				return "CG";
			case "general_graardor":
				return "Bandos";
			case "commander_zilyana":
				return "Zily";
			case "kril_tsutsaroth":
				return "Zammy";
			case "kreearra":
				return "Arma";
			case "dagannoth_rex":
				return "Rex";
			case "dagannoth_prime":
				return "Prime";
			case "dagannoth_supreme":
				return "Supreme";
			case "kalphite_queen":
				return "KQ";
			case "king_black_dragon":
				return "KBD";
			case "corporeal_beast":
				return "Corp";
			case "cerberus":
				return "Cerb";
			case "grotesque_guardians":
				return "GG";
			case "phosanis_nightmare":
				return "PNM";
			case "the_nightmare":
				return "NM";
			case "phantom_muspah":
				return "Muspah";
			case "thermonuclear_smoke_devil":
				return "Thermy";
			case "wintertodt":
				return "WT";
			case "tzkal_zuk":
				return "Zuk";
			case "tztok_jad":
				return "Jad";
			case "vardorvis":
				return "Vard";
			case "the_leviathan":
				return "Levi";
			case "the_whisperer":
				return "Whisp";
			case "venenatis":
				return "Vene";
			case "sarachnis":
				return "Sarach";
			case "zalcano":
				return "Zalc";
			case "last_man_standing":
				return "LMS";
			case "bounty_hunter_hunter":
				return "Bounty Hunter";
			case "bounty_hunter_rogue":
				return "Bounty Hunter Rogue";
			case "soul_wars":
				return "SW";
			case "pest_control":
				return "PC";
			case "barbarian_assault":
				return "BA";
			case "castle_wars":
				return "CW";
			case "guardians_of_the_rift":
				return "GotR";
			case "volcanic_mine":
				return "VM";
			case "collection_log":
				return "Col Log";
			default:
				break;
		}
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
			return icon() + " " + metric + ": <b>+" + formatNumber(gained) + " " + suffix + "</b>";
		}

		private String icon()
		{
			if ("XP".equals(label))
			{
				return "▴";
			}
			if ("KC".equals(label))
			{
				return "⚔";
			}
			return "★";
		}
	}
}
