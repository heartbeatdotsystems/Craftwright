package systems.heartbeat.craftwright.agent.observe;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import systems.heartbeat.craftwright.agent.control.Ticking;
import systems.heartbeat.craftwright.agent.mixin.DisconnectedScreenAccessor;

/**
 * Notices the "Connection lost" screen and reports its reason once.
 * The reason is the one the server sent. The screen can miss it: when the server closes the
 * connection before the client thread has handled the kick, the game shows a bare "Disconnected".
 */
public final class DisconnectWatcher implements Ticking {
	private final GameObserver observer;
	private final Runnable onDisconnect;
	private volatile Component serverReason;
	private Screen reported;

	public DisconnectWatcher(GameObserver observer, Runnable onDisconnect) {
		this.observer = observer;
		this.onDisconnect = onDisconnect;
	}

	/** The reason from the server's disconnect packet. Called on the network thread. */
	public void serverSaid(Component reason) {
		serverReason = reason;
	}

	@Override
	public void tick(Minecraft minecraft) {
		Screen screen = minecraft.gui.screen();
		if (screen instanceof DisconnectedScreen && screen != reported) {
			reported = screen;
			onDisconnect.run();
			observer.disconnected(reasonOf(screen));
		}
	}

	private Component reasonOf(Screen screen) {
		Component fromServer = serverReason;
		serverReason = null;
		return fromServer != null ? fromServer : ((DisconnectedScreenAccessor) screen).craftwright$details().reason();
	}

	@Override
	public void reset(Minecraft minecraft) {
		// nothing in progress to cancel
	}
}
