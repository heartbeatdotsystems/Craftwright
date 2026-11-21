package systems.heartbeat.craftwright.agent.game;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import systems.heartbeat.craftwright.agent.protocol.RequestException;

/** The parts of the running game a request needs, or a clear error when the client is not that far yet. */
public final class Game {
	private Game() {
	}

	public static boolean inWorld(Minecraft minecraft) {
		return minecraft.player != null && minecraft.level != null && minecraft.getConnection() != null;
	}

	public static LocalPlayer player(Minecraft minecraft) {
		if (minecraft.player == null || minecraft.level == null) {
			throw new RequestException("The client is not in a world");
		}
		return minecraft.player;
	}

	public static ClientLevel level(Minecraft minecraft) {
		player(minecraft);
		return minecraft.level;
	}

	public static ClientPacketListener connection(Minecraft minecraft) {
		ClientPacketListener connection = minecraft.getConnection();
		if (connection == null) {
			throw new RequestException("The client is not connected to a server");
		}
		return connection;
	}

	/**
	 * Movement, mining and clicking in the world only work while no screen has the keyboard,
	 * like for a real player. Closes an open menu the way Escape would.
	 */
	public static void closeScreenForPlay(Minecraft minecraft) {
		LocalPlayer player = minecraft.player;
		if (minecraft.gui.screen() == null || player == null || player.isDeadOrDying()) {
			return;
		}
		if (player.containerMenu != player.inventoryMenu) {
			player.closeContainer();
		} else {
			minecraft.gui.setScreen(null);
		}
	}
}
