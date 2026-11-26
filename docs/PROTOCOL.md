# Craftwright wire protocol, version 1

The agent inside the client connects to the driver over TCP on `127.0.0.1`. Both sides send one JSON object per line, UTF-8.

## Start

The game is started with these JVM properties:

| Property | Meaning |
|---|---|
| `craftwright.connect` | `host:port` of the driver. Without it the agent stays off. |
| `craftwright.token` | shared secret, repeated in the hello message |
| `craftwright.client` | id of this client in the test |
| `craftwright.username` | player name for the offline account |

Once the title screen is up the agent connects and sends:

```json
{"token":"…","client":"Alex","protocol":1,"agent":"0.1.0","minecraft":"26.3","player":"Alex"}
```

## Requests and responses

```json
→ {"id":7,"method":"chat.command","params":{"command":"spawn"}}
← {"id":7,"result":{}}
← {"id":8,"error":{"message":"The client is not in a world"}}
```

Requests run on the client thread, in the order they arrive. A few answer later, when the game has the result: `client.screenshot` and `chat.complete`.

## Events

```json
← {"event":"chat","data":{"kind":"system","text":{"plain":"Welcome","json":{"text":"Welcome","color":"gold"}}}}
```

| Event | Data |
|---|---|
| `chat` | `kind` (`system`, `player`, `client`), `text` |
| `title` | `text`, `fadeIn`, `stay`, `fadeOut` (ticks) |
| `subtitle`, `actionBar` | `text` |
| `titleClear` | |
| `sound` | `id`, `source`, `x`, `y`, `z`, `volume`, `pitch` |
| `particle` | `id`, `options`, `x`, `y`, `z`, `count` |
| `disconnect` | `reason` (the server's kick reason, else the text of the disconnect screen) |

## Shapes

- A **text** is `{"plain": "...", "json": <text component>}`.
- An **item** is `null` for an empty slot, otherwise `id`, `count`, `name`, `lore[]`, `enchanted`, `glint`, `damage` and `data`. `data` is the stack as data packs write it: `id`, `count`, `components`.
- An **entity** is `id`, `uuid`, `type`, `name`, `displayName`, `customName` (or `null`), `customNameVisible`, `x`, `y`, `z`, `invisible`, `glowing`, `sneaking`, and for living entities `health` and `equipment` (slot name to item).

## Methods

### Client

| Method | Params | Result |
|---|---|---|
| `client.state` | | `screen`, `screenTitle`, `inWorld` |
| `client.connect` | `host`, `port` | |
| `client.disconnect` | | |
| `client.quit` | | |
| `client.screenshot` | `file` | answers when the PNG is written |

### Chat

| Method | Params | Result |
|---|---|---|
| `chat.send` | `message` | |
| `chat.command` | `command`, with or without slash | |
| `chat.click` | `contains` | `click`: the click event as chat components write it (`action` plus `command`, `url` or `value`). A `run_command` click is run. |
| `chat.complete` | `input` | `suggestions[]`, empty if the server offers nothing or stays silent for 3 s |

### HUD and scoreboard

| Method | Params | Result |
|---|---|---|
| `hud.bossBars` | | `bossBars[]`: `name`, `progress`, `color`, `overlay` |
| `hud.tabList` | | `header`, `footer`, `entries[]`: `name`, `displayName`, `gameMode`, `latency` |
| `scoreboard.display` | `slot` (`sidebar`, `list`, `below_name`) | `shown`, `objective`, `title`, `lines[]`: `owner`, `text`, `score`, `formatted` |
| `scoreboard.teams` | | `teams[]`: `name`, `displayName`, `prefix`, `suffix`, `nameTagVisibility`, `members[]` |

### Menus and trades

| Method | Params | Result |
|---|---|---|
| `menu.get` | | `open`, and if open `id`, `type`, `title`, `slots[]` (`index`, `item`, `playerInventory`), `carried` |
| `menu.inventory` | | the player's own inventory in the same form |
| `menu.click` | `menu`, `slot` (`-999` is next to the menu), `button`, `mode` (`pickup`, `quick_move`, `swap`, `clone`, `throw`, `pickup_all`) | |
| `menu.drag` | `menu`, `slots[]`, `button` (`0` spread evenly, `1` one each) | |
| `menu.close` | | |
| `trade.offers` | | `merchantLevel`, `offers[]`: `index`, `firstCost`, `secondCost`, `result`, `uses`, `maxUses`, `outOfStock` |
| `trade.select` | `index` | |
| `anvil.rename` | `name` | |

### Player

| Method | Params | Result |
|---|---|---|
| `player.state` | | `x`, `y`, `z`, `yaw`, `pitch`, `health`, `food`, `saturation`, `experienceLevel`, `experienceProgress`, `gameMode`, `dimension`, `onGround`, `dead`, `sneaking`, `sprinting`, `flying`, `mayFly`, `selectedHotbarSlot`, `effects[]` (`id`, `amplifier`, `duration`) |
| `player.inventory` | | `slots[]` of items |
| `player.heldItem` | | `item`, `offHand` |
| `player.selectHotbar` | `slot` | |
| `player.swapHands` | | |
| `player.drop` | `wholeStack` | |
| `player.respawn` | | |
| `player.look` | `yaw`, `pitch` | |
| `player.lookAt` | `x`, `y`, `z` | |
| `player.input` | `forward`, `back`, `left`, `right`, `jump`, `sneak`, `sprint` | |
| `player.jump` | | |
| `player.walkTo` | `x`, `z` | |
| `player.walking` | | `walking` |
| `player.use`, `player.attack` | | |
| `player.useBlock` | `x`, `y`, `z`, optional `face` (`up`, `down`, `north`, `south`, `east`, `west`) | |
| `player.breakBlock` | `x`, `y`, `z` | |
| `player.breaking` | | `breaking` |
| `player.stopBreaking` | | |
| `player.useEntity`, `player.attackEntity` | `target` (type id, name or UUID) | |

### World

| Method | Params | Result |
|---|---|---|
| `world.block` | `x`, `y`, `z` | `id`, `properties`, `blockEntity` (data or `null`) |
| `world.entities` | | `entities[]` |
| `world.time` | | `dayTime`, `raining` |
| `world.border` | | `centerX`, `centerZ`, `size` |
