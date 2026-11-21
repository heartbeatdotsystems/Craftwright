# Craftwright

**End-to-end tests for Minecraft servers, played by the real game.**

Craftwright starts a real Paper server and real Minecraft clients without a window, and lets your Java tests drive them: join, chat, run commands, click through menus, walk, mine. Your assertions read what a player would actually see.

```java
@CraftwrightTest(plugins = "build/libs/MyPlugin-*.jar")
class WelcomeTest {

    @Test
    void newPlayersAreGreeted(Server server, @Player("Alex") Client alex) {
        alex.chat().await("Welcome, Alex!");
        alex.hud().titles().await("Welcome");

        alex.chat().command("kit");
        Menu kits = alex.menus().await("Kits");
        kits.slotNamed("Starter").click();

        alex.player().awaitCount("stone_sword", 1);
        assertTrue(server.logContains("Alex claimed kit starter"));
    }
}
```

A Java library with a JUnit 5 extension. Minecraft 26.3, Paper, Java 25.

## Documentation

Everything for developers is in the docs: **[pages.heartbeat.systems/craftwright-wiki](https://pages.heartbeat.systems/craftwright-wiki/en/)**

- [Getting started](https://pages.heartbeat.systems/craftwright-wiki/en/start.html): add it to a Gradle build, write the first test, run it
- [What a test can do](https://pages.heartbeat.systems/craftwright-wiki/en/api.html): the API with examples
- [What it cannot do](https://pages.heartbeat.systems/craftwright-wiki/en/limits.html): the limits, so you know before you start
- [Running in CI](https://pages.heartbeat.systems/craftwright-wiki/en/ci.html): the GitHub Action in this repository
- [How it works](https://pages.heartbeat.systems/craftwright-wiki/en/internals.html): driver, agent, protocol, security

## Working on Craftwright

```
./gradlew build                   # compiles agent, driver and junit, runs the unit tests
./gradlew e2eTest --no-parallel   # the self tests: a real server and real clients, a few minutes
```

`agent/` is the Fabric mod inside the client, `driver/` the Java library a test calls, `junit/` the JUnit 5 extension. The interface between agent and driver is described in [docs/PROTOCOL.md](docs/PROTOCOL.md).

## License

Craftwright is free software under the [GNU Lesser General Public License, version 3](COPYING.LESSER).
You can use it in tests of any project, open or closed. Changes to Craftwright itself stay open under the same license.

Not an official Minecraft product. Not approved by or associated with Mojang or Microsoft. Minecraft is downloaded from the official servers on the machine that runs the tests.
