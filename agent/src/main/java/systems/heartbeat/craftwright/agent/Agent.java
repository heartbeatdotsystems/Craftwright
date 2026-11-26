package systems.heartbeat.craftwright.agent;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import systems.heartbeat.craftwright.agent.control.Mining;
import systems.heartbeat.craftwright.agent.control.Movement;
import systems.heartbeat.craftwright.agent.control.Ticking;
import systems.heartbeat.craftwright.agent.feature.ChatFeature;
import systems.heartbeat.craftwright.agent.feature.ClientFeature;
import systems.heartbeat.craftwright.agent.feature.Feature;
import systems.heartbeat.craftwright.agent.feature.HudFeature;
import systems.heartbeat.craftwright.agent.feature.InteractionFeature;
import systems.heartbeat.craftwright.agent.feature.MenuFeature;
import systems.heartbeat.craftwright.agent.feature.MovementFeature;
import systems.heartbeat.craftwright.agent.feature.PlayerFeature;
import systems.heartbeat.craftwright.agent.feature.ScoreboardFeature;
import systems.heartbeat.craftwright.agent.feature.AnvilFeature;
import systems.heartbeat.craftwright.agent.feature.TradeFeature;
import systems.heartbeat.craftwright.agent.feature.WorldFeature;
import systems.heartbeat.craftwright.agent.observe.ChatLog;
import systems.heartbeat.craftwright.agent.observe.DisconnectWatcher;
import systems.heartbeat.craftwright.agent.observe.GameObserver;
import systems.heartbeat.craftwright.agent.protocol.DriverLink;
import systems.heartbeat.craftwright.agent.protocol.Messages;
import systems.heartbeat.craftwright.agent.protocol.Router;

/**
 * The agent inside the client. It puts the parts together: the link to the driver,
 * the features that answer requests, and the things that need every tick.
 */
public final class Agent {
	private static final Agent INSTANCE = new Agent();

	private final Router router = new Router();
	private final ChatLog chatLog = new ChatLog();
	private final GameObserver observer = new GameObserver(this::emit, chatLog);
	private final Movement movement = new Movement();
	private final Mining mining = new Mining();
	private final DisconnectWatcher disconnects = new DisconnectWatcher(observer, this::resetControls);
	private final List<Ticking> ticking = List.of(movement, mining, disconnects);
	private DriverLink link;
	private boolean started;

	private Agent() {
		List<Feature> features = List.of(
			new ClientFeature(this::resetControls),
			new ChatFeature(chatLog),
			new HudFeature(),
			new ScoreboardFeature(),
			new MenuFeature(),
			new TradeFeature(),
			new AnvilFeature(),
			new PlayerFeature(),
			new MovementFeature(movement),
			new InteractionFeature(mining),
			new WorldFeature());
		features.forEach(feature -> feature.register(router));
	}

	public static Agent get() {
		return INSTANCE;
	}

	/** Where the mixins report what the client was shown. */
	public GameObserver observer() {
		return observer;
	}

	/** Where the mixins report that the server closes the connection. */
	public DisconnectWatcher disconnects() {
		return disconnects;
	}

	/** Called at the end of every client tick. */
	public void tick(Minecraft minecraft) {
		if (!AgentConfig.enabled()) {
			return;
		}
		if (!started) {
			if (!finishedLoading(minecraft)) {
				return;
			}
			started = true;
			connect(minecraft);
		}
		ticking.forEach(task -> task.tick(minecraft));
	}

	private static boolean finishedLoading(Minecraft minecraft) {
		return minecraft.gui.overlay() == null && minecraft.gui.screen() != null;
	}

	private void connect(Minecraft minecraft) {
		try {
			String[] address = AgentConfig.CONNECT.split(":");
			link = DriverLink.open(address[0], Integer.parseInt(address[1]));
			link.send(Messages.hello(AgentConfig.TOKEN, AgentConfig.CLIENT_ID, version("craftwright-agent"), version("minecraft"),
				minecraft.getUser().getName()));
			link.listen(
				request -> minecraft.execute(() -> router.dispatch(minecraft, request, link::send)),
				() -> quitWithoutDriver(minecraft));
			System.out.println("[Craftwright] connected to the driver at " + AgentConfig.CONNECT);
		} catch (IOException | RuntimeException e) {
			System.err.println("[Craftwright] cannot reach the driver at " + AgentConfig.CONNECT + ": " + e);
			minecraft.stop();
		}
	}

	/** A test client without its test has no reason to keep running. */
	private static void quitWithoutDriver(Minecraft minecraft) {
		System.out.println("[Craftwright] driver disconnected, quitting");
		minecraft.execute(minecraft::stop);
	}

	private void resetControls() {
		Minecraft minecraft = Minecraft.getInstance();
		ticking.forEach(task -> task.reset(minecraft));
	}

	private void emit(String name, JsonObject data) {
		if (link != null) {
			link.send(Messages.event(name, data));
		}
	}

	private static String version(String modId) {
		return FabricLoader.getInstance().getModContainer(modId)
			.map(mod -> mod.getMetadata().getVersion().getFriendlyString()).orElse("unknown");
	}
}
