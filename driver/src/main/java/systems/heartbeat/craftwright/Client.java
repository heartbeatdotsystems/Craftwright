package systems.heartbeat.craftwright;

import com.google.gson.JsonObject;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import systems.heartbeat.craftwright.internal.AgentConnection;
import systems.heartbeat.craftwright.internal.Await;

/**
 * One real Minecraft client, running without a window.
 *
 * <pre>{@code
 * Client alex = craftwright.client("Alex").start();
 * alex.join(server);
 * alex.command("spawn");
 * alex.chat().await("Welcome back");
 * }</pre>
 */
public final class Client implements AutoCloseable {
	private final String name;
	private final AgentConnection connection;
	private final Runnable stopProcess;
	private final Supplier<String> launcherLog;
	private final Path directory;
	private final boolean rendering;
	private final Messages chat;
	private final Hud hud;
	private final Player player;
	private final World world;
	private volatile Duration timeout;
	private volatile Text lastDisconnectReason;

	Client(String name, AgentConnection connection, Runnable stopProcess, Supplier<String> launcherLog, Path directory,
			boolean rendering, Duration timeout) {
		this.name = name;
		this.connection = connection;
		this.stopProcess = stopProcess;
		this.launcherLog = launcherLog;
		this.directory = directory;
		this.rendering = rendering;
		this.timeout = timeout;
		this.chat = new Messages("chat message", this);
		this.hud = new Hud(this);
		this.player = new Player(this);
		this.world = new World(this);
		connection.onEvent(this::onEvent);
	}

	private void onEvent(JsonObject message) {
		JsonObject data = message.has("data") ? message.getAsJsonObject("data") : new JsonObject();
		switch (message.get("event").getAsString()) {
			case "chat" -> chat.add(Text.of(data.get("text")));
			case "title" -> hud.titles().add(Text.of(data.get("text")));
			case "subtitle" -> hud.subtitles().add(Text.of(data.get("text")));
			case "actionBar" -> hud.actionBar().add(Text.of(data.get("text")));
			case "disconnect" -> lastDisconnectReason = Text.of(data.get("reason"));
			default -> {
				// events this driver version does not know are ignored
			}
		}
	}

	/** The account name of this client. */
	public String name() {
		return name;
	}

	/** Game directory of this client, with {@code logs/latest.log}, screenshots and options. */
	public Path directory() {
		return directory;
	}

	/** How long {@code await…} calls wait unless told otherwise. */
	public Duration timeout() {
		return timeout;
	}

	public Client timeout(Duration timeout) {
		this.timeout = timeout;
		return this;
	}

	// ── connection ──────────────────────────────────────────────────────────

	/** Connects to the server and waits until the player is in the world. */
	public Client join(Server server) {
		return join(server.host(), server.port());
	}

	public Client join(String host, int port) {
		connect(host, port);
		Await.untilTrue(name + " to join " + host + ":" + port, timeout.plus(Duration.ofSeconds(30)), () -> {
			if (lastDisconnectReason != null) {
				throw new CraftwrightException(name + " could not join " + host + ":" + port + ": " + lastDisconnectReason.plain());
			}
			return isInWorld();
		}, () -> "Screen: " + screen());
		return this;
	}

	/**
	 * Starts connecting without waiting for the world. Use this when the server is
	 * expected to refuse the player, then check {@link #awaitDisconnect()}.
	 */
	public Client connect(String host, int port) {
		lastDisconnectReason = null;
		JsonObject params = new JsonObject();
		params.addProperty("host", host);
		params.addProperty("port", port);
		call("client.connect", params);
		return this;
	}

	public Client connect(Server server) {
		return connect(server.host(), server.port());
	}

	/** Leaves the server, like "Disconnect" in the game menu. */
	public void leave() {
		call("client.disconnect");
		Await.untilTrue(name + " to leave the server", timeout, () -> !isInWorld(), null);
	}

	/** True while the player is in a world on a server. */
	public boolean isInWorld() {
		return call("client.state").get("inWorld").getAsBoolean();
	}

	/**
	 * Waits until the server closes the connection (kick, ban, whitelist, shutdown)
	 * and returns the reason shown on the disconnect screen.
	 */
	public Text awaitDisconnect() {
		return awaitDisconnect(timeout);
	}

