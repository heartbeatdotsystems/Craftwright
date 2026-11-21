package systems.heartbeat.craftwright.agent.game;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import systems.heartbeat.craftwright.agent.protocol.RequestException;

/** Where the player looks and what is within reach. */
public final class Aim {
	private Aim() {
	}

	public static void turnTo(LocalPlayer player, Vec3 target) {
		Vec3 eye = player.getEyePosition();
		double dx = target.x - eye.x;
		double dy = target.y - eye.y;
		double dz = target.z - eye.z;
		player.setYRot(yawTowards(dx, dz));
		player.setXRot((float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))));
	}

	public static float yawTowards(double dx, double dz) {
		return (float) Math.toDegrees(Math.atan2(-dx, dz));
	}

	/** The side of the block the player is looking at when standing where it stands. */
	public static Direction faceTowards(LocalPlayer player, Vec3 center) {
		Vec3 eye = player.getEyePosition();
		return Direction.getApproximateNearest(eye.x - center.x, eye.y - center.y, eye.z - center.z);
	}

	public static void requireReach(LocalPlayer player, BlockPos pos) {
		double reach = player.blockInteractionRange() + 1.0;
		if (player.getEyePosition().distanceTo(Vec3.atCenterOf(pos)) > reach) {
			throw new RequestException("The block at " + pos.toShortString() + " is out of reach, walk closer first");
		}
	}
}
