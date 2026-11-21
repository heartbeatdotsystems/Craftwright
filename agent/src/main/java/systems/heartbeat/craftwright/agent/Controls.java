package systems.heartbeat.craftwright.agent;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/** Everything that takes more than one tick: held keys, walking to a point, mining a block. */
final class Controls {
	boolean forward;
	boolean back;
	boolean left;
	boolean right;
	boolean jump;
	boolean sneak;
	boolean sprint;

	private int jumpTicks;
	private boolean walking;
	private double walkX;
	private double walkZ;
	private BlockPos breaking;
	private Direction breakingFace;
	private boolean keysTouched;

	void jumpOnce() {
		jumpTicks = 2;
	}

	void walkTo(double x, double z) {
		walking = true;
		walkX = x;
		walkZ = z;
	}

	boolean isWalking() {
		return walking;
	}

	void stopWalking() {
		walking = false;
	}

	void startBreaking(Minecraft minecraft, BlockPos pos, Direction face) {
		breaking = pos;
		breakingFace = face;
		minecraft.gameMode.startDestroyBlock(pos, face);
	}

	boolean isBreaking() {
		return breaking != null;
	}

	void stopBreaking(Minecraft minecraft) {
		if (breaking != null && minecraft.gameMode != null) {
			minecraft.gameMode.stopDestroyBlock();
		}
		breaking = null;
	}

	void reset(Minecraft minecraft) {
		forward = back = left = right = jump = sneak = sprint = false;
		jumpTicks = 0;
		walking = false;
		breaking = null;
		applyKeys(minecraft.options, false, false, false, false, false, false, false);
	}

	void tick(Minecraft minecraft) {
		LocalPlayer player = minecraft.player;
		if (player == null || minecraft.level == null) {
			walking = false;
			breaking = null;
			return;
		}
		boolean goForward = forward;
		boolean doJump = jump || jumpTicks > 0;
		if (jumpTicks > 0) {
			jumpTicks--;
		}
		if (walking) {
			double dx = walkX - player.getX();
			double dz = walkZ - player.getZ();
			if (dx * dx + dz * dz < 0.35 * 0.35) {
				walking = false;
			} else {
				player.setYRot((float) Math.toDegrees(Math.atan2(-dx, dz)));
				goForward = true;
				if (player.horizontalCollision && player.onGround()) {
					doJump = true;
				}
			}
		}
		boolean any = goForward || back || left || right || doJump || sneak || sprint;
		// Keys are only driven while a test asks for movement, and released once afterwards.
		if (any || keysTouched) {
			applyKeys(minecraft.options, goForward, back, left, right, doJump, sneak, sprint);
			keysTouched = any;
		}
		if (breaking != null) {
			if (minecraft.level.getBlockState(breaking).isAir()) {
				minecraft.gameMode.stopDestroyBlock();
				breaking = null;
			} else {
				minecraft.gameMode.continueDestroyBlock(breaking, breakingFace);
			}
		}
	}

	private static void applyKeys(Options options, boolean forward, boolean back, boolean left, boolean right,
			boolean jump, boolean sneak, boolean sprint) {
		options.keyUp.setDown(forward);
		options.keyDown.setDown(back);
		options.keyLeft.setDown(left);
		options.keyRight.setDown(right);
		options.keyJump.setDown(jump);
		options.keyShift.setDown(sneak);
		options.keySprint.setDown(sprint);
	}
}
