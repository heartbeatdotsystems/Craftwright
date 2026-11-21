package systems.heartbeat.craftwright.agent;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import systems.heartbeat.craftwright.agent.mixin.BossHealthOverlayAccessor;
import systems.heartbeat.craftwright.agent.mixin.MinecraftInvoker;
import systems.heartbeat.craftwright.agent.mixin.PlayerTabOverlayAccessor;

/** One method per protocol request. Everything here runs on the client thread. */
final class Requests {
	private final Agent agent;
	private final Controls controls;

	Requests(Agent agent, Controls controls) {
		this.agent = agent;
		this.controls = controls;
	}

	JsonElement handle(Minecraft minecraft, String method, JsonObject params) {
		return switch (method) {
			case "client.state" -> clientState(minecraft);
			case "client.connect" -> connect(minecraft, params);
			case "client.disconnect" -> disconnect(minecraft);
			case "client.quit" -> quit(minecraft);
			case "client.screenshot" -> screenshot(minecraft, params);
			case "chat.send" -> chatSend(minecraft, params);
			case "chat.command" -> chatCommand(minecraft, params);
			case "chat.click" -> chatClick(minecraft, params);
			case "hud.bossBars" -> bossBars(minecraft);
			case "hud.tabList" -> tabList(minecraft);
			case "hud.sidebar" -> sidebar(minecraft);
			case "menu.get" -> menuGet(minecraft);
			case "menu.click" -> menuClick(minecraft, params);
			case "menu.close" -> menuClose(minecraft);
			case "player.state" -> playerState(minecraft);
			case "player.look" -> look(minecraft, params);
			case "player.lookAt" -> lookAt(minecraft, params);
			case "player.input" -> input(minecraft, params);
			case "player.jump" -> jump(minecraft);
			case "player.walkTo" -> walkTo(minecraft, params);
			case "player.walking" -> flag("walking", controls.isWalking());
			case "player.selectHotbar" -> selectHotbar(minecraft, params);
			case "player.use" -> use(minecraft);
			case "player.attack" -> attack(minecraft);
			case "player.useBlock" -> useBlock(minecraft, params);
			case "player.breakBlock" -> breakBlock(minecraft, params);
			case "player.breaking" -> flag("breaking", controls.isBreaking());
			case "player.stopBreaking" -> stopBreaking(minecraft);
			case "player.useEntity" -> useEntity(minecraft, params);
			case "player.attackEntity" -> attackEntity(minecraft, params);
			case "player.drop" -> drop(minecraft, params);
			case "player.respawn" -> respawn(minecraft);
			case "player.inventory" -> inventory(minecraft);
			case "player.heldItem" -> heldItem(minecraft);
			case "world.block" -> block(minecraft, params);
			case "world.entities" -> entities(minecraft);
			case "world.time" -> time(minecraft);
			default -> throw new RequestException("Unknown method '" + method + "'. This agent speaks protocol " + Agent.PROTOCOL_VERSION);
		};
	}

	// ── client ──────────────────────────────────────────────────────────────

	private JsonElement clientState(Minecraft minecraft) {
		JsonObject result = new JsonObject();
		Screen screen = minecraft.gui.screen();
		result.addProperty("screen", screen == null ? "" : screen.getClass().getSimpleName());
		result.add("screenTitle", screen == null ? JsonNull.INSTANCE : Texts.wire(screen.getTitle()));
		result.addProperty("inWorld", minecraft.player != null && minecraft.level != null && minecraft.getConnection() != null);
		return result;
	}

