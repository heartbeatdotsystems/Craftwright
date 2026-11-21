package systems.heartbeat.craftwright.agent.protocol;

import com.google.gson.JsonElement;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;

/** Answers one protocol method once the game has the result, for example after the server replied. */
@FunctionalInterface
public interface DeferredHandler {
	CompletableFuture<JsonElement> handle(Minecraft minecraft, Params params);
}
