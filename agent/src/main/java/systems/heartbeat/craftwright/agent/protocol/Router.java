package systems.heartbeat.craftwright.agent.protocol;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;

/** Finds the handler for a request and turns its outcome into the response message. */
public final class Router implements Routes {
	private final Map<String, DeferredHandler> handlers = new HashMap<>();

	@Override
	public void on(String method, Handler handler) {
		onDeferred(method, (minecraft, params) -> CompletableFuture.completedFuture(handler.handle(minecraft, params)));
	}

	@Override
	public void onDeferred(String method, DeferredHandler handler) {
		if (handlers.putIfAbsent(method, handler) != null) {
			throw new IllegalStateException("Two features answer '" + method + "'");
		}
	}

	/** Runs the request on the calling (client) thread and hands the response to {@code reply}, now or later. */
	public void dispatch(Minecraft minecraft, JsonObject request, Consumer<JsonObject> reply) {
		JsonElement id = request.get("id");
		String method = request.get("method").getAsString();
		JsonObject values = request.has("params") && request.get("params").isJsonObject()
			? request.getAsJsonObject("params") : new JsonObject();
		DeferredHandler handler = handlers.get(method);
		if (handler == null) {
			reply.accept(Messages.error(id, "Unknown method '" + method + "'. This agent speaks protocol " + Messages.PROTOCOL_VERSION));
			return;
		}
		try {
			handler.handle(minecraft, new Params(method, values)).whenComplete((result, failure) ->
				reply.accept(failure == null ? Messages.result(id, result) : Messages.error(id, describe(method, failure))));
		} catch (RuntimeException failure) {
			reply.accept(Messages.error(id, describe(method, failure)));
		}
	}

	private static String describe(String method, Throwable failure) {
		Throwable cause = failure instanceof java.util.concurrent.CompletionException && failure.getCause() != null
			? failure.getCause() : failure;
		if (cause instanceof RequestException) {
			return cause.getMessage();
		}
		cause.printStackTrace();
		return method + " crashed in the client: " + cause;
	}
}
