package systems.heartbeat.craftwright.agent.observe;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import net.minecraft.network.chat.Component;

/** The last lines of the chat window with their full components, so a test can click on one. */
public final class ChatLog {
	private static final int CAPACITY = 200;
	private final Deque<Component> lines = new ArrayDeque<>();

	public synchronized void add(Component line) {
		lines.addLast(line);
		while (lines.size() > CAPACITY) {
			lines.removeFirst();
		}
	}

	/** Newest first. */
	public synchronized List<Component> newestFirst() {
		List<Component> copy = new ArrayList<>(lines);
		java.util.Collections.reverse(copy);
		return copy;
	}
}
