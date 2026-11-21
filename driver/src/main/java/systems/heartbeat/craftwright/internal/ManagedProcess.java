package systems.heartbeat.craftwright.internal;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import systems.heartbeat.craftwright.CraftwrightException;

/** A child process whose output is kept line by line and mirrored to a file. */
public final class ManagedProcess {
	private final String label;
	private final Process process;
	private final BufferedWriter stdin;
	private final List<String> lines = new ArrayList<>();

	public ManagedProcess(String label, List<String> command, Path workingDirectory, Map<String, String> environment, Path logFile) {
		this.label = label;
		try {
			Files.createDirectories(workingDirectory);
			ProcessBuilder builder = new ProcessBuilder(command).directory(workingDirectory.toFile()).redirectErrorStream(true);
			builder.environment().putAll(environment);
			this.process = builder.start();
			this.stdin = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
			Thread reader = new Thread(() -> pump(logFile), "craftwright-output-" + label);
			reader.setDaemon(true);
			reader.start();
		} catch (IOException e) {
			throw new CraftwrightException("Cannot start " + label + ": " + String.join(" ", command), e);
		}
	}

	private void pump(Path logFile) {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
				BufferedWriter file = Files.newBufferedWriter(logFile, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
			String line;
			while ((line = reader.readLine()) != null) {
				synchronized (lines) {
					lines.add(line);
				}
				file.write(line);
				file.newLine();
				file.flush();
			}
		} catch (IOException ignored) {
			// the process ended
		}
	}

	public void writeLine(String line) {
		try {
			synchronized (stdin) {
				stdin.write(line);
				stdin.write('\n');
				stdin.flush();
			}
		} catch (IOException e) {
			throw new CraftwrightException("Cannot write to " + label + ", it is no longer running", e);
		}
	}

	public List<String> lines() {
		synchronized (lines) {
			return List.copyOf(lines);
		}
	}

	public int lineCount() {
		synchronized (lines) {
			return lines.size();
		}
	}

	public String line(int index) {
		synchronized (lines) {
			return lines.get(index);
		}
	}

	/** The last lines of output, for error messages. */
	public String tail(int count) {
		synchronized (lines) {
			int from = Math.max(0, lines.size() - count);
			return String.join("\n", lines.subList(from, lines.size()));
		}
	}

	public boolean isAlive() {
		return process.isAlive();
	}

	public boolean waitFor(long seconds) {
		try {
			return process.waitFor(seconds, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}
	}

	/** Kills the process and everything it started. */
	public void destroy() {
		process.descendants().forEach(ProcessHandle::destroyForcibly);
		process.destroyForcibly();
		waitFor(10);
	}
}
