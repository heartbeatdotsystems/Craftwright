package systems.heartbeat.craftwright.agent.observe;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import systems.heartbeat.craftwright.agent.control.Ticking;
import systems.heartbeat.craftwright.agent.mixin.DisconnectedScreenAccessor;

/** Notices the "Connection lost" screen and reports its reason once. */
public final class DisconnectWatcher implements Ticking {
	private final GameObserver observer;
	private final Runnable onDisconnect;
	private Screen reported;

	public DisconnectWatcher(GameObserver observer, Runnable onDisconnect) {
		this.observer = observer;
		this.onDisconnect = onDisconnect;
	}

	@Override
	public void tick(Minecraft minecraft) {
		Screen screen = minecraft.gui.screen();
		if (screen instanceof DisconnectedScreen && screen != reported) {
			reported = screen;
			onDisconnect.run();
			observer.disconnected(((DisconnectedScreenAccessor) screen).craftwright$details().reason());
		}
	}

	@Override
	public void reset(Minecraft minecraft) {
		// nothing in progress to cancel
	}
}
