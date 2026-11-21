package systems.heartbeat.craftwright;

import com.google.gson.JsonElement;
import java.util.List;
import systems.heartbeat.craftwright.internal.Json;

/**
 * The chat line of a client: what it writes and what it reads.
 *
 * <pre>{@code
 * alex.chat().command("regeln");
 * alex.chat().received().await("Regeln auf Fernweh");
 * alex.chat().click("[Annehmen]");
 * }</pre>
 */
public final class Chat {
	private final Remote remote;
	private final Received<Text> received;

	Chat(Remote remote) {
		this.remote = remote;
		this.received = new Received<>("chat message", remote, Text::plain);
	}

	/** Sends a chat message. */
	public void say(String message) {
		remote.call("chat.send", Json.object("message", message));
	}

	/** Runs a command as the player, with or without the leading slash. */
	public void command(String command) {
		remote.call("chat.command", Json.object("command", command));
	}

	/** Every chat and system message this client was shown. */
	public Received<Text> received() {
		return received;
	}

	/** Short for {@code received().await(words)}. */
	public Text await(String words) {
		return received.await(words);
	}

	/**
	 * Clicks a clickable text in the newest message that contains the words, like a player
	 * clicking it in the chat window. A command behind the click is run.
	 */
	public Click click(String words) {
		return Click.of(remote.call("chat.click", Json.object("contains", words)).get("click"));
	}

	/**
	 * What pressing tab would offer for a command typed so far, e.g. {@code complete("/gamemode ")}.
	 * Empty when there is nothing to offer. A server that stays silent in that case (Paper does for
	 * plugin commands) is given a few seconds before the answer is taken as empty.
	 */
	public List<String> complete(String typedSoFar) {
		return Json.list(remote.call("chat.complete", Json.object("input", typedSoFar)), "suggestions", JsonElement::getAsString);
	}
}