	public Text awaitDisconnect(Duration timeout) {
		return Await.until(name + " to be disconnected", timeout, () -> Optional.ofNullable(lastDisconnectReason),
			() -> "Screen: " + screen());
	}

	/** The reason of the last disconnect since the last connect, if there was one. */
	public Optional<Text> disconnectReason() {
		return Optional.ofNullable(lastDisconnectReason);
	}

	/** Name of the screen the client shows, e.g. {@code TitleScreen}, or an empty string in game. */
	public String screen() {
		return call("client.state").get("screen").getAsString();
	}

	// ── talking ─────────────────────────────────────────────────────────────

	/** Sends a chat message. */
	public void say(String message) {
		JsonObject params = new JsonObject();
		params.addProperty("message", message);
		call("chat.send", params);
	}

	/** Runs a command as the player, with or without the leading slash. */
	public void command(String command) {
		JsonObject params = new JsonObject();
		params.addProperty("command", command.startsWith("/") ? command.substring(1) : command);
		call("chat.command", params);
	}

	/** Chat and system messages this client received. */
	public Messages chat() {
		return chat;
	}

	/**
	 * Clicks a clickable chat text, like a player clicking it in the chat window.
	 * Runs the command or suggestion behind the first received message that contains the words.
	 */
	public void clickChat(String messageContains) {
		JsonObject params = new JsonObject();
		params.addProperty("contains", messageContains);
		call("chat.click", params);
	}

	// ── looking ─────────────────────────────────────────────────────────────

	/** Titles, action bar, boss bars, tab list and sidebar. */
	public Hud hud() {
		return hud;
	}

	public Player player() {
		return player;
	}

	public World world() {
		return world;
	}

	/** The container screen that is open right now, if any. */
	public Optional<Menu> menu() {
		return Menu.current(this);
	}

	/** Waits for any container screen to open. */
	public Menu awaitMenu() {
		return Menu.awaitOpen(this, menu -> true, "", timeout);
	}

	/** Waits for a container screen whose title contains the given words. */
	public Menu awaitMenu(String titleContains) {
		return Menu.awaitOpen(this, menu -> menu.title().contains(titleContains), "titled \"" + titleContains + "\"", timeout);
	}

	public Menu awaitMenu(Predicate<Menu> condition) {
		return Menu.awaitOpen(this, condition, "matching the condition", timeout);
	}

	/** Waits until no container screen is open. */
	public void awaitMenuClosed() {
		Await.untilTrue("the menu of " + name + " to close", timeout, () -> menu().isEmpty(), null);
	}

	/**
	 * Saves what the client renders as a PNG. Needs a client started with
	 * {@link ClientBuilder#rendering(boolean) rendering} switched on.
	 */
	public Path screenshot(Path file) {
		if (!rendering) {
			throw new CraftwrightException("Client '" + name + "' runs without rendering. Start it with rendering(true) to take screenshots.");
		}
		JsonObject params = new JsonObject();
		params.addProperty("file", file.toAbsolutePath().toString());
		call("client.screenshot", params);
		Await.untilTrue("the screenshot " + file, timeout, () -> java.nio.file.Files.exists(file), null);
		return file;
	}

	// ── plumbing ────────────────────────────────────────────────────────────

	/** Minecraft version the client runs, e.g. {@code 26.3}. */
	public String minecraftVersion() {
		return connection.hello().get("minecraft").getAsString();
	}

	/**
	 * Sends a raw protocol request. For features the typed API does not cover yet,
	 * see docs/PROTOCOL.md for the methods.
	 */
	public JsonObject call(String method, JsonObject params) {
		return connection.call(method, params);
	}

	public JsonObject call(String method) {
		return connection.call(method, null);
	}

	/** The last lines the client process printed, for failure reports. */
	public String launcherLog() {
		return launcherLog.get();
	}

	/** Quits the game. */
	@Override
	public void close() {
		try {
			if (!connection.isClosed()) {
				connection.call("client.quit", null);
			}
		} catch (CraftwrightException ignored) {
			// the process is stopped below either way
		}
		stopProcess.run();
		connection.close();
	}

	@Override
	public String toString() {
		return "Client[" + name + "]";
	}
}
