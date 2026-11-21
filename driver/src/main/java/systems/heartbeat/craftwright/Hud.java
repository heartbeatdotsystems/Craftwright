package systems.heartbeat.craftwright;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import systems.heartbeat.craftwright.internal.Await;

/** What sits on top of the game view: titles, action bar, boss bars, tab list and sidebar. */
public final class Hud {
	private final Client client;
	private final Messages titles;
	private final Messages subtitles;
	private final Messages actionBar;

	Hud(Client client) {
		this.client = client;
		this.titles = new Messages("title", client);
		this.subtitles = new Messages("subtitle", client);
		this.actionBar = new Messages("action bar text", client);
	}

	/** Every title the server sent, e.g. {@code hud().titles().await("Welcome")}. */
	public Messages titles() {
		return titles;
	}

	public Messages subtitles() {
		return subtitles;
	}

	/** Texts shown above the hotbar. */
	public Messages actionBar() {
		return actionBar;
	}

	/** The boss bars shown at the top of the screen right now. */
	public List<BossBar> bossBars() {
		List<BossBar> bars = new ArrayList<>();
		for (JsonElement element : client.call("hud.bossBars").getAsJsonArray("bossBars")) {
			JsonObject bar = element.getAsJsonObject();
			bars.add(new BossBar(Text.of(bar.get("name")), bar.get("progress").getAsFloat(),
				bar.get("color").getAsString(), bar.get("overlay").getAsString()));
		}
		return bars;
	}

	/** Waits for a boss bar whose text contains the given words. */
	public BossBar awaitBossBar(String contains) {
		return awaitBossBar(contains, client.timeout());
	}

	public BossBar awaitBossBar(String contains, Duration timeout) {
		return Await.until("a boss bar containing \"" + contains + "\" for " + client.name(), timeout,
			() -> bossBars().stream().filter(bar -> bar.name().contains(contains)).findFirst(),
			() -> "Boss bars shown: " + bossBars().stream().map(bar -> bar.name().plain()).toList());
	}

	/** The player list shown while holding tab. */
	public TabList tabList() {
		JsonObject result = client.call("hud.tabList");
		List<TabList.Entry> entries = new ArrayList<>();
		for (JsonElement element : result.getAsJsonArray("entries")) {
			JsonObject entry = element.getAsJsonObject();
			entries.add(new TabList.Entry(entry.get("name").getAsString(), Text.of(entry.get("displayName")),
				entry.get("gameMode").getAsString(), entry.get("latency").getAsInt()));
		}
		return new TabList(Text.of(result.get("header")), Text.of(result.get("footer")), List.copyOf(entries));
	}

	/** The scoreboard on the right side of the screen, if one is shown. */
	public Optional<Sidebar> sidebar() {
		JsonObject result = client.call("hud.sidebar");
		if (!result.get("shown").getAsBoolean()) {
			return Optional.empty();
		}
		List<Sidebar.Line> lines = new ArrayList<>();
		for (JsonElement element : result.getAsJsonArray("lines")) {
			JsonObject line = element.getAsJsonObject();
			lines.add(new Sidebar.Line(Text.of(line.get("text")), line.get("score").getAsInt()));
		}
		return Optional.of(new Sidebar(Text.of(result.get("title")), List.copyOf(lines)));
	}

	/**
	 * @param progress 0 to 1
	 * @param color    pink, blue, red, green, yellow, purple or white
	 * @param overlay  progress or notched_6, notched_10, notched_12, notched_20
	 */
	public record BossBar(Text name, float progress, String color, String overlay) {
	}

	public record TabList(Text header, Text footer, List<Entry> entries) {

		/** Account names of everyone listed. */
		public List<String> players() {
			return entries.stream().map(Entry::name).toList();
		}

		public Optional<Entry> entry(String playerName) {
			return entries.stream().filter(entry -> entry.name().equals(playerName)).findFirst();
		}

		/**
		 * @param name        account name
		 * @param displayName the name as listed, with prefixes and colours the server set
		 */
		public record Entry(String name, Text displayName, String gameMode, int latency) {
		}
	}

	public record Sidebar(Text title, List<Line> lines) {

		/** The visible lines, top to bottom. */
		public List<String> texts() {
			return lines.stream().map(line -> line.text().plain()).toList();
		}

		public record Line(Text text, int score) {
		}
	}
}
