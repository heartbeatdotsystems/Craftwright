# Craftwright

**End-to-end tests for Minecraft servers, played by the real game.**

Craftwright starts a real Paper server and real Minecraft clients without a window, and lets your tests drive them: join, chat, run commands, click through menus, walk, mine. Your assertions read what a player would actually see, such as chat, titles, boss bars, the tab list and the world as the client has it.

```java
@CraftwrightTest(plugins = "build/libs/MyPlugin-*.jar")
class WelcomeTest {

    @Test
    void newPlayersAreGreeted(Server server, @Player("Alex") Client alex) {
        alex.chat().await("Welcome, Alex!");
        alex.hud().titles().await("Welcome");

        alex.command("kit starter");
        Menu kits = alex.awaitMenu("Kits");
        kits.slotNamed("Starter").click();

        alex.player().awaitState("to own a stone sword", state -> alex.player().count("stone_sword") == 1);
        assertTrue(server.logContains("Alex claimed kit starter"));
    }
}
```

> Not an official Minecraft product. Not approved by or associated with Mojang or Microsoft.

## Why a real client

| Approach | What plays | What it cannot tell you |
|---|---|---|
| Mock server (MockBukkit) | nobody, the server is simulated | whether anything works on a real server |
| Protocol bots (Mineflayer, MCProtocolLib) | a re-implementation of the client | what the real client does with your packets, and they lag behind new versions |
| **Craftwright** | the original client | (it is slower: a client takes about a minute to start) |

If your plugin sends a title, opens a menu, hides a player or fakes a block, the only place where that either works or does not is the vanilla client. So that is where Craftwright looks.

## Quick start

Requirements: Java 25 and network access on the first run (Paper, the game and HeadlessMC are downloaded once into `~/.craftwright`). Developed and tested on Linux so far.

```kotlin
// build.gradle.kts
dependencies {
    testImplementation("systems.heartbeat.craftwright:craftwright-junit:0.1.0")
}
```

Not on Maven Central yet. Until then, build it locally with `./gradlew publishToMavenLocal`.

Without JUnit, or for anything unusual, use the session API directly:

```java
try (Craftwright craftwright = Craftwright.create()) {
    Server server = craftwright.paper("26.3")
        .plugin("build/libs/MyPlugin.jar")
        .file("plugins/MyPlugin/config.yml", "welcome: true\n")
        .flatWorld()
        .start();

    Client alex = craftwright.client("Alex").start();
    Client steve = craftwright.client("Steve").start();
    alex.join(server);
    steve.join(server);

    server.console("vanish Steve");
    assertFalse(alex.hud().tabList().players().contains("Steve"));
    assertFalse(alex.world().visiblePlayers().contains("Steve"));
}
```

## What a test can do

**Server** – `craftwright.paper(version)`: plugins from files or URLs, config files, `server.properties`, world templates, flat world, seed. Then `console(...)`, `awaitLog(...)`, `status()` (the server list ping: MOTD, slots, hover sample), `restart()`.

**Connection** – `join`, `leave`, `connect` plus `awaitDisconnect()` to read the kick, ban or whitelist message. Any number of clients, each with its own player name.

**Seeing** – everything as the client has it:

| | |
|---|---|
| `client.chat()` | chat and system messages, with the full component (colours, click and hover events) |
| `client.hud().titles()`, `subtitles()`, `actionBar()` | every title and action bar text received |
| `client.hud().bossBars()`, `tabList()`, `sidebar()` | boss bars, tab list with header, footer and display names, scoreboard sidebar |
| `client.menu()`, `awaitMenu(title)` | open container: type, title, slots, item names and lore |
| `client.player()` | position, health, food, game mode, dimension, inventory |
| `client.world()` | blocks and entities the server sent to this client |

**Doing** – `say`, `command`, `clickChat` (clickable chat texts), `walkTo`, `hold(Key...)`, `jump`, `look`, `lookAt`, `use`, `attack`, `useBlock`, `breakBlock`, `useEntity`, `attackEntity`, `selectHotbar`, `drop`, `respawn`, and on menus `click`, `rightClick`, `shiftClick`, `swapWithHotbar`, `drop`, `close`.

**Waiting** – no sleeps. Every `await…` polls until its condition holds and fails with what was seen instead:

```
Timed out after 15000 ms waiting for chat message containing "Welcome" for Alex
Last chat message received:
  | Alex joined the game
  | You need to accept the rules first: /rules
```

## How it works

```
 your test ──► driver (Java) ◄──── localhost socket, JSON lines ────► agent (Fabric mod)
                 │                                                      inside the real client
                 ├── starts Paper (downloaded from PaperMC)
                 └── starts the client through HeadlessMC, offline account, no window
```

- The **agent** is a small Fabric mod. It reads what the client shows and presses what a player could press. It only connects to `127.0.0.1`, needs a token the driver generates, and does nothing unless the driver started the game.
- The **protocol** is one JSON object per line and documented in [docs/PROTOCOL.md](docs/PROTOCOL.md), so drivers in other languages are possible.
- Servers run in offline mode, which is why any player name works and no account is involved. [HeadlessMC](https://github.com/headlesshq/headlessmc) allows offline accounts for headless runs like this only.

## Supported versions

| Minecraft | Server | Client |
|---|---|---|
| 26.3 | Paper | Fabric agent |

## In CI

```yaml
- uses: actions/setup-java@v5
  with: { distribution: temurin, java-version: '25' }
- uses: actions/cache@v5
  with: { path: ~/.craftwright, key: craftwright-26.3 }
- run: ./gradlew test
```

A GitHub-hosted runner handles a server and two clients. The first run downloads the game, later runs start from the cache.

## Status and roadmap

Early. The API can still change before 1.0.

- [x] Paper server, any number of clients, chat, HUD, menus, movement, world queries
- [x] JUnit 5 extension
- [ ] Screenshots and video (client with rendering)
- [ ] More Minecraft versions, Velocity and multi-server setups
- [ ] Drivers for Node and Python

## What it is not

Not a bot framework for playing on other people's servers, not a cheat client, not a stress tester. It is a test tool for servers you start yourself.
