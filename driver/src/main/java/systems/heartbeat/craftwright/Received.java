package systems.heartbeat.craftwright;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import systems.heartbeat.craftwright.internal.Await;

/**
 * Everything of one kind a client received so far: chat lines, titles, sounds, particles.
 *
 * <p>Every entry can satisfy one {@code await} only. Two awaits for the same words
 * therefore need two entries, and an entry that arrived before the await was called still counts.</p>
 *
 * @param <T> what was received
 */
public final class Received<T> {
	private static final int SHOWN_ON_FAILURE = 15;

	private final String what;
	private final Remote remote;
	private final Function<T, String> readable;
	private final List<T> entries = new ArrayList<>();
	private final List<Boolean> used = new ArrayList<>();

	/**
	 * @param what     the kind in a sentence, e.g. "chat message"
	 * @param readable the text of an entry that {@code await("words")} looks at and failures print
	 */
	Received(String what, Remote remote, Function<T, String> readable) {
		this.what = what;
		this.remote = remote;
		this.readable = readable;
	}

	synchronized void add(T entry) {
		entries.add(entry);
		used.add(Boolean.FALSE);
	}

	/** Everything received so far, oldest first. */
	public synchronized List<T> all() {
		return List.copyOf(entries);
	}

	/** The most recent entry, if any. */
	public synchronized Optional<T> last() {
		return entries.isEmpty() ? Optional.empty() : Optional.of(entries.get(entries.size() - 1));
	}

	/** Forgets everything received so far. */
	public synchronized void clear() {
		entries.clear();
		used.clear();
	}

	/** True if an entry with these words was received, whether or not an await already used it. */
	public synchronized boolean contains(String words) {
		return entries.stream().anyMatch(entry -> readable.apply(entry).contains(words));
	}

	/** Waits for an entry that contains the given words. */
	public T await(String words) {
		return await(words, remote.timeout());
	}

	public T await(String words, Duration timeout) {
		return await("containing \"" + words + "\"", entry -> readable.apply(entry).contains(words), timeout);
	}

	public T await(Pattern pattern) {
		return await("matching /" + pattern + "/", entry -> pattern.matcher(readable.apply(entry)).find(), remote.timeout());
	}

	public T await(Predicate<T> condition) {
		return await(condition, remote.timeout());
	}

	public T await(Predicate<T> condition, Duration timeout) {
		return await("matching the condition", condition, timeout);
	}

	private T await(String description, Predicate<T> condition, Duration timeout) {
		return remote.await(what + " " + description, timeout, () -> find(condition, true), this::recent);
	}

	/**
	 * Watches for the given time and fails if an entry with these words shows up.
	 * Entries that were already there count too, unless an await used them.
	 */
	public void expectNone(String words, Duration watchFor) {
		expectNone(entry -> readable.apply(entry).contains(words), watchFor);
	}

	public void expectNone(Predicate<T> condition, Duration watchFor) {
		Await.during(watchFor, () -> find(condition, false).ifPresent(hit -> {
			throw new AssertionError(remote.name() + " received a " + what + " that should not appear: \"" + readable.apply(hit) + "\"");
		}));
	}

	private synchronized Optional<T> find(Predicate<T> condition, boolean take) {
		for (int i = 0; i < entries.size(); i++) {
			if (!used.get(i) && condition.test(entries.get(i))) {
				if (take) {
					used.set(i, Boolean.TRUE);
				}
				return Optional.of(entries.get(i));
			}
		}
		return Optional.empty();
	}

	private synchronized String recent() {
		if (entries.isEmpty()) {
			return "No " + what + " received so far.";
		}
		int from = Math.max(0, entries.size() - SHOWN_ON_FAILURE);
		return "Last " + what + "s received:\n" + entries.subList(from, entries.size()).stream()
			.map(entry -> "  | " + readable.apply(entry)).collect(Collectors.joining("\n"));
	}
}
