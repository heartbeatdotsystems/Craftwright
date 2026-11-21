package systems.heartbeat.craftwright.junit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@code Client} test parameter and names its player.
 * The client is started once per test class and reused by every test that asks for the same name.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Player {

	/** The player name, 1 to 16 letters, digits or underscores. */
	String value();

	/** Join the server before the test starts, unless the player is already on it. */
	boolean join() default true;

	/** Start the client with rendering, needed for screenshots. */
	boolean rendering() default false;
}
