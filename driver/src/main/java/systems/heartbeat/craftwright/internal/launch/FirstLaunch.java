package systems.heartbeat.craftwright.internal.launch;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * The first launch of a Minecraft version downloads and installs the game into the shared cache.
 * Two clients doing that at once would overwrite each other's files, so the first one goes alone.
 */
public final class FirstLaunch {
	private static final Object LOCK = new Object();
	private static final Set<String> INSTALLED = ConcurrentHashMap.newKeySet();

	private FirstLaunch() {
	}

	/** Runs the launch, alone if this version has not been started in this JVM yet. */
	public static <T> T guarded(String minecraftVersion, Supplier<T> launch) {
		if (INSTALLED.contains(minecraftVersion)) {
			return launch.get();
		}
		synchronized (LOCK) {
			T result = launch.get();
			INSTALLED.add(minecraftVersion);
			return result;
		}
	}
}
