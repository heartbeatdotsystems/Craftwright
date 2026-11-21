package systems.heartbeat.craftwright.agent;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import systems.heartbeat.craftwright.agent.mixin.DisconnectedScreenAccessor;

/**
 * The agent inside the client. It connects to the driver once the game has loaded,
 * runs every request on the client thread and pushes events back.
 */
public final class Agent {
	public static final int PROTOCOL_VERSION = 1;
	private static final int CHAT_MEMORY = 200;
	private static final Agent INSTANCE = new Agent();

	private final Deque<Component> recentChat = new ArrayDeque<>();
	private final Controls controls = new Controls();
	private final Requests requests = new Requests(this, controls);
	private BufferedWriter writer;
	private boolean started;
	private Screen reportedDisconnect;

	private Agent() {
	}

	public static Agent get() {
		return INSTANCE;
	}

	Deque<Component> recentChat() {
		return recentChat;
	}

	/** Called at the end of every client tick. */
	public void tick(Minecraft minecraft) {
		if (!AgentConfig.enabled()) {
			return;
		}
		if (!started) {
			if (minecraft.gui.overlay() != null || minecraft.gui.screen() == null) {
				return;
			}
			started = true;
			connect(minecraft);
		}
		controls.tick(minecraft);
		Screen screen = minecraft.gui.screen();
		if (screen instanceof DisconnectedScreen && screen != reportedDisconnect) {
			reportedDisconnect = screen;
			controls.reset(minecraft);
			JsonObject data = new JsonObject();
			data.add("reason", Texts.wire(((DisconnectedScreenAccessor) screen).craftwright$details().reason()));
			event("disconnect", data);
		}
	}

	private void connect(Minecraft minecraft) {
		try {
			String[] address = AgentConfig.CONNECT.split(":");
			Socket socket = new Socket(address[0], Integer.parseInt(address[1]));
			socket.setTcpNoDelay(true);
			writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
			JsonObject hello = new JsonObject();
			hello.addProperty("token", AgentConfig.TOKEN);
			hello.addProperty("client", AgentConfig.CLIENT_ID);
			hello.addProperty("protocol", PROTOCOL_VERSION);
			hello.addProperty("agent", version("craftwright-agent"));
			hello.addProperty("minecraft", version("minecraft"));
			hello.addProperty("player", minecraft.getUser().getName());
			send(hello);
			Thread reader = new Thread(() -> readLoop(minecraft, socket), "craftwright-agent");
			reader.setDaemon(true);
			reader.start();
			System.out.println("[Craftwright] connected to the driver at " + AgentConfig.CONNECT);
		} catch (IOException | RuntimeException e) {
			System.err.println("[Craftwright] cannot reach the driver at " + AgentConfig.CONNECT + ": " + e);
			minecraft.stop();
		}
	}

	private static String version(String modId) {
		return FabricLoader.getInstance().getModContainer(modId)
			.map(mod -> mod.getMetadata().getVersion().getFriendlyString()).orElse("unknown");
	}

	private void readLoop(Minecraft minecraft, Socket socket) {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.isEmpty()) {
					continue;
				}
				JsonObject request = JsonParser.parseString(line).getAsJsonObject();
				minecraft.execute(() -> answer(minecraft, request));
			}
		} catch (IOException | RuntimeException e) {
			// the driver went away
		}
		// A test client without its test has no reason to keep running.
		System.out.println("[Craftwright] driver disconnected, quitting");
		minecraft.execute(minecraft::stop);
	}

	private void answer(Minecraft minecraft, JsonObject request) {
		JsonObject response = new JsonObject();
		response.add("id", request.get("id"));
		String method = request.get("method").getAsString();
		JsonObject params = request.has("params") && request.get("params").isJsonObject()
			? request.getAsJsonObject("params") : new JsonObject();
		try {
			response.add("result", requests.handle(minecraft, method, params));
		} catch (RequestException e) {
			response.add("error", error(e.getMessage()));
		} catch (RuntimeException e) {
			e.printStackTrace();
			response.add("error", error(method + " crashed in the client: " + e));
		}
		send(response);
	}

	private static JsonObject error(String message) {
		JsonObject error = new JsonObject();
		error.addProperty("message", message);
		return error;
	}

	public void chatEvent(String kind, Component text) {
		if (!AgentConfig.enabled()) {
			return;
		}
		synchronized (recentChat) {
			recentChat.addLast(text);
			while (recentChat.size() > CHAT_MEMORY) {
				recentChat.removeFirst();
			}
		}
		JsonObject data = new JsonObject();
		data.addProperty("kind", kind);
		data.add("text", Texts.wire(text));
		event("chat", data);
	}

	public void textEvent(String event, Component text) {
		if (!AgentConfig.enabled() || text == null) {
			return;
		}
		JsonObject data = new JsonObject();
		data.add("text", Texts.wire(text));
		event(event, data);
	}

	void event(String name, JsonElement data) {
		JsonObject message = new JsonObject();
		message.addProperty("event", name);
		message.add("data", data);
		send(message);
	}

	private void send(JsonObject message) {
		BufferedWriter out = writer;
		if (out == null) {
			return;
		}
		try {
			synchronized (out) {
				out.write(message.toString());
				out.write('\n');
				out.flush();
			}
		} catch (IOException e) {
			// the read loop notices the lost connection and quits the game
		}
	}
}
