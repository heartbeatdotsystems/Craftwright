package systems.heartbeat.craftwright.junit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Starts one real Paper server for the test class and hands out real clients as test parameters.
 *
 * <pre>{@code
 * @CraftwrightTest(plugins = "build/libs/MyPlugin-*.jar")
 * class WelcomeTest {
 *     @Test
 *     void greetsNewPlayers(Server server, @Player("Alex") Client alex) {
 *         alex.chat().await("Welcome, Alex");
 *     }
 * }
 * }</pre>
 *
 * <p>The server and the clients are shared by all tests of the class and stopped after the last one.
 * For anything the attributes do not cover, add a static method annotated with {@link ConfigureServer},
 * or name a {@link ServerSetup} to share one server between several test classes.</p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(CraftwrightExtension.class)
@Tag("craftwright")
public @interface CraftwrightTest {

	/** Minecraft version of the Paper server and the clients. */
	String minecraft() default "26.3";

	/** Plugin jars to install. A {@code *} in the file name picks the newest match, e.g. {@code build/libs/MyPlugin-*.jar}. */
	String[] plugins() default {};

	/** An empty flat world: quick to start and the same on every run. Switch off to test in a normal world. */
	boolean flatWorld() default true;

	/** How long {@code await…} calls wait before the test fails. */
	int timeoutSeconds() default 15;

	/**
	 * A named setup. Test classes with the same setup and the same settings share one server for the whole run.
	 * Without it every test class starts its own.
	 */
	Class<? extends ServerSetup> setup() default ServerSetup.PerClass.class;
}
