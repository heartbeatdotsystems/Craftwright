package systems.heartbeat.craftwright;

import com.google.gson.JsonObject;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;
import systems.heartbeat.craftwright.internal.Json;

/**
 * One real Minecraft client, running without a window.
 *
 * <pre>{@code
 * Client alex = craftwright.client("Alex").start();
 * alex.join(server);
 * alex.chat().command("spawn");
 * alex.chat().await("Welcome back");
 * }</pre>
 */
public final class Client implements AutoCloseable {
	/** Joining loads the world on top of the usual waiting time. */
	private static final Duration JOIN_ALLOWANCE = Duration.ofSeconds(30);

	private final Remote remote;
	private final ClientProcess process;
	private final Chat chat;
	private final Hud hud;
	private final Scoreboard scoreboard;
	private final Menus menus;
	private final Player player;
	private final World world;
	private final Received<Sound> sounds;
	private final Received<Particles> particles;
	private volatile Text lastDisconnectReason;

	Client(Remote remote, ClientProcess process) {
		this.remote = remote;
		this.process = process;
		this.chat = new Chat(remote);
		this.hud = new Hud(remote);
		this.scoreboard = new Scoreboard(remote);
		this.menus = new Menus(remote);
		this.player = new Player(remote);
		this.world = new World(remote);
		this.sounds = new Received<>("sound", remote, Sound::id);
		this.particles = new Received<>("particle", remote, Particles::id);
		remote.connection().onEvent(this::onEvent);
	}

	private void onEvent(JsonObject message) {
		JsonObject data = message.has("data") ? message.getAsJsonObject("data") : new JsonObject();
		switch (message.get("event").getAsString()) {
			case "chat" -> chat.received().add(Text.of(data.get("text")));
			case "title" -> hud.titles().add(Title.of(data));
			case "subtitle" -> hud.subtitles().add(Text.of(data.get("text")));
			case "actionBar" -> hud.actionBar().add(Text.of(data.get("text")));
			case "sound" -> sounds.add(Sound.of(data));
			case "particle" -> particles.add(Particles.of(data));
			case "disconnect" -> lastDisconnectReason = Text.of(data.get("reason"));
			default -> {
				// events this driver version does not know are ignored
			}
		}
	}

	/** The account name of this client. */
	public String name() {
		return remote.name();
	}

	/** Game directory of this client, with {@code logs/latest.log} and options. */
	public Path directory() {
		return process.gameDirectory();
	}

	/** How long {@code await…} calls wait unless told otherwise. */
	public Duration timeout() {
		return remote.timeout();
	}

	public Client timeout(Duration timeout) {
		remote.timeout(timeout);
		return this;
	}

	/** Minecraft version the client runs, e.g. {@code 26.3}. */
	public String minecraftVersion() {
		return remote.connection().hello().get("minecraft").getAsString();
	}

	// ── servers ─────────────────────────────────────────────────────────────

	/** Connects to the server and waits until the player is in the world. */
	public Client join(Server server) {
		return join(server.host(), server.port());
	}

	public Client join(String host, int port) {
		connect(host, port);
		remote.awaitTrue("to join " + host + ":" + port, remote.timeout().plus(JOIN_ALLOWANCE), () -> {
			if (lastDisconnectReason != null) {
				throw new CraftwrightException(name() + " could not join " + host + ":" + port + ": " + lastDisconnectReason.plain());
			}
			return isInWorld();
		}, () -> "Screen: " + screen());
		return this;
	}

	/**
	 * Starts connecting without waiting for the world. Use this when the server is
	 * expected to refuse the player, then check {@link #awaitDisconnect()}.
	 */
	public Client connect(Server server) {
		return connect(server.host(), server.port());
	}

	public Client connect(String host, int port) {
		lastDisconnectReason = null;
		remote.call("client.connect", Json.object("host", host, "port", port));
		return this;
	}

	/** Leaves the server, like "Disconnect" in the game menu. */
	public void leave() {
		remote.call("client.disconnect");
		remote.awaitTrue("to leave the server", () -> !isInWorld(), null);
	}

	/** True while the player is in a world on a server. */
	public boolean isInWorld() {
		return remote.call("client.state").get("inWorld").getAsBoolean();
	}

	/**
	 * Waits until the server closes the connection (kick, ban, whitelist, shutdown)
	 * and returns the reason the server gave, or what the disconnect screen says if it gave none.
	 */
	public Text awaitDisconnect() {
		return awaitDisconnect(remote.timeout());
	}

	public Text awaitDisconnect(Duration timeout) {
		return remote.await("to be disconnected", timeout, this::disconnectReason, () -> "Screen: " + screen());
	}

	/** The reason of the last disconnect since the last connect, if there was one. */
	public Optional<Text> disconnectReason() {
		return Optional.ofNullable(lastDisconnectReason);
	}

	/** Name of the screen the client shows, e.g. {@code TitleScreen}, or an empty string in game. */
	public String screen() {
		return remote.call("client.state").get("screen").getAsString();
	}

	// ── what a player has in front of them ──────────────────────────────────

	/** Writing and reading chat, commands, clickable texts. */
	public Chat chat() {
		return chat;
	}

	/** Titles, action bar, boss bars and the player list. */
	public Hud hud() {
		return hud;
	}

	/** Sidebar, player list scores and teams. */
	public Scoreboard scoreboard() {
		return scoreboard;
	}

	/** Chests, plugin menus, the own inventory and trading. */
	public Menus menus() {
		return menus;
	}

	public Player player() {
		return player;
	}

	public World world() {
		return world;
	}

	/** Every sound the server played for this client. */
	public Received<Sound> sounds() {
		return sounds;
	}

	/** Every burst of particles the server showed this client. */
	public Received<Particles> particles() {
		return particles;
	}

	/**
	 * Saves what the client renders as a PNG. Needs a client started with
	 * {@link ClientBuilder#rendering(boolean) rendering} switched on.
	 */
	public Path screenshot(Path file) {
		if (!process.rendering()) {
			throw new CraftwrightException("Client '" + name() + "' runs without rendering. Start it with rendering(true) to take screenshots.");
		}
		remote.call("client.screenshot", Json.object("file", file.toAbsolutePath().toString()));
		return file;
	}

	/**
	 * Waits until the probe has a value, with this client's timeout. For everything that has no
	 * await of its own: {@code alex.await("the sidebar", () -> alex.scoreboard().sidebar())}.
	 *
	 * @param description what is waited for, completes the sentence "Timed out waiting for …"
	 */
	public <T> T await(String description, Supplier<Optional<T>> probe) {
		return remote.await(description, probe, null);
	}

	/** Waits until the condition holds, with this client's timeout. */
	public void awaitTrue(String description, Supplier<Boolean> condition) {
		remote.awaitTrue(description, condition, null);
	}

	// ── plumbing ────────────────────────────────────────────────────────────

	/**
	 * Sends a raw protocol request. For features the typed API does not cover yet,
	 * see docs/PROTOCOL.md for the methods.
	 */
	public JsonObject call(String method, JsonObject params) {
		return remote.call(method, params);
	}

	public JsonObject call(String method) {
		return remote.call(method);
	}

	/** The last lines the client process printed, for failure reports. */
	public String launcherLog() {
		return process.recentOutput();
	}

	/** Quits the game. */
	@Override
	public void close() {
		try {
			if (!remote.connection().isClosed()) {
				remote.call("client.quit");
			}
		} catch (CraftwrightException ignored) {
			// the process is stopped below either way
		}
		process.stop();
		remote.connection().close();
	}

	@Override
	public String toString() {
		return "Client[" + name() + "]";
	}
}
