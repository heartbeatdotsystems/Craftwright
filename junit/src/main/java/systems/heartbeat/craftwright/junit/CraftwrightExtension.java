package systems.heartbeat.craftwright.junit;

import java.util.List;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import systems.heartbeat.craftwright.Client;
import systems.heartbeat.craftwright.Craftwright;
import systems.heartbeat.craftwright.Server;

/**
 * Connects JUnit to Craftwright: finds or starts the environment of a test class,
 * hands its server and clients to test methods, and prints a report when a test fails.
 */
public final class CraftwrightExtension implements BeforeAllCallback, ParameterResolver, AfterTestExecutionCallback {
	private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(CraftwrightExtension.class);
	/** Under this key the class-level store points to the environment of the class, shared or its own. */
	private static final String CURRENT = "environment";

	@Override
	public void beforeAll(ExtensionContext context) {
		Class<?> testClass = context.getRequiredTestClass();
		CraftwrightTest settings = testClass.getAnnotation(CraftwrightTest.class);
		if (settings == null) {
			throw new IllegalStateException(testClass.getName() + " uses the Craftwright extension without @CraftwrightTest");
		}
		context.getStore(NAMESPACE).put(CURRENT, environmentFor(settings, testClass, context));
	}

	/**
	 * A shared environment lives in the root store, so JUnit keeps it until the whole run is over.
	 * An environment of one class lives in the store of that class and ends with it.
	 */
	private static Reference environmentFor(CraftwrightTest settings, Class<?> testClass, ExtensionContext context) {
		if (!Environment.isShared(settings)) {
			Environment own = Environment.start(settings, testClass);
			context.getStore(NAMESPACE).put(Environment.class, own);
			return new Reference(own);
		}
		List<Object> key = List.of(settings.setup(), settings.minecraft(), List.of(settings.plugins()), settings.flatWorld(), settings.timeoutSeconds());
		return new Reference(context.getRoot().getStore(NAMESPACE)
			.getOrComputeIfAbsent(key, unused -> Environment.start(settings, testClass), Environment.class));
	}

	@Override
	public boolean supportsParameter(ParameterContext parameter, ExtensionContext context) {
		Class<?> type = parameter.getParameter().getType();
		return type == Server.class || type == Craftwright.class || type == Client.class;
	}

	@Override
	public Object resolveParameter(ParameterContext parameter, ExtensionContext context) {
		Environment environment = current(context);
		if (environment == null) {
			throw new ParameterResolutionException("No Craftwright server is running. Is the class annotated with @CraftwrightTest?");
		}
		Class<?> type = parameter.getParameter().getType();
		if (type == Server.class) {
			return environment.server();
		}
		if (type == Craftwright.class) {
			return environment.craftwright();
		}
		Player player = parameter.findAnnotation(Player.class).orElseThrow(() -> new ParameterResolutionException(
			"A Client parameter needs a player name: @Player(\"Alex\") Client alex"));
		return environment.client(player);
	}

	@Override
	public void afterTestExecution(ExtensionContext context) {
		Environment environment = current(context);
		if (context.getExecutionException().isPresent() && environment != null) {
			System.err.println(FailureReport.of(environment));
		}
	}

	private static Environment current(ExtensionContext context) {
		Reference reference = context.getStore(NAMESPACE).get(CURRENT, Reference.class);
		return reference == null ? null : reference.environment();
	}

	/**
	 * Points to an environment without owning it. JUnit closes every closeable value of a store,
	 * and a shared environment must outlive the classes that merely use it.
	 */
	private record Reference(Environment environment) {
	}
}
