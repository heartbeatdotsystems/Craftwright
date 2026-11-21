package systems.heartbeat.craftwright.internal.server;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import systems.heartbeat.craftwright.internal.Await;
import systems.heartbeat.craftwright.internal.ManagedProcess;

/**
 * Reading the console of a running server. Every line can satisfy one await only,
 * so two awaits for the same words need two lines.
 */
public final class ConsoleLog {
	private static final int LINES_ON_FAILURE = 25;

	private final ManagedProcess process;
	private final Set<Integer> usedLines = new HashSet<>();

	public ConsoleLog(ManagedProcess process) {
		this.process = process;
	}

	public List<String> lines() {
		return process.lines();
	}

	public boolean contains(String words) {
		return process.lines().stream().anyMatch(line -> line.contains(words));
	}

	public String await(String description, Predicate<String> condition, Duration timeout) {
		return Await.until(description, timeout, () -> take(condition), () -> "Last output:\n" + process.tail(LINES_ON_FAILURE));
	}

	private synchronized Optional<String> take(Predicate<String> condition) {
		int count = process.lineCount();
		for (int index = 0; index < count; index++) {
			if (usedLines.contains(index)) {
				continue;
			}
			String line = process.line(index);
			if (condition.test(line)) {
				usedLines.add(index);
				return Optional.of(line);
			}
		}
		return Optional.empty();
	}
}
