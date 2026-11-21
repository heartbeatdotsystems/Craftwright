package systems.heartbeat.craftwright.agent.feature;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Inventory;
import systems.heartbeat.craftwright.agent.game.Game;
import systems.heartbeat.craftwright.agent.protocol.Params;
import systems.heartbeat.craftwright.agent.protocol.RequestException;
import systems.heartbeat.craftwright.agent.protocol.Routes;
import systems.heartbeat.craftwright.agent.wire.Ids;
import systems.heartbeat.craftwright.agent.wire.ItemWire;

/** The player itself: its state, what it carries and which hand holds what. */
public final class PlayerFeature implements Feature {
	private static final int HOTBAR_SLOTS = 9;

	@Override
	public void register(Routes routes) {
		routes.on("player.state", (minecraft, params) -> state(minecraft));
		routes.on("player.inventory", (minecraft, params) -> inventory(minecraft));
		routes.on("player.heldItem", (minecraft, params) -> heldItem(minecraft));
		routes.on("player.selectHotbar", this::selectHotbar);
		routes.on("player.swapHands", (minecraft, params) -> swapHands(minecraft));
		routes.on("player.drop", this::drop);
		routes.on("player.respawn", (minecraft, params) -> respawn(minecraft));
	}

	private JsonElement state(Minecraft minecraft) {
		LocalPlayer player = Game.player(minecraft);
		JsonObject state = new JsonObject();
		state.addProperty("x", player.getX());
		state.addProperty("y", player.getY());
		state.addProperty("z", player.getZ());
		state.addProperty("yaw", player.getYRot());
		state.addProperty("pitch", player.getXRot());
		state.addProperty("health", player.getHealth());
		state.addProperty("food", player.getFoodData().getFoodLevel());
		state.addProperty("saturation", player.getFoodData().getSaturationLevel());
		state.addProperty("experienceLevel", player.experienceLevel);
		state.addProperty("experienceProgress", player.experienceProgress);
		state.addProperty("gameMode", minecraft.gameMode.getPlayerMode().getName());
		state.addProperty("dimension", String.valueOf(minecraft.level.dimension().identifier()));
		state.addProperty("onGround", player.onGround());
		state.addProperty("dead", player.isDeadOrDying());
		state.addProperty("sneaking", player.isCrouching());
		state.addProperty("sprinting", player.isSprinting());
		state.addProperty("flying", player.getAbilities().flying);
		state.addProperty("mayFly", player.getAbilities().mayfly);
		state.addProperty("selectedHotbarSlot", player.getInventory().getSelectedSlot());
		state.add("effects", effects(player));
		return state;
	}

	private static JsonArray effects(LocalPlayer player) {
		JsonArray effects = new JsonArray();
		for (MobEffectInstance effect : player.getActiveEffects()) {
			JsonObject wire = new JsonObject();
			wire.addProperty("id", Ids.of(effect.getEffect()));
			wire.addProperty("amplifier", effect.getAmplifier());
			wire.addProperty("duration", effect.isInfiniteDuration() ? -1 : effect.getDuration());
			effects.add(wire);
		}
		return effects;
	}

	private JsonElement inventory(Minecraft minecraft) {
		Inventory inventory = Game.player(minecraft).getInventory();
		JsonArray slots = new JsonArray();
		for (int index = 0; index < inventory.getContainerSize(); index++) {
			slots.add(ItemWire.of(inventory.getItem(index)));
		}
		JsonObject result = new JsonObject();
		result.add("slots", slots);
		return result;
	}

	private JsonElement heldItem(Minecraft minecraft) {
		LocalPlayer player = Game.player(minecraft);
		JsonObject result = new JsonObject();
		result.add("item", ItemWire.of(player.getMainHandItem()));
		result.add("offHand", ItemWire.of(player.getOffhandItem()));
		return result;
	}

	private JsonElement selectHotbar(Minecraft minecraft, Params params) {
		int slot = params.integer("slot");
		if (slot < 0 || slot >= HOTBAR_SLOTS) {
			throw new RequestException("Hotbar slots go from 0 to " + (HOTBAR_SLOTS - 1) + ", got " + slot);
		}
		Game.player(minecraft).getInventory().setSelectedSlot(slot);
		return Feature.done();
	}

	private JsonElement swapHands(Minecraft minecraft) {
		Game.player(minecraft);
		Game.connection(minecraft).send(new ServerboundPlayerActionPacket(
			ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));
		return Feature.done();
	}

	private JsonElement drop(Minecraft minecraft, Params params) {
		minecraft.gameMode.dropItem(Game.player(minecraft), params.flag("wholeStack"));
		return Feature.done();
	}

	private JsonElement respawn(Minecraft minecraft) {
		if (!Game.player(minecraft).isDeadOrDying()) {
			throw new RequestException("The player is not dead");
		}
		Game.connection(minecraft).send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
		minecraft.gui.setScreen(null);
		return Feature.done();
	}
}
