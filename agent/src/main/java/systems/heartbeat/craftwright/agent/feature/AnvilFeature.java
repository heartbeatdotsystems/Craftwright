package systems.heartbeat.craftwright.agent.feature;

import com.google.gson.JsonElement;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundRenameItemPacket;
import net.minecraft.world.inventory.AnvilMenu;
import systems.heartbeat.craftwright.agent.game.Game;
import systems.heartbeat.craftwright.agent.protocol.Params;
import systems.heartbeat.craftwright.agent.protocol.RequestException;
import systems.heartbeat.craftwright.agent.protocol.Routes;

/** Typing into the name field of an open anvil, as a player does before taking the result. */
public final class AnvilFeature implements Feature {
	@Override
	public void register(Routes routes) {
		routes.on("anvil.rename", this::rename);
	}

	private JsonElement rename(Minecraft minecraft, Params params) {
		if (!(Game.player(minecraft).containerMenu instanceof AnvilMenu menu)) {
			throw new RequestException("No anvil is open.");
		}
		String name = params.string("name");
		menu.setItemName(name);
		Game.connection(minecraft).send(new ServerboundRenameItemPacket(name));
		return Feature.done();
	}
}
