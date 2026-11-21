package systems.heartbeat.craftwright;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import systems.heartbeat.craftwright.internal.Await;

/**
 * A stream of texts the client received: chat, titles, subtitles or action bar texts.
 *
 * <p>Every text can satisfy one {@code await} only. Two awaits for the same words
 * therefore need two messages, and a message that arrived before the await was
 * called still counts.</p>
 */
public final class Messages {
	private final String what;
	private final Client client;
	private final List<Text> messages = new ArrayList<>();
	private final List<Boolean> consumed = new ArrayList<>();

	Messages(String what, Client client) {
		this.what = what;
		this.client = client;
	}

	synchronized void add(Text text) {
		messages.add(text);
		consumed.add(Boolean.FALSE);
	}

	/** Everything received so far, oldest first. */
	public synchronized List<Text> messages() {
		return List.copyOf(messages);
	}

	/** The most recent text, if any. */
	public synchronized Optional<Text> last() {
		return messages.isEmpty() ? Optional.empty() : Optional.of(messages.get(messages.size() - 1));
	}

	/** Forgets everything received so far. */
	public synchronized void clear() {
		messages.clear();
		consumed.clear();
	}

	/** Waits for a text that contains the given words. */
	public Text await(String contains) {
		return await("containing \"" + contains + "\"", text -> text.contains(contains), client.timeout());
	}

	public Text await(String contains, Duration timeout) {
		return await("containing \"" + contains + "\"", text -> text.contains(contains), timeout);
	}

	public Text await(Pattern pattern) {
		return await("matching /" + pattern + "/", text -> text.matches(pattern), client.timeout());
	}

	public Text await(Predicate<Text> condition) {
		return await("matching the condition", condition, client.timeout());
	}

	public Text await(Predicate<Text> condition, Duration timeout) {
		return await("matching the condition", condition, timeout);
	}

	private Text await(String description, Predicate<Text> condition, Duration timeout) {
		return Await.until(what + " " + description + " for " + client.name(), timeout, () -> take(condition), this::recent);
	}

	/** True if a text with these words was received, whether or not an await already used it. */
	public synchronized boolean contains(String part) {
		return messages.stream().anyMatch(text -> text.contains(part));
	}

	/**
	 * Watches for the given time and fails if a matching text shows up.
	 * Texts that were already there count too, unless an await used them.
	 */
	public void expectNone(String contains, Duration watchFor) {
		long deadline = System.nanoTime() + watchFor.toNanos();
		do {
			Optional<Text> hit = peek(text -> text.contains(contains));
			if (hit.isPresent()) {
				throw new AssertionError(client.name() + " received " + what + " that should not appear: \"" + hit.get().plain() + "\"");
			}
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
		} while (System.nanoTime() < deadline);
	}

	private synchronized Optional<Text> take(Predicate<Text> condition) {
		for (int i = 0; i < messages.size(); i++) {
			if (!consumed.get(i) && condition.test(messages.get(i))) {
				consumed.set(i, Boolean.TRUE);
				return Optional.of(messages.get(i));
			}
		}
		return Optional.empty();
	}

	private synchronized Optional<Text> peek(Predicate<Text> condition) {
		for (int i = 0; i < messages.size(); i++) {
			if (!consumed.get(i) && condition.test(messages.get(i))) {
				return Optional.of(messages.get(i));
			}
		}
		return Optional.empty();
	}

	private synchronized String recent() {
		if (messages.isEmpty()) {
			return "No " + what + " received so far.";
		}
		int from = Math.max(0, messages.size() - 15);
		return "Last " + what + " received:\n" + messages.subList(from, messages.size()).stream()
			.map(text -> "  | " + text.plain()).collect(Collectors.joining("\n"));
	}
}
