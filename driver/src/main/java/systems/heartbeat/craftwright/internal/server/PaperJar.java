package systems.heartbeat.craftwright.internal.server;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import systems.heartbeat.craftwright.internal.Downloads;

/** The Paper server jar of a Minecraft version, downloaded once from PaperMC and kept in the cache. */
public final class PaperJar {
	private static final String BUILDS = "https://fill.papermc.io/v3/projects/paper/versions/%s/builds/latest";

	private PaperJar() {
	}

	public static Path forVersion(String version, Path cache) {
		Path jar = cache.resolve("paper").resolve("paper-" + version + ".jar");
		if (Files.isRegularFile(jar)) {
			return jar;
		}
		JsonObject build = JsonParser.parseString(Downloads.text(String.format(BUILDS, version))).getAsJsonObject();
		String url = build.getAsJsonObject("downloads").getAsJsonObject("server:default").get("url").getAsString();
		return Downloads.file(url, jar);
	}
}
