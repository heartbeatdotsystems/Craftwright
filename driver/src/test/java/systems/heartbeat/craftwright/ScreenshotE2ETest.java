package systems.heartbeat.craftwright;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** A client with rendering draws real frames, and a test can save them. */
@Tag("e2e")
class ScreenshotE2ETest {
	@Test
	void renderingClientTakesAScreenshot() throws IOException {
		try (Craftwright craftwright = Craftwright.create().defaultTimeout(Duration.ofSeconds(60))) {
			Server server = craftwright.paper("26.3").flatWorld().start();
			Client camera = craftwright.client("Camera").rendering(true).start();
			camera.join(server);
			server.console("time set noon");
			Position position = camera.player().state().position();
			int x = position.blockX();
			int y = position.blockY() + 1;
			int z = position.blockZ() + 3;
			server.console("setblock " + x + " " + y + " " + z + " minecraft:diamond_block");
			camera.player().awaitState("to see the diamond block", state -> camera.world().block(x, y, z).is("diamond_block"));
			camera.player().lookAt(x + 0.5, y + 0.5, z + 0.5);

			Path file = camera.screenshot(craftwright.directory().resolve("screenshots").resolve("camera.png"));

			BufferedImage image = ImageIO.read(file.toFile());
			assertTrue(image.getWidth() >= 320 && image.getHeight() >= 240, "a full frame: " + image.getWidth() + "x" + image.getHeight());
			Set<Integer> colors = new HashSet<>();
			for (int px = 0; px < image.getWidth(); px += 16) {
				for (int py = 0; py < image.getHeight(); py += 16) {
					colors.add(image.getRGB(px, py));
				}
			}
			assertTrue(colors.size() > 20, "the screenshot is a real frame, not one flat colour (" + colors.size() + " colours)");
		}
	}
}
