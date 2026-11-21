package systems.heartbeat.craftwright.junit;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import systems.heartbeat.craftwright.Client;
import systems.heartbeat.craftwright.Craftwright;
import systems.heartbeat.craftwright.PaperServerBuilder;
import systems.heartbeat.craftwright.Server;

/** One running server with its clients. JUnit closes it when the tests that use it are over. */
final class Environment implements AutoCloseable {
	private final Craftwright craftwright;
	private final Server server;
	private final Map<String, Client> clients = new ConcurrentHashMap<>();

	private Environment(Craftwright craftwright, Server server) {
		this.craftwright = craftwright;
		this.server = server;
	}

	/** Starts the server the annotation describes. Nothing stays running if that fails. */
	static Environment start(CraftwrightTest settings, Class<?> testClass) {
		Craftwright craftwright = Craftwright.create().defaultTimeout(Duration.ofSeconds(settings.timeoutSeconds()));
		try {
			PaperServerBuilder paper = craftwright.paper(settings.minecraft());
			if (settings.flatWorld()) {
				paper.flatWorld();
			}
			for (String plugin : settings.plugins()) {
				paper.plugin(plugin);
			}
			if (isShared(settings)) {
				instantiate(settings.setup()).configure(paper);
			} else {
				configureFromTestClass(testClass, paper);
			}
			return new Environment(craftwright, paper.start());
		} catch (RuntimeException e) {
			craftwright.close();
			throw e;
		}
	}

	static boolean isShared(CraftwrightTest settings) {
		return settings.setup() != ServerSetup.PerClass.class;
	}

	private static ServerSetup instantiate(Class<? extends ServerSetup> setup) {
		try {
			var constructor = setup.getDeclaredConstructor();
			constructor.setAccessible(true);
			return constructor.newInstance();
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(setup.getName() + " needs a constructor without parameters", e);
		}
	}

	private static void configureFromTestClass(Class<?> testClass, PaperServerBuilder paper) {
		for (Method method : testClass.getDeclaredMethods()) {
			if (!method.isAnnotationPresent(ConfigureServer.class)) {
				continue;
			}
			if (!Modifier.isStatic(method.getModifiers()) || method.getParameterCount() != 1
					|| method.getParameterTypes()[0] != PaperServerBuilder.class) {
				throw new IllegalStateException("@ConfigureServer needs the form: static void " + method.getName() + "(PaperServerBuilder paper)");
			}
			try {
				method.setAccessible(true);
				method.invoke(null, paper);
			} catch (ReflectiveOperationException e) {
				throw new IllegalStateException("@ConfigureServer method " + method.getName() + " failed", e.getCause() != null ? e.getCause() : e);
			}
		}
	}

	Craftwright craftwright() {
		return craftwright;
	}

	Server server() {
		return server;
	}

	Collection<Client> clients() {
		return clients.values();
	}

	/** The client of this player, started on first use and on the server if the parameter asks for it. */
	Client client(Player player) {
		Client client = clients.computeIfAbsent(player.value(),
			name -> craftwright.client(name).rendering(player.rendering()).start());
		if (player.join() && !client.isInWorld()) {
			client.join(server);
		}
		return client;
	}

	@Override
	public void close() {
		craftwright.close();
	}
}
