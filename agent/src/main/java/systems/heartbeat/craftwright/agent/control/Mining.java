package systems.heartbeat.craftwright.agent.control;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/** Keeps the attack button on one block until it breaks, with the real mining time of tool and game mode. */
public final class Mining implements Ticking {
	private BlockPos target;
	private Direction face;

	public void start(Minecraft minecraft, BlockPos pos, Direction side) {
		target = pos;
		face = side;
		minecraft.gameMode.startDestroyBlock(pos, side);
	}

	public boolean isMining() {
		return target != null;
	}

	public void stop(Minecraft minecraft) {
		if (target != null && minecraft.gameMode != null) {
			minecraft.gameMode.stopDestroyBlock();
		}
		target = null;
	}

	@Override
	public void reset(Minecraft minecraft) {
		target = null;
	}

	@Override
	public void tick(Minecraft minecraft) {
		if (target == null || minecraft.level == null) {
			return;
		}
		if (minecraft.level.getBlockState(target).isAir()) {
			stop(minecraft);
		} else {
			minecraft.gameMode.continueDestroyBlock(target, face);
		}
	}
}
