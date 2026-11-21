package systems.heartbeat.craftwright.agent.feature;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.suggestion.Suggestion;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import systems.heartbeat.craftwright.agent.game.Game;
import systems.heartbeat.craftwright.agent.observe.ChatLog;
import systems.heartbeat.craftwright.agent.protocol.Params;
import systems.heartbeat.craftwright.agent.protocol.RequestException;
import systems.heartbeat.craftwright.agent.protocol.Routes;
import systems.heartbeat.craftwright.agent.wire.Codecs;

/** The chat line: sending messages and commands, clicking on what the server wrote, tab completion. */
public final class ChatFeature implements Feature {
	private final ChatLog chatLog;

	public ChatFeature(ChatLog chatLog) {
		this.chatLog = chatLog;
	}

	@Override
	public void register(Routes routes) {
		routes.on("chat.send", this::send);
		routes.on("chat.command", this::command);
		routes.on("chat.click", this::click);
		routes.onDeferred("chat.complete", this::complete);
	}

	private JsonElement send(Minecraft minecraft, Params params) {
		Game.connection(minecraft).sendChat(params.string("message"));
		return Feature.done();
	}

	private JsonElement command(Minecraft minecraft, Params params) {
		Game.connection(minecraft).sendCommand(withoutSlash(params.string("command")));
		return Feature.done();
	}

	private JsonElement click(Minecraft minecraft, Params params) {
		String contains = params.string("contains");
		for (Component message : chatLog.newestFirst()) {
			if (!message.getString().contains(contains)) {
				continue;
			}
			ClickEvent click = clickableIn(message, contains);
			if (click == null) {
				throw new RequestException("The chat message \"" + message.getString() + "\" has nothing to click");
			}
			return perform(minecraft, click);
		}
		throw new RequestException("No chat message contains \"" + contains + "\"");
	}

	/** Prefers the clickable part that itself carries the words, otherwise the first clickable part. */
	private static ClickEvent clickableIn(Component message, String contains) {
		ClickEvent first = null;
		for (Component part : message.toFlatList()) {
			ClickEvent click = part.getStyle().getClickEvent();
			if (click == null) {
				continue;
			}
			if (part.getString().contains(contains)) {
				return click;
			}
			if (first == null) {
				first = click;
			}
		}
		return first;
	}

	/** Runs a command click like the game does. Every click is reported in the form chat components use. */
	private static JsonElement perform(Minecraft minecraft, ClickEvent click) {
		if (click instanceof ClickEvent.RunCommand run) {
			Game.connection(minecraft).sendCommand(withoutSlash(run.command()));
		}
		JsonObject result = new JsonObject();
		result.add("click", Codecs.json(ClickEvent.CODEC, click));
		return result;
	}

	/** Asks for completions the way the chat box does, including the ones only the server knows. */
	private CompletableFuture<JsonElement> complete(Minecraft minecraft, Params params) {
		ClientPacketListener connection = Game.connection(minecraft);
		String input = withoutSlash(params.string("input"));
		CommandDispatcher<ClientSuggestionProvider> commands = connection.getCommands();
		return commands.getCompletionSuggestions(commands.parse(input, connection.getSuggestionsProvider())).thenApply(suggestions -> {
			JsonArray texts = new JsonArray();
			for (Suggestion suggestion : suggestions.getList()) {
				texts.add(suggestion.getText());
			}
			JsonObject result = new JsonObject();
			result.add("suggestions", texts);
			return result;
		});
	}

	private static String withoutSlash(String command) {
		return command.startsWith("/") ? command.substring(1) : command;
	}
}