	/** Reads the frame the game rendered last and writes it as a PNG. The file appears once it is complete. */
	private JsonElement screenshot(Minecraft minecraft, JsonObject params) {
		java.nio.file.Path file = java.nio.file.Path.of(params.get("file").getAsString());
		java.nio.file.Path partial = file.resolveSibling(file.getFileName() + ".part");
		net.minecraft.client.Screenshot.takeScreenshot(minecraft.gameRenderer.mainRenderTarget(), image -> {
			try (image) {
				if (file.getParent() != null) {
					java.nio.file.Files.createDirectories(file.getParent());
				}
				image.writeToFile(partial);
				java.nio.file.Files.move(partial, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
			} catch (java.io.IOException e) {
				System.out.println("[Craftwright] could not write the screenshot " + file + ": " + e);
			}
		});
		return new JsonObject();
	}

	private JsonElement connect(Minecraft minecraft, JsonObject params) {
		if (minecraft.level != null) {
			throw new RequestException("The client is already on a server. Leave first.");
		}
		String host = params.get("host").getAsString();
		int port = params.get("port").getAsInt();
		ServerData server = new ServerData("Craftwright", host + ":" + port, ServerData.Type.OTHER);
		ConnectScreen.startConnecting(new TitleScreen(), minecraft, new ServerAddress(host, port), server, false, null);
		return new JsonObject();
	}

	private JsonElement disconnect(Minecraft minecraft) {
		if (minecraft.level != null) {
			controls.reset(minecraft);
			minecraft.disconnectFromWorld(Component.translatable("multiplayer.status.quitting"));
		}
		if (minecraft.level == null && !(minecraft.gui.screen() instanceof TitleScreen)) {
			minecraft.gui.setScreen(new TitleScreen());
		}
		return new JsonObject();
	}

	private JsonElement quit(Minecraft minecraft) {
		minecraft.stop();
		return new JsonObject();
	}

	// ── chat ────────────────────────────────────────────────────────────────

	private JsonElement chatSend(Minecraft minecraft, JsonObject params) {
		connection(minecraft).sendChat(params.get("message").getAsString());
		return new JsonObject();
	}

	private JsonElement chatCommand(Minecraft minecraft, JsonObject params) {
		connection(minecraft).sendCommand(params.get("command").getAsString());
		return new JsonObject();
	}

	private JsonElement chatClick(Minecraft minecraft, JsonObject params) {
		String contains = params.get("contains").getAsString();
		List<Component> messages;
		synchronized (agent.recentChat()) {
			messages = new ArrayList<>(agent.recentChat());
		}
		for (int i = messages.size() - 1; i >= 0; i--) {
			Component message = messages.get(i);
			if (!message.getString().contains(contains)) {
				continue;
			}
			ClickEvent click = findClick(message, contains);
			if (click == null) {
				throw new RequestException("The chat message \"" + message.getString() + "\" has nothing to click");
			}
			JsonObject result = new JsonObject();
			if (click instanceof ClickEvent.RunCommand run) {
				String command = run.command();
				connection(minecraft).sendCommand(command.startsWith("/") ? command.substring(1) : command);
				result.addProperty("action", "run_command");
				result.addProperty("value", command);
			} else if (click instanceof ClickEvent.SuggestCommand suggest) {
				result.addProperty("action", "suggest_command");
				result.addProperty("value", suggest.command());
			} else {
				result.addProperty("action", click.action().name().toLowerCase(Locale.ROOT));
				result.addProperty("value", click.toString());
			}
			return result;
		}
		throw new RequestException("No chat message contains \"" + contains + "\"");
	}

	/** Prefers the clickable part that itself carries the words, otherwise the first clickable part. */
	private static ClickEvent findClick(Component message, String contains) {
		ClickEvent first = null;
		for (Component part : message.toFlatList()) {
			ClickEvent click = part.getStyle().getClickEvent();
			if (click == null) {
				continue;
			}
			if (part.getString().contains(contains)) {
				return click;
			}
			if (first == null) {
				first = click;
			}
		}
		return first;
	}

	// ── hud ─────────────────────────────────────────────────────────────────

	private JsonElement bossBars(Minecraft minecraft) {
		JsonArray bars = new JsonArray();
		for (LerpingBossEvent event : ((BossHealthOverlayAccessor) minecraft.gui.hud.getBossOverlay()).craftwright$events().values()) {
			JsonObject bar = new JsonObject();
			bar.add("name", Texts.wire(event.getName()));
			bar.addProperty("progress", event.getProgress());
			bar.addProperty("color", event.getColor().name().toLowerCase(Locale.ROOT));
			bar.addProperty("overlay", event.getOverlay().name().toLowerCase(Locale.ROOT));
			bars.add(bar);
		}
		JsonObject result = new JsonObject();
		result.add("bossBars", bars);
		return result;
	}

	private JsonElement tabList(Minecraft minecraft) {
		ClientPacketListener connection = connection(minecraft);
		PlayerTabOverlayAccessor overlay = (PlayerTabOverlayAccessor) minecraft.gui.hud.getTabList();
		JsonArray entries = new JsonArray();
		for (PlayerInfo info : connection.getListedOnlinePlayers()) {
			JsonObject entry = new JsonObject();
			entry.addProperty("name", info.getProfile().name());
			entry.add("displayName", Texts.wire(minecraft.gui.hud.getTabList().getNameForDisplay(info)));
			entry.addProperty("gameMode", info.getGameMode().getName());
			entry.addProperty("latency", info.getLatency());
			entries.add(entry);
		}
		JsonObject result = new JsonObject();
		result.add("header", Texts.wire(overlay.craftwright$header()));
		result.add("footer", Texts.wire(overlay.craftwright$footer()));
		result.add("entries", entries);
		return result;
	}

	private JsonElement sidebar(Minecraft minecraft) {
		Scoreboard scoreboard = connection(minecraft).scoreboard();
		Objective objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
		JsonObject result = new JsonObject();
		result.addProperty("shown", objective != null);
		if (objective == null) {
			return result;
		}
		List<PlayerScoreEntry> scores = new ArrayList<>(scoreboard.listPlayerScores(objective));
		scores.removeIf(PlayerScoreEntry::isHidden);
		scores.sort(Comparator.comparingInt(PlayerScoreEntry::value).reversed().thenComparing(PlayerScoreEntry::owner, String.CASE_INSENSITIVE_ORDER));
		JsonArray lines = new JsonArray();
		for (PlayerScoreEntry score : scores.subList(0, Math.min(15, scores.size()))) {
			JsonObject line = new JsonObject();
			PlayerTeam team = scoreboard.getPlayersTeam(score.owner());
			line.add("text", Texts.wire(PlayerTeam.formatNameForTeam(team, score.ownerName())));
			line.addProperty("score", score.value());
			lines.add(line);
		}
		result.add("title", Texts.wire(objective.getDisplayName()));
		result.add("lines", lines);
		return result;
	}

	// ── menus ───────────────────────────────────────────────────────────────

	private JsonElement menuGet(Minecraft minecraft) {
		JsonObject result = new JsonObject();
		LocalPlayer player = minecraft.player;
		if (player == null || player.containerMenu == player.inventoryMenu) {
			result.addProperty("open", false);
			return result;
		}
		AbstractContainerMenu menu = player.containerMenu;
		Screen screen = minecraft.gui.screen();
		result.addProperty("open", true);
		result.addProperty("id", menu.containerId);
		result.addProperty("type", menuType(menu));
		result.add("title", screen instanceof AbstractContainerScreen<?> ? Texts.wire(screen.getTitle()) : Texts.wire(Component.empty()));
		JsonArray slots = new JsonArray();
		for (int i = 0; i < menu.slots.size(); i++) {
			Slot slot = menu.slots.get(i);
			JsonObject entry = new JsonObject();
			entry.addProperty("index", i);
			entry.add("item", item(slot.getItem()));
			entry.addProperty("playerInventory", slot.container instanceof Inventory);
			slots.add(entry);
		}
		result.add("slots", slots);
		result.add("carried", item(menu.getCarried()));
		return result;
	}

	private static String menuType(AbstractContainerMenu menu) {
		try {
			return String.valueOf(BuiltInRegistries.MENU.getKey(menu.getType()));
		} catch (UnsupportedOperationException e) {
			return "";
		}
	}

	private JsonElement menuClick(Minecraft minecraft, JsonObject params) {
		LocalPlayer player = player(minecraft);
		AbstractContainerMenu menu = player.containerMenu;
		if (menu == player.inventoryMenu) {
			throw new RequestException("No menu is open");
		}
		if (params.has("menu") && params.get("menu").getAsInt() != menu.containerId) {
			throw new RequestException("That menu is no longer open, the server opened another one. Ask for the menu again.");
		}
		int slot = params.get("slot").getAsInt();
		if (!menu.isValidSlotIndex(slot)) {
			throw new RequestException("The open menu has no slot " + slot);
		}
		ContainerInput input = switch (params.get("mode").getAsString()) {
			case "pickup" -> ContainerInput.PICKUP;
			case "quick_move" -> ContainerInput.QUICK_MOVE;
			case "swap" -> ContainerInput.SWAP;
			case "clone" -> ContainerInput.CLONE;
			case "throw" -> ContainerInput.THROW;
			case "pickup_all" -> ContainerInput.PICKUP_ALL;
			default -> throw new RequestException("Unknown click mode '" + params.get("mode").getAsString() + "'");
		};
		minecraft.gameMode.handleContainerInput(menu.containerId, slot, params.get("button").getAsInt(), input, player);
		return new JsonObject();
	}

	private JsonElement menuClose(Minecraft minecraft) {
		player(minecraft).closeContainer();
		return new JsonObject();
	}

	private static JsonElement item(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return JsonNull.INSTANCE;
		}
		JsonObject item = new JsonObject();
		item.addProperty("id", String.valueOf(BuiltInRegistries.ITEM.getKey(stack.getItem())));
		item.addProperty("count", stack.getCount());
		item.add("name", Texts.wire(stack.getHoverName()));
		JsonArray lore = new JsonArray();
		ItemLore lines = stack.get(DataComponents.LORE);
		if (lines != null) {
			for (Component line : lines.lines()) {
				lore.add(Texts.wire(line));
			}
		}
		item.add("lore", lore);
		return item;
	}

