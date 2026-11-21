package systems.heartbeat.craftwright.agent.observe;

import com.google.gson.JsonObject;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import systems.heartbeat.craftwright.agent.protocol.Events;
import systems.heartbeat.craftwright.agent.wire.Codecs;
import systems.heartbeat.craftwright.agent.wire.Ids;
import systems.heartbeat.craftwright.agent.wire.TextWire;

/**
 * Turns what the client was just shown into protocol events. The mixins call in here;
 * this class decides the event name and its data, nothing else.
 */
public final class GameObserver {
	private final Events events;
	private final ChatLog chatLog;

	public GameObserver(Events events, ChatLog chatLog) {
		this.events = events;
		this.chatLog = chatLog;
	}

	public void chat(String kind, Component text) {
		chatLog.add(text);
		JsonObject data = text(text);
		data.addProperty("kind", kind);
		events.emit("chat", data);
	}

	public void title(Component text, int fadeIn, int stay, int fadeOut) {
		if (text == null) {
			return;
		}
		JsonObject data = text(text);
		data.addProperty("fadeIn", fadeIn);
		data.addProperty("stay", stay);
		data.addProperty("fadeOut", fadeOut);
		events.emit("title", data);
	}

	public void subtitle(Component text) {
		if (text != null) {
			events.emit("subtitle", text(text));
		}
	}

	public void actionBar(Component text) {
		if (text != null) {
			events.emit("actionBar", text(text));
		}
	}

	public void titlesCleared() {
		events.emit("titleClear", new JsonObject());
	}

	public void disconnected(Component reason) {
		JsonObject data = new JsonObject();
		data.add("reason", TextWire.of(reason));
		events.emit("disconnect", data);
	}

	public void sound(Holder<SoundEvent> sound, SoundSource source, double x, double y, double z, float volume, float pitch) {
		JsonObject data = new JsonObject();
		data.addProperty("id", String.valueOf(sound.value().location()));
		data.addProperty("source", Ids.of(source));
		data.addProperty("x", x);
		data.addProperty("y", y);
		data.addProperty("z", z);
		data.addProperty("volume", volume);
		data.addProperty("pitch", pitch);
		events.emit("sound", data);
	}

	public void particles(ParticleOptions particle, double x, double y, double z, int count) {
		JsonObject data = new JsonObject();
		data.addProperty("id", String.valueOf(BuiltInRegistries.PARTICLE_TYPE.getKey(particle.getType())));
		data.add("options", Codecs.json(ParticleTypes.CODEC, particle));
		data.addProperty("x", x);
		data.addProperty("y", y);
		data.addProperty("z", z);
		data.addProperty("count", count);
		events.emit("particle", data);
	}

	private static JsonObject text(Component text) {
		JsonObject data = new JsonObject();
		data.add("text", TextWire.of(text));
		return data;
	}
}
