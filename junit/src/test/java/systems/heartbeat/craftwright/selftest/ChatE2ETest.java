package systems.heartbeat.craftwright.selftest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import systems.heartbeat.craftwright.Click;
import systems.heartbeat.craftwright.Client;
import systems.heartbeat.craftwright.Server;
import systems.heartbeat.craftwright.Text;
import systems.heartbeat.craftwright.junit.CraftwrightTest;
import systems.heartbeat.craftwright.junit.Player;

/** Writing, reading, clicking and completing in the chat line. */
@Tag("e2e")
@CraftwrightTest(setup = SelfTestServer.class)
class ChatE2ETest {

	@Test
	void messagesGoBothWays(Server server, @Player("Alex") Client alex) {
		alex.chat().say("hello from a real client");
		server.awaitLog("hello from a real client");
		server.console("say greetings from the console");
		alex.chat().await("greetings from the console");
	}

	@Test
	void commandsRunAsThePlayer(Server server, @Player("Alex") Client alex) {
		alex.chat().command("me waves");
		server.awaitLog("* Alex waves");
		alex.chat().command("/me waves again");
		server.awaitLog("* Alex waves again");
	}

	@Test
	void messagesKeepTheirFormatting(Server server, @Player("Alex") Client alex) {
		server.console("tellraw Alex {\"text\":\"Shiny words\",\"color\":\"gold\",\"bold\":true}");
		Text message = alex.chat().await("Shiny words");
		assertTrue(message.json().toString().contains("gold"), message.json().toString());
	}

	@Test
	void clickRunsTheCommandBehindTheText(Server server, @Player("Alex") Client alex) {
		server.console("tellraw Alex [\"Read the rules \",{\"text\":\"[Accept]\",\"click_event\":{\"action\":\"run_command\",\"command\":\"/me accepts the rules\"}}]");
		alex.chat().await("Read the rules");

		Click click = alex.chat().click("[Accept]");

		assertEquals("run_command", click.action());
		assertEquals(Optional.of("/me accepts the rules"), click.value());
		server.awaitLog("* Alex accepts the rules");
	}

	@Test
	void clickReportsALinkWithoutOpeningIt(Server server, @Player("Alex") Client alex) {
		server.console("tellraw Alex {\"text\":\"[Website]\",\"click_event\":{\"action\":\"open_url\",\"url\":\"https://example.org/rules\"}}");
		alex.chat().await("[Website]");

		Click click = alex.chat().click("[Website]");

		assertEquals("open_url", click.action());
		assertEquals(Optional.of("https://example.org/rules"), click.value());
	}

	@Test
	void tabCompletesCommandsAndPlayerNames(@Player("Alex") Client alex) {
		assertEquals(List.of("msg"), alex.chat().complete("/ms"));
		assertTrue(alex.chat().complete("/msg ").contains("Alex"), "player names are offered");
	}

	@Test
	void expectNoneCatchesWhatMustNotBeSaid(Server server, @Player("Alex") Client alex) {
		server.console("say only public words");
		alex.chat().await("only public words");
		alex.chat().received().expectNone("words nobody said", Duration.ofMillis(500));
	}
}
