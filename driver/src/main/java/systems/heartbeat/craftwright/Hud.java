package systems.heartbeat.craftwright;

import java.time.Duration;
import java.util.List;
import systems.heartbeat.craftwright.internal.Json;

/** What sits on top of the game view: titles, the action bar, boss bars and the player list. */
public final class Hud {
	private final Remote remote;
	private final Received<Title> titles;
	private final Received<Text> subtitles;
	private final Received<Text> actionBar;

	Hud(Remote remote) {
		this.remote = remote;
		this.titles = new Received<>("title", remote, title -> title.text().plain());
		this.subtitles = new Received<>("subtitle", remote, Text::plain);
		this.actionBar = new Received<>("action bar text", remote, Text::plain);
	}

	/** Every title the server sent, e.g. {@code hud().titles().await("Welcome")}. */
	public Received<Title> titles() {
		return titles;
	}

	public Received<Text> subtitles() {
		return subtitles;
	}

	/** Texts shown above the hotbar. */
	public Received<Text> actionBar() {
		return actionBar;
	}

	/** The boss bars shown at the top of the screen right now. */
	public List<BossBar> bossBars() {
		return Json.list(remote.call("hud.bossBars"), "bossBars", BossBar::of);
	}

	/** Waits for a boss bar whose text contains the given words. */
	public BossBar awaitBossBar(String words) {
		return awaitBossBar(words, remote.timeout());
	}

	public BossBar awaitBossBar(String words, Duration timeout) {
		return remote.await("a boss bar containing \"" + words + "\"", timeout,
			() -> bossBars().stream().filter(bar -> bar.name().contains(words)).findFirst(),
			() -> "Boss bars shown: " + bossBars().stream().map(bar -> bar.name().plain()).toList());
	}

	/** The player list shown while holding tab. */
	public TabList tabList() {
		return TabList.of(remote.call("hud.tabList"));
	}
}