	// ── player ──────────────────────────────────────────────────────────────

	private JsonElement playerState(Minecraft minecraft) {
		LocalPlayer player = player(minecraft);
		JsonObject state = new JsonObject();
		state.addProperty("x", player.getX());
		state.addProperty("y", player.getY());
		state.addProperty("z", player.getZ());
		state.addProperty("yaw", player.getYRot());
		state.addProperty("pitch", player.getXRot());
		state.addProperty("health", player.getHealth());
		state.addProperty("food", player.getFoodData().getFoodLevel());
		state.addProperty("saturation", player.getFoodData().getSaturationLevel());
		state.addProperty("experienceLevel", player.experienceLevel);
		state.addProperty("gameMode", minecraft.gameMode.getPlayerMode().getName());
		state.addProperty("dimension", String.valueOf(minecraft.level.dimension().identifier()));
		state.addProperty("onGround", player.onGround());
		state.addProperty("dead", player.isDeadOrDying());
		state.addProperty("selectedHotbarSlot", player.getInventory().getSelectedSlot());
		return state;
	}

	private JsonElement look(Minecraft minecraft, JsonObject params) {
		LocalPlayer player = player(minecraft);
		player.setYRot(params.get("yaw").getAsFloat());
		player.setXRot(Math.clamp(params.get("pitch").getAsFloat(), -90f, 90f));
		return new JsonObject();
	}

