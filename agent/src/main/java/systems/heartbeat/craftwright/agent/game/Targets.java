package systems.heartbeat.craftwright.agent.game;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import systems.heartbeat.craftwright.agent.protocol.RequestException;
import systems.heartbeat.craftwright.agent.wire.EntityWire;
import systems.heartbeat.craftwright.agent.wire.Ids;

/** Finds the entity a test means when it says "villager" or "Steve". */
public final class Targets {
	private Targets() {
	}

	/** The nearest entity within reach whose type id, name or UUID is {@code target}. */
	public static Entity nearestInReach(Minecraft minecraft, String target) {
		LocalPlayer player = Game.player(minecraft);
		double reach = player.entityInteractionRange() + 1.0;
		Entity nearest = null;
		for (Entity entity : minecraft.level.entitiesForRendering()) {
			if (entity == player || !matches(entity, target) || entity.distanceTo(player) > reach) {
				continue;
			}
			if (nearest == null || entity.distanceTo(player) < nearest.distanceTo(player)) {
				nearest = entity;
			}
		}
		if (nearest == null) {
			throw new RequestException("No entity '" + target + "' within reach (" + reach + " blocks)");
		}
		return nearest;
	}

	private static boolean matches(Entity entity, String target) {
		return EntityWire.type(entity).equals(Ids.qualify(target))
			|| entity.getName().getString().equals(target)
			|| entity.getUUID().toString().equals(target);
	}
}
