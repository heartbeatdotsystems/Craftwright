package systems.heartbeat.craftwright.internal;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;
import systems.heartbeat.craftwright.AwaitTimeoutException;

/** Polls a condition until it holds or the timeout passes. */
public final class Await {
	private static final long POLL_MILLIS = 50;

	private Await() {
	}

	/**
	 * @param what    what is being waited for, used in the timeout message
	 * @param context extra state for the timeout message, evaluated only on timeout
	 */
	public static <T> T until(String what, Duration timeout, Supplier<Optional<T>> probe, Supplier<String> context) {
		long deadline = System.nanoTime() + timeout.toNanos();
		while (true) {
			Optional<T> value = probe.get();
			if (value.isPresent()) {
				return value.get();
			}
			if (System.nanoTime() >= deadline) {
				String extra = context == null ? "" : context.get();
				throw new AwaitTimeoutException("Timed out after " + timeout.toMillis() + " ms waiting for " + what
					+ (extra == null || extra.isEmpty() ? "" : "\n" + extra));
			}
			try {
				Thread.sleep(POLL_MILLIS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new AwaitTimeoutException("Interrupted while waiting for " + what);
			}
		}
	}

	public static void untilTrue(String what, Duration timeout, Supplier<Boolean> probe, Supplier<String> context) {
		until(what, timeout, () -> probe.get() ? Optional.of(Boolean.TRUE) : Optional.empty(), context);
	}
}