	private JsonElement lookAt(Minecraft minecraft, JsonObject params) {
		turnTo(player(minecraft), vec(params));
		return new JsonObject();
	}

	private static void turnTo(LocalPlayer player, Vec3 target) {
		Vec3 eye = player.getEyePosition();
		double dx = target.x - eye.x;
		double dy = target.y - eye.y;
		double dz = target.z - eye.z;
		double horizontal = Math.sqrt(dx * dx + dz * dz);
		player.setYRot((float) Math.toDegrees(Math.atan2(-dx, dz)));
		player.setXRot((float) -Math.toDegrees(Math.atan2(dy, horizontal)));
	}

	private JsonElement input(Minecraft minecraft, JsonObject params) {
		player(minecraft);
		controls.stopWalking();
		controls.forward = bool(params, "forward");
		controls.back = bool(params, "back");
		controls.left = bool(params, "left");
		controls.right = bool(params, "right");
		controls.jump = bool(params, "jump");
		controls.sneak = bool(params, "sneak");
		controls.sprint = bool(params, "sprint");
		return new JsonObject();
	}

	private JsonElement jump(Minecraft minecraft) {
		player(minecraft);
		controls.jumpOnce();
		return new JsonObject();
	}

	private JsonElement walkTo(Minecraft minecraft, JsonObject params) {
		player(minecraft);
		closeScreenForPlay(minecraft);
		controls.walkTo(params.get("x").getAsDouble(), params.get("z").getAsDouble());
		return new JsonObject();
	}

	private JsonElement selectHotbar(Minecraft minecraft, JsonObject params) {
		int slot = params.get("slot").getAsInt();
		if (slot < 0 || slot > 8) {
			throw new RequestException("Hotbar slots go from 0 to 8, got " + slot);
		}
		player(minecraft).getInventory().setSelectedSlot(slot);
		return new JsonObject();
	}

