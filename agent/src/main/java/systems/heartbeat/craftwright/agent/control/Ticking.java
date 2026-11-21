package systems.heartbeat.craftwright.agent.control;

import net.minecraft.client.Minecraft;

/** Something that needs every client tick to finish what a single request started. */
public interface Ticking {
	void tick(Minecraft minecraft);

	/** Called when the client leaves the world: whatever was in progress is over. */
	void reset(Minecraft minecraft);
}
