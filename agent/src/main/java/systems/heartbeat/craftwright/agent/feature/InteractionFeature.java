package systems.heartbeat.craftwright.agent.feature;

import com.google.gson.JsonElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import systems.heartbeat.craftwright.agent.control.Mining;
import systems.heartbeat.craftwright.agent.game.Aim;
import systems.heartbeat.craftwright.agent.game.Game;
import systems.heartbeat.craftwright.agent.game.Targets;
import systems.heartbeat.craftwright.agent.mixin.MinecraftInvoker;
import systems.heartbeat.craftwright.agent.protocol.Params;
import systems.heartbeat.craftwright.agent.protocol.RequestException;
import systems.heartbeat.craftwright.agent.protocol.Routes;

/** The two mouse buttons: using and attacking whatever is in front of the player, blocks and entities by name. */
public final class InteractionFeature implements Feature {
	private final Mining mining;

	public InteractionFeature(Mining mining) {
		this.mining = mining;
	}

	@Override
	public void register(Routes routes) {
		routes.on("player.use", (minecraft, params) -> use(minecraft));
		routes.on("player.attack", (minecraft, params) -> attack(minecraft));
		routes.on("player.useBlock", this::useBlock);
		routes.on("player.breakBlock", this::breakBlock);
		routes.on("player.breaking", (minecraft, params) -> Feature.value("breaking", mining.isMining()));
		routes.on("player.stopBreaking", (minecraft, params) -> stopBreaking(minecraft));
		routes.on("player.useEntity", this::useEntity);
		routes.on("player.attackEntity", this::attackEntity);
	}

	private JsonElement use(Minecraft minecraft) {
		aimWhereLooking(minecraft);
		((MinecraftInvoker) minecraft).craftwright$startUseItem();
		return Feature.done();
	}

	private JsonElement attack(Minecraft minecraft) {
		aimWhereLooking(minecraft);
		((MinecraftInvoker) minecraft).craftwright$startAttack();
		return Feature.done();
	}

	/** The game only updates its crosshair target while rendering, a headless client never does. */
	private static void aimWhereLooking(Minecraft minecraft) {
		LocalPlayer player = Game.player(minecraft);
		Game.closeScreenForPlay(minecraft);
		minecraft.hitResult = player.raycastHitResult(1.0f, player);
	}

	/** Right click on a block: opens it, or places the held block against the given side. */
	private JsonElement useBlock(Minecraft minecraft, Params params) {
		LocalPlayer player = Game.player(minecraft);
		BlockPos pos = blockPos(params);
		Aim.requireReach(player, pos);
		Vec3 center = Vec3.atCenterOf(pos);
		Aim.turnTo(player, center);
		Direction face = params.has("face") ? face(params.string("face")) : Aim.faceTowards(player, center);
		minecraft.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, new BlockHitResult(center, face, pos, false));
		return Feature.done();
	}

	private JsonElement breakBlock(Minecraft minecraft, Params params) {
		LocalPlayer player = Game.player(minecraft);
		BlockPos pos = blockPos(params);
		Aim.requireReach(player, pos);
		if (minecraft.level.getBlockState(pos).isAir()) {
			throw new RequestException("There is no block at " + pos.toShortString());
		}
		Game.closeScreenForPlay(minecraft);
		Vec3 center = Vec3.atCenterOf(pos);
		Aim.turnTo(player, center);
		mining.start(minecraft, pos, Aim.faceTowards(player, center));
		return Feature.done();
	}

	private JsonElement stopBreaking(Minecraft minecraft) {
		mining.stop(minecraft);
		return Feature.done();
	}

	private JsonElement useEntity(Minecraft minecraft, Params params) {
		LocalPlayer player = Game.player(minecraft);
		Entity entity = Targets.nearestInReach(minecraft, params.string("target"));
		Aim.turnTo(player, entity.getEyePosition());
		minecraft.gameMode.interact(player, entity, new EntityHitResult(entity), InteractionHand.MAIN_HAND);
		return Feature.done();
	}

	private JsonElement attackEntity(Minecraft minecraft, Params params) {
		LocalPlayer player = Game.player(minecraft);
		Entity entity = Targets.nearestInReach(minecraft, params.string("target"));
		Aim.turnTo(player, entity.getEyePosition());
		minecraft.gameMode.attack(player, entity);
		return Feature.done();
	}

	private static BlockPos blockPos(Params params) {
		return BlockPos.containing(params.number("x"), params.number("y"), params.number("z"));
	}

	private static Direction face(String name) {
		Direction direction = Direction.byName(name);
		if (direction == null) {
			throw new RequestException("Unknown block side '" + name + "'. Use up, down, north, south, east or west.");
		}
		return direction;
	}
}