	private JsonElement use(Minecraft minecraft) {
		LocalPlayer player = player(minecraft);
		minecraft.hitResult = player.raycastHitResult(1.0f, player);
		((MinecraftInvoker) minecraft).craftwright$startUseItem();
		return new JsonObject();
	}

	private JsonElement attack(Minecraft minecraft) {
		LocalPlayer player = player(minecraft);
		minecraft.hitResult = player.raycastHitResult(1.0f, player);
		((MinecraftInvoker) minecraft).craftwright$startAttack();
		return new JsonObject();
	}

	private JsonElement useBlock(Minecraft minecraft, JsonObject params) {
		LocalPlayer player = player(minecraft);
		BlockPos pos = blockPos(params);
		requireReach(player, pos);
		Vec3 center = Vec3.atCenterOf(pos);
		turnTo(player, center);
		BlockHitResult hit = new BlockHitResult(center, faceTowards(player, center), pos, false);
		minecraft.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hit);
		return new JsonObject();
	}

	private JsonElement breakBlock(Minecraft minecraft, JsonObject params) {
		LocalPlayer player = player(minecraft);
		BlockPos pos = blockPos(params);
		requireReach(player, pos);
		if (minecraft.level.getBlockState(pos).isAir()) {
			throw new RequestException("There is no block at " + pos.toShortString());
		}
		closeScreenForPlay(minecraft);
		Vec3 center = Vec3.atCenterOf(pos);
		turnTo(player, center);
		controls.startBreaking(minecraft, pos, faceTowards(player, center));
		return new JsonObject();
	}

	private JsonElement stopBreaking(Minecraft minecraft) {
		controls.stopBreaking(minecraft);
		return new JsonObject();
	}

	private JsonElement useEntity(Minecraft minecraft, JsonObject params) {
		LocalPlayer player = player(minecraft);
		Entity entity = nearestEntity(minecraft, params.get("target").getAsString());
		turnTo(player, entity.getEyePosition());
		minecraft.gameMode.interact(player, entity, new EntityHitResult(entity), InteractionHand.MAIN_HAND);
		return new JsonObject();
	}

	private JsonElement attackEntity(Minecraft minecraft, JsonObject params) {
		LocalPlayer player = player(minecraft);
		Entity entity = nearestEntity(minecraft, params.get("target").getAsString());
		turnTo(player, entity.getEyePosition());
		minecraft.gameMode.attack(player, entity);
		return new JsonObject();
	}

	private Entity nearestEntity(Minecraft minecraft, String target) {
		LocalPlayer player = minecraft.player;
		double reach = player.entityInteractionRange() + 1.0;
		String wanted = target.contains(":") ? target : "minecraft:" + target;
		Entity best = null;
		for (Entity entity : minecraft.level.entitiesForRendering()) {
			if (entity == player) {
				continue;
			}
			boolean matches = String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType())).equals(wanted)
				|| entity.getName().getString().equals(target);
			if (matches && entity.distanceTo(player) <= reach && (best == null || entity.distanceTo(player) < best.distanceTo(player))) {
				best = entity;
			}
		}
		if (best == null) {
			throw new RequestException("No entity '" + target + "' within reach (" + reach + " blocks)");
		}
		return best;
	}

	private JsonElement drop(Minecraft minecraft, JsonObject params) {
		minecraft.gameMode.dropItem(player(minecraft), bool(params, "wholeStack"));
		return new JsonObject();
	}

	private JsonElement respawn(Minecraft minecraft) {
		LocalPlayer player = player(minecraft);
		if (!player.isDeadOrDying()) {
			throw new RequestException("The player is not dead");
		}
		connection(minecraft).send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
		minecraft.gui.setScreen(null);
		return new JsonObject();
	}

	private JsonElement inventory(Minecraft minecraft) {
		Inventory inventory = player(minecraft).getInventory();
		JsonArray slots = new JsonArray();
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			slots.add(item(inventory.getItem(i)));
		}
		JsonObject result = new JsonObject();
		result.add("slots", slots);
		return result;
	}

	private JsonElement heldItem(Minecraft minecraft) {
		JsonObject result = new JsonObject();
		result.add("item", item(player(minecraft).getMainHandItem()));
		return result;
	}

	// ── world ───────────────────────────────────────────────────────────────

	private JsonElement block(Minecraft minecraft, JsonObject params) {
		ClientLevel level = level(minecraft);
		BlockPos pos = blockPos(params);
		JsonObject result = new JsonObject();
		JsonObject properties = new JsonObject();
		if (!level.isLoaded(pos)) {
			result.addProperty("id", "minecraft:void_air");
			result.addProperty("blockEntity", false);
		} else {
			BlockState state = level.getBlockState(pos);
			result.addProperty("id", String.valueOf(BuiltInRegistries.BLOCK.getKey(state.getBlock())));
			for (Property<?> property : state.getProperties()) {
				properties.addProperty(property.getName(), valueName(state, property));
			}
			result.addProperty("blockEntity", level.getBlockEntity(pos) != null);
		}
		result.add("properties", properties);
		return result;
	}

	private static <T extends Comparable<T>> String valueName(BlockState state, Property<T> property) {
		return property.getName(state.getValue(property));
	}

	private JsonElement entities(Minecraft minecraft) {
		ClientLevel level = level(minecraft);
		JsonArray entities = new JsonArray();
		for (Entity entity : level.entitiesForRendering()) {
			if (entity == minecraft.player) {
				continue;
			}
			JsonObject entry = new JsonObject();
			entry.addProperty("id", entity.getId());
			entry.addProperty("uuid", entity.getUUID().toString());
			entry.addProperty("type", String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType())));
			entry.add("name", Texts.wire(entity.getName()));
			entry.addProperty("x", entity.getX());
			entry.addProperty("y", entity.getY());
			entry.addProperty("z", entity.getZ());
			entry.addProperty("invisible", entity.isInvisible());
			entities.add(entry);
		}
		JsonObject result = new JsonObject();
		result.add("entities", entities);
		return result;
	}

	private JsonElement time(Minecraft minecraft) {
		ClientLevel level = level(minecraft);
		JsonObject result = new JsonObject();
		result.addProperty("dayTime", Math.floorMod(level.getDefaultClockTime(), 24000L));
		result.addProperty("raining", level.isRaining());
		return result;
	}

	// ── helpers ─────────────────────────────────────────────────────────────

	private static LocalPlayer player(Minecraft minecraft) {
		if (minecraft.player == null || minecraft.level == null) {
			throw new RequestException("The client is not in a world");
		}
		return minecraft.player;
	}

	private static ClientLevel level(Minecraft minecraft) {
		player(minecraft);
		return minecraft.level;
	}

	private static ClientPacketListener connection(Minecraft minecraft) {
		ClientPacketListener connection = minecraft.getConnection();
		if (connection == null) {
			throw new RequestException("The client is not connected to a server");
		}
		return connection;
	}

	/** Movement and mining only work while no screen has the keyboard, like for a real player. */
	private static void closeScreenForPlay(Minecraft minecraft) {
		LocalPlayer player = minecraft.player;
		if (minecraft.gui.screen() != null && player != null && !player.isDeadOrDying()) {
			if (player.containerMenu != player.inventoryMenu) {
				player.closeContainer();
			} else {
				minecraft.gui.setScreen(null);
			}
		}
	}

	private static void requireReach(LocalPlayer player, BlockPos pos) {
		double reach = player.blockInteractionRange() + 1.0;
		if (player.getEyePosition().distanceTo(Vec3.atCenterOf(pos)) > reach) {
			throw new RequestException("The block at " + pos.toShortString() + " is out of reach, walk closer first");
		}
	}

	private static Direction faceTowards(LocalPlayer player, Vec3 center) {
		Vec3 eye = player.getEyePosition();
		return Direction.getApproximateNearest(eye.x - center.x, eye.y - center.y, eye.z - center.z);
	}

	private static BlockPos blockPos(JsonObject params) {
		return BlockPos.containing(params.get("x").getAsDouble(), params.get("y").getAsDouble(), params.get("z").getAsDouble());
	}

	private static Vec3 vec(JsonObject params) {
		return new Vec3(params.get("x").getAsDouble(), params.get("y").getAsDouble(), params.get("z").getAsDouble());
	}

	private static boolean bool(JsonObject params, String key) {
		return params.has(key) && params.get(key).getAsBoolean();
	}

	private static JsonObject flag(String name, boolean value) {
		JsonObject result = new JsonObject();
		result.addProperty(name, value);
		return result;
	}
}
