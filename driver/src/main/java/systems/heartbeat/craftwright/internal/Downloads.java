package systems.heartbeat.craftwright.internal;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import systems.heartbeat.craftwright.CraftwrightException;

/** Fetches files once and keeps them in the cache directory. */
public final class Downloads {
	private static final String USER_AGENT = "Craftwright/0.1 (+https://github.com/heartbeatdotsystems/Craftwright)";
	private static final HttpClient HTTP = HttpClient.newBuilder()
		.followRedirects(HttpClient.Redirect.NORMAL)
		.connectTimeout(Duration.ofSeconds(20))
		.build();

	private Downloads() {
	}

	public static String text(String url) {
		try {
			HttpResponse<String> response = HTTP.send(request(url), HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() != 200) {
				throw new CraftwrightException("GET " + url + " answered " + response.statusCode());
			}
			return response.body();
		} catch (IOException e) {
			throw new CraftwrightException("GET " + url + " failed", e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new CraftwrightException("Interrupted during GET " + url);
		}
	}

	/** Downloads to {@code target} unless it is already there. Safe when several JVMs do it at once. */
	public static Path file(String url, Path target) {
		if (Files.isRegularFile(target)) {
			return target;
		}
		try {
			Files.createDirectories(target.getParent());
			Path partial = Files.createTempFile(target.getParent(), target.getFileName().toString(), ".part");
			try {
				HttpResponse<Path> response = HTTP.send(request(url), HttpResponse.BodyHandlers.ofFile(partial));
				if (response.statusCode() != 200) {
					throw new CraftwrightException("GET " + url + " answered " + response.statusCode());
				}
				Files.move(partial, target, StandardCopyOption.REPLACE_EXISTING);
			} finally {
				Files.deleteIfExists(partial);
			}
			return target;
		} catch (IOException e) {
			throw new CraftwrightException("Download of " + url + " failed", e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new CraftwrightException("Interrupted while downloading " + url);
		}
	}

	private static HttpRequest request(String url) {
		return HttpRequest.newBuilder(URI.create(url)).header("User-Agent", USER_AGENT).timeout(Duration.ofMinutes(5)).GET().build();
	}
}
