package systems.heartbeat.craftwright.agent.feature;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import systems.heartbeat.craftwright.agent.game.Game;
import systems.heartbeat.craftwright.agent.protocol.Params;
import systems.heartbeat.craftwright.agent.protocol.RequestException;
import systems.heartbeat.craftwright.agent.protocol.Routes;
import systems.heartbeat.craftwright.agent.wire.TextWire;

/** The client as a program: which screen it shows, joining and leaving servers, screenshots, quitting. */
public final class ClientFeature implements Feature {
	private final Runnable onLeave;

	/** @param onLeave runs before the client leaves a world, to drop whatever was in progress there */
	public ClientFeature(Runnable onLeave) {
		this.onLeave = onLeave;
	}

	@Override
	public void register(Routes routes) {
		routes.on("client.state", (minecraft, params) -> state(minecraft));
		routes.on("client.connect", this::connect);
		routes.on("client.disconnect", (minecraft, params) -> disconnect(minecraft));
		routes.on("client.quit", (minecraft, params) -> quit(minecraft));
		routes.onDeferred("client.screenshot", this::screenshot);
	}

	private JsonElement state(Minecraft minecraft) {
		Screen screen = minecraft.gui.screen();
		JsonObject state = new JsonObject();
		state.addProperty("screen", screen == null ? "" : screen.getClass().getSimpleName());
		state.add("screenTitle", screen == null ? JsonNull.INSTANCE : TextWire.of(screen.getTitle()));
		state.addProperty("inWorld", Game.inWorld(minecraft));
		return state;
	}

	private JsonElement connect(Minecraft minecraft, Params params) {
		if (minecraft.level != null) {
			throw new RequestException("The client is already on a server. Leave first.");
		}
		String host = params.string("host");
		int port = params.integer("port");
		ServerData server = new ServerData("Craftwright", host + ":" + port, ServerData.Type.OTHER);
		ConnectScreen.startConnecting(new TitleScreen(), minecraft, new ServerAddress(host, port), server, false, null);
		return Feature.done();
	}

	private JsonElement disconnect(Minecraft minecraft) {
		if (minecraft.level != null) {
			onLeave.run();
			minecraft.disconnectFromWorld(Component.translatable("multiplayer.status.quitting"));
		}
		if (minecraft.level == null && !(minecraft.gui.screen() instanceof TitleScreen)) {
			minecraft.gui.setScreen(new TitleScreen());
		}
		return Feature.done();
	}

	private JsonElement quit(Minecraft minecraft) {
		minecraft.stop();
		return Feature.done();
	}

	/** Answers once the frame the game rendered last is on disk as a PNG. */
	private CompletableFuture<JsonElement> screenshot(Minecraft minecraft, Params params) {
		Path file = Path.of(params.string("file"));
		CompletableFuture<JsonElement> written = new CompletableFuture<>();
		Screenshot.takeScreenshot(minecraft.gameRenderer.mainRenderTarget(), image -> {
			try (image) {
				if (file.getParent() != null) {
					Files.createDirectories(file.getParent());
				}
				Path partial = file.resolveSibling(file.getFileName() + ".part");
				image.writeToFile(partial);
				Files.move(partial, file, StandardCopyOption.REPLACE_EXISTING);
				written.complete(Feature.done());
			} catch (IOException | RuntimeException e) {
				written.completeExceptionally(new RequestException("Could not write the screenshot " + file + ": " + e));
			}
		});
		return written;
	}
}
