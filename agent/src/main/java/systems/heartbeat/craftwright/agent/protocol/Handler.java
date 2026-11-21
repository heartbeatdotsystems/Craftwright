package systems.heartbeat.craftwright.agent.protocol;

import com.google.gson.JsonElement;
import net.minecraft.client.Minecraft;

/** Answers one protocol method right away. Runs on the client thread. */
@FunctionalInterface
public interface Handler {
	JsonElement handle(Minecraft minecraft, Params params);
}
