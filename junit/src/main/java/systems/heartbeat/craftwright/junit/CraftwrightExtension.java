package systems.heartbeat.craftwright.junit;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import systems.heartbeat.craftwright.Client;
import systems.heartbeat.craftwright.ClientBuilder;
import systems.heartbeat.craftwright.Craftwright;
import systems.heartbeat.craftwright.PaperServerBuilder;
import systems.heartbeat.craftwright.Server;
import systems.heartbeat.craftwright.Text;

/** The JUnit 5 side of {@link CraftwrightTest}. */
public final class CraftwrightExtension implements BeforeAllCallback, ParameterResolver, AfterTestExecutionCallback {
	private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(CraftwrightExtension.class);

	@Override
	public void beforeAll(ExtensionContext context) {
		Class<?> testClass = context.getRequiredTestClass();
		CraftwrightTest settings = testClass.getAnnotation(CraftwrightTest.class);
		if (settings == null) {
			throw new IllegalStateException(testClass.getName() + " uses the Craftwright extension without @CraftwrightTest");
		}
		Environment environment = new Environment(Craftwright.create().defaultTimeout(Duration.ofSeconds(settings.timeoutSeconds())));
		context.getStore(NAMESPACE).put(Environment.class, environment);
		try {
			PaperServerBuilder paper = environment.craftwright.paper(settings.minecraft());
			if (settings.flatWorld()) {
				paper.flatWorld();
			}
			for (String plugin : settings.plugins()) {
				paper.plugin(plugin);
			}
			for (Method method : testClass.getDeclaredMethods()) {
				if (method.isAnnotationPresent(ConfigureServer.class)) {
					configure(method, paper);
				}
			}
			environment.server = paper.start();
		} catch (RuntimeException e) {
			environment.close();
			throw e;
		}
	}

	private static void configure(Method method, PaperServerBuilder paper) {
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

	@Override
	public boolean supportsParameter(ParameterContext parameter, ExtensionContext context) {
		Class<?> type = parameter.getParameter().getType();
		return type == Server.class || type == Craftwright.class || type == Client.class;
	}

	@Override
	public Object resolveParameter(ParameterContext parameter, ExtensionContext context) {
		Environment environment = context.getStore(NAMESPACE).get(Environment.class, Environment.class);
		if (environment == null) {
			throw new ParameterResolutionException("No Craftwright server is running. Is the class annotated with @CraftwrightTest?");
		}
		Class<?> type = parameter.getParameter().getType();
		if (type == Server.class) {
			return environment.server;
		}
		if (type == Craftwright.class) {
			return environment.craftwright;
		}
		Player player = parameter.findAnnotation(Player.class).orElseThrow(() -> new ParameterResolutionException(
			"A Client parameter needs a player name: @Player(\"Alex\") Client alex"));
		Client client = environment.clients.computeIfAbsent(player.value(), name -> {
			ClientBuilder builder = environment.craftwright.client(name).rendering(player.rendering());
			return builder.start();
		});
		if (player.join() && !client.isInWorld()) {
			client.join(environment.server);
		}
		return client;
	}

	/** On a failed test, prints what every player saw last. That is usually the whole explanation. */
	@Override
	public void afterTestExecution(ExtensionContext context) {
		if (context.getExecutionException().isEmpty()) {
			return;
		}
		Environment environment = context.getStore(NAMESPACE).get(Environment.class, Environment.class);
		if (environment == null) {
			return;
		}
		StringBuilder report = new StringBuilder("\n──── Craftwright: state after the failed test ────\n");
		environment.clients.forEach((name, client) -> {
			report.append(name).append(":\n");
			try {
				report.append("  screen: ").append(client.screen().isEmpty() ? "(in game)" : client.screen()).append('\n');
				report.append("  open menu: ").append(client.menu().map(menu -> menu.title().plain()).orElse("none")).append('\n');
				report.append("  last chat:\n").append(lastLines(client.chat().messages()));
			} catch (RuntimeException e) {
				report.append("  client not reachable: ").append(e.getMessage()).append('\n');
			}
		});
		if (environment.server != null) {
			List<String> log = environment.server.log();
			report.append("server console:\n");
			log.subList(Math.max(0, log.size() - 20), log.size()).forEach(line -> report.append("  | ").append(line).append('\n'));
			report.append("files: ").append(environment.craftwright.directory()).append('\n');
		}
		System.err.println(report);
	}

	private static String lastLines(List<Text> messages) {
		if (messages.isEmpty()) {
			return "    (nothing)\n";
		}
		return messages.subList(Math.max(0, messages.size() - 10), messages.size()).stream()
			.map(text -> "    | " + text.plain()).collect(Collectors.joining("\n")) + "\n";
	}

	/** Everything one test class started. JUnit closes it after the last test. */
	private static final class Environment implements ExtensionContext.Store.CloseableResource {
		private final Craftwright craftwright;
		private final Map<String, Client> clients = new ConcurrentHashMap<>();
		private Server server;

		private Environment(Craftwright craftwright) {
			this.craftwright = craftwright;
		}

		@Override
		public void close() {
			craftwright.close();
		}
	}
}
