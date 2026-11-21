package systems.heartbeat.craftwright;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * The answer to a server list ping: what players see in the multiplayer menu before they join.
 *
 * @param motd          the description lines
 * @param versionName   the version text, shown when the client version does not match
 * @param onlinePlayers the number shown as online
 * @param maxPlayers    the number shown as slots
 * @param playerSample  the lines shown when hovering the player count
 * @param hasIcon       true if the server sends an icon
 */
public record ServerStatus(Text motd, String versionName, int protocolVersion, int onlinePlayers, int maxPlayers,
		List<String> playerSample, boolean hasIcon, JsonObject raw) {

	/** Pings a server the way the multiplayer menu does. */
	public static ServerStatus ping(String host, int port) {
		try (Socket socket = new Socket()) {
			socket.connect(new InetSocketAddress(host, port), 5000);
			socket.setSoTimeout(10000);
			DataOutputStream out = new DataOutputStream(socket.getOutputStream());
			DataInputStream in = new DataInputStream(socket.getInputStream());

			ByteArrayOutputStream handshake = new ByteArrayOutputStream();
			DataOutputStream packet = new DataOutputStream(handshake);
			writeVarInt(packet, 0x00);
			writeVarInt(packet, -1);
			byte[] hostBytes = host.getBytes(StandardCharsets.UTF_8);
			writeVarInt(packet, hostBytes.length);
			packet.write(hostBytes);
			packet.writeShort(port);
			writeVarInt(packet, 1);
			writeVarInt(out, handshake.size());
			out.write(handshake.toByteArray());

			writeVarInt(out, 1);
			writeVarInt(out, 0x00);
			out.flush();

			readVarInt(in);
			int packetId = readVarInt(in);
			if (packetId != 0x00) {
				throw new CraftwrightException("Unexpected answer to the server list ping: packet " + packetId);
			}
			byte[] json = new byte[readVarInt(in)];
			in.readFully(json);
			return parse(JsonParser.parseString(new String(json, StandardCharsets.UTF_8)).getAsJsonObject());
		} catch (IOException e) {
			throw new CraftwrightException("Server list ping to " + host + ":" + port + " failed", e);
		}
	}

	static ServerStatus parse(JsonObject raw) {
		JsonObject version = raw.has("version") ? raw.getAsJsonObject("version") : new JsonObject();
		JsonObject players = raw.has("players") ? raw.getAsJsonObject("players") : new JsonObject();
		List<String> sample = new ArrayList<>();
		if (players.has("sample")) {
			for (JsonElement entry : players.getAsJsonArray("sample")) {
				sample.add(entry.getAsJsonObject().get("name").getAsString());
			}
		}
		return new ServerStatus(
			Text.fromComponent(raw.get("description")),
			version.has("name") ? version.get("name").getAsString() : "",
			version.has("protocol") ? version.get("protocol").getAsInt() : -1,
			players.has("online") ? players.get("online").getAsInt() : 0,
			players.has("max") ? players.get("max").getAsInt() : 0,
			List.copyOf(sample), raw.has("favicon"), raw);
	}

	private static void writeVarInt(DataOutputStream out, int value) throws IOException {
		while ((value & ~0x7F) != 0) {
			out.writeByte((value & 0x7F) | 0x80);
			value >>>= 7;
		}
		out.writeByte(value);
	}

	private static int readVarInt(DataInputStream in) throws IOException {
		int value = 0;
		int shift = 0;
		while (true) {
			int read = in.readUnsignedByte();
			value |= (read & 0x7F) << shift;
			if ((read & 0x80) == 0) {
				return value;
			}
			shift += 7;
			if (shift > 35) {
				throw new IOException("VarInt too long");
			}
		}
	}
}
