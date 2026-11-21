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

Requests run on the client thread, in the order they arrive.

## Events

```json
← {"event":"chat","data":{"kind":"system","text":{"plain":"Welcome","json":{"text":"Welcome","color":"gold"}}}}
```

| Event | Data |
|---|---|
| `chat` | `kind` (`system`, `player`, `client`), `text` |
| `title`, `subtitle`, `actionBar` | `text` |
| `disconnect` | `reason` |

A text is always `{"plain": "...", "json": <text component>}`.

## Methods

| Method | Params | Result |
|---|---|---|
| `client.state` | | `screen`, `screenTitle`, `inWorld` |
| `client.connect` | `host`, `port` | |
| `client.disconnect` | | |
| `client.quit` | | |
| `chat.send` | `message` | |
| `chat.command` | `command` (no slash) | |
| `chat.click` | `contains` | `action`, `value` |
| `hud.bossBars` | | `bossBars[]`: `name`, `progress`, `color`, `overlay` |
| `hud.tabList` | | `header`, `footer`, `entries[]`: `name`, `displayName`, `gameMode`, `latency` |
| `hud.sidebar` | | `shown`, `title`, `lines[]`: `text`, `score` |
| `menu.get` | | `open`, `id`, `type`, `title`, `slots[]`: `index`, `item`, `playerInventory`; `carried` |
| `menu.click` | `menu`, `slot`, `button`, `mode` (`pickup`, `quick_move`, `swap`, `clone`, `throw`, `pickup_all`) | |
| `menu.close` | | |
| `player.state` | | `x`, `y`, `z`, `yaw`, `pitch`, `health`, `food`, `saturation`, `experienceLevel`, `gameMode`, `dimension`, `onGround`, `dead`, `selectedHotbarSlot` |
| `player.look` | `yaw`, `pitch` | |
| `player.lookAt` | `x`, `y`, `z` | |
| `player.input` | `forward`, `back`, `left`, `right`, `jump`, `sneak`, `sprint` | |
| `player.jump` | | |
| `player.walkTo` | `x`, `z` | |
| `player.walking` | | `walking` |
| `player.selectHotbar` | `slot` | |
| `player.use`, `player.attack` | | |
| `player.useBlock`, `player.breakBlock` | `x`, `y`, `z` | |
| `player.breaking` | | `breaking` |
| `player.stopBreaking` | | |
| `player.useEntity`, `player.attackEntity` | `target` (type id or name) | |
| `player.drop` | `wholeStack` | |
| `player.respawn` | | |
| `player.inventory` | | `slots[]` of items |
| `player.heldItem` | | `item` |
| `world.block` | `x`, `y`, `z` | `id`, `properties`, `blockEntity` |
| `world.entities` | | `entities[]`: `id`, `uuid`, `type`, `name`, `x`, `y`, `z`, `invisible` |
| `world.time` | | `dayTime`, `raining` |

An item is `null` or `{"id","count","name","lore[]"}`.
