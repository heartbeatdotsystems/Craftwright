package systems.heartbeat.craftwright.junit;

import java.util.List;
import java.util.stream.Collectors;
import systems.heartbeat.craftwright.Client;
import systems.heartbeat.craftwright.Text;

/** What every player saw last and what the server printed last. After a failed test that is usually the whole explanation. */
final class FailureReport {
	private static final int CHAT_LINES = 10;
	private static final int CONSOLE_LINES = 20;

	private FailureReport() {
	}

	static String of(Environment environment) {
		StringBuilder report = new StringBuilder("\n──── Craftwright: state after the failed test ────\n");
		environment.clients().forEach(client -> report.append(client.name()).append(":\n").append(describe(client)));
		report.append("server console:\n").append(indented(tail(environment.server().log(), CONSOLE_LINES), "  | "));
		report.append("files: ").append(environment.craftwright().directory()).append('\n');
		return report.toString();
	}

	private static String describe(Client client) {
		try {
			return "  screen: " + (client.screen().isEmpty() ? "(in game)" : client.screen()) + '\n'
				+ "  open menu: " + client.menus().current().map(menu -> menu.title().plain()).orElse("none") + '\n'
				+ "  last chat:\n" + indented(tail(client.chat().received().all().stream().map(Text::plain).toList(), CHAT_LINES), "    | ");
		} catch (RuntimeException e) {
			return "  client not reachable: " + e.getMessage() + '\n';
		}
	}

	private static List<String> tail(List<String> lines, int count) {
		return lines.subList(Math.max(0, lines.size() - count), lines.size());
	}

	private static String indented(List<String> lines, String prefix) {
		return lines.isEmpty() ? prefix + "(nothing)\n" : lines.stream().map(line -> prefix + line + '\n').collect(Collectors.joining());
	}
}
