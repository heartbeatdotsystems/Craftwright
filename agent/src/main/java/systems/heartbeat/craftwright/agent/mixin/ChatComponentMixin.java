package systems.heartbeat.craftwright.agent.mixin;

import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import systems.heartbeat.craftwright.agent.Agent;

/** Reports every line that reaches the chat window, which is exactly what a player would read. */
@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {
	@Inject(method = "addServerSystemMessage", at = @At("HEAD"))
	private void craftwright$system(Component text, CallbackInfo callback) {
		Agent.get().chatEvent("system", text);
	}

	@Inject(method = "addPlayerMessage", at = @At("HEAD"))
	private void craftwright$player(Component text, MessageSignature signature, GuiMessageTag tag, CallbackInfo callback) {
		Agent.get().chatEvent("player", text);
	}

	@Inject(method = "addClientSystemMessage", at = @At("HEAD"))
	private void craftwright$client(Component text, CallbackInfo callback) {
		Agent.get().chatEvent("client", text);
	}
}
