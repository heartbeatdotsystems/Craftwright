package systems.heartbeat.craftwright.agent.control;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import systems.heartbeat.craftwright.agent.game.Aim;

/**
 * Moves the player the way a keyboard would: by holding the game's own movement keys.
 * Collision, sneaking at edges, hunger and server-side movement checks all stay real.
 */
public final class Movement implements Ticking {
	private static final double ARRIVAL_DISTANCE = 0.35;
	private static final int JUMP_TICKS = 2;

	private Keys held = Keys.NONE;
	private int jumpTicks;
	private boolean walking;
	private double targetX;
	private double targetZ;
	private boolean keysTouched;

	public void hold(Keys keys) {
		walking = false;
		held = keys;
	}

	public void jumpOnce() {
		jumpTicks = JUMP_TICKS;
	}

	public void walkTo(double x, double z) {
		walking = true;
		targetX = x;
		targetZ = z;
	}

	public boolean isWalking() {
		return walking;
	}

	@Override
	public void reset(Minecraft minecraft) {
		held = Keys.NONE;
		jumpTicks = 0;
		walking = false;
		keysTouched = false;
		press(minecraft.options, Keys.NONE);
	}

	@Override
	public void tick(Minecraft minecraft) {
		LocalPlayer player = minecraft.player;
		if (player == null) {
			return;
		}
		Keys keys = held;
		if (jumpTicks > 0) {
			jumpTicks--;
			keys = keys.withJump();
		}
		if (walking) {
			keys = steer(player, keys);
		}
		// Keys are only driven while a test asks for movement, and released once afterwards.
		if (keys.any() || keysTouched) {
			press(minecraft.options, keys);
			keysTouched = keys.any();
		}
	}

	private Keys steer(LocalPlayer player, Keys keys) {
		double dx = targetX - player.getX();
		double dz = targetZ - player.getZ();
		if (dx * dx + dz * dz < ARRIVAL_DISTANCE * ARRIVAL_DISTANCE) {
			walking = false;
			return keys;
		}
		player.setYRot(Aim.yawTowards(dx, dz));
		boolean blocked = player.horizontalCollision && player.onGround();
		return blocked ? keys.withForward().withJump() : keys.withForward();
	}

	private static void press(Options options, Keys keys) {
		options.keyUp.setDown(keys.forward());
		options.keyDown.setDown(keys.back());
		options.keyLeft.setDown(keys.left());
		options.keyRight.setDown(keys.right());
		options.keyJump.setDown(keys.jump());
		options.keyShift.setDown(keys.sneak());
		options.keySprint.setDown(keys.sprint());
	}
}
