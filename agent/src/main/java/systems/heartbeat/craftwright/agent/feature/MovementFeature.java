package systems.heartbeat.craftwright.agent.feature;

import com.google.gson.JsonElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import systems.heartbeat.craftwright.agent.control.Keys;
import systems.heartbeat.craftwright.agent.control.Movement;
import systems.heartbeat.craftwright.agent.game.Aim;
import systems.heartbeat.craftwright.agent.game.Game;
import systems.heartbeat.craftwright.agent.protocol.Params;
import systems.heartbeat.craftwright.agent.protocol.Routes;

/** Looking around and walking. */
public final class MovementFeature implements Feature {
	private final Movement movement;

	public MovementFeature(Movement movement) {
		this.movement = movement;
	}

	@Override
	public void register(Routes routes) {
		routes.on("player.look", this::look);
		routes.on("player.lookAt", this::lookAt);
		routes.on("player.input", this::input);
		routes.on("player.jump", (minecraft, params) -> jump(minecraft));
		routes.on("player.walkTo", this::walkTo);
		routes.on("player.walking", (minecraft, params) -> Feature.value("walking", movement.isWalking()));
	}

	private JsonElement look(Minecraft minecraft, Params params) {
		LocalPlayer player = Game.player(minecraft);
		player.setYRot((float) params.number("yaw"));
		player.setXRot(Math.clamp((float) params.number("pitch"), -90f, 90f));
		return Feature.done();
	}

	private JsonElement lookAt(Minecraft minecraft, Params params) {
		Aim.turnTo(Game.player(minecraft), new Vec3(params.number("x"), params.number("y"), params.number("z")));
		return Feature.done();
	}

	private JsonElement input(Minecraft minecraft, Params params) {
		Game.player(minecraft);
		movement.hold(new Keys(params.flag("forward"), params.flag("back"), params.flag("left"), params.flag("right"),
			params.flag("jump"), params.flag("sneak"), params.flag("sprint")));
		return Feature.done();
	}

	private JsonElement jump(Minecraft minecraft) {
		Game.player(minecraft);
		movement.jumpOnce();
		return Feature.done();
	}

	private JsonElement walkTo(Minecraft minecraft, Params params) {
		Game.player(minecraft);
		Game.closeScreenForPlay(minecraft);
		movement.walkTo(params.number("x"), params.number("z"));
		return Feature.done();
	}
}
