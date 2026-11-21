package systems.heartbeat.craftwright.junit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a static method that finishes the server setup before it starts.
 *
 * <pre>{@code
 * @ConfigureServer
 * static void server(PaperServerBuilder paper) {
 *     paper.file("plugins/MyPlugin/config.yml", "welcome: true\n").property("difficulty", "peaceful");
 * }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigureServer {
}
