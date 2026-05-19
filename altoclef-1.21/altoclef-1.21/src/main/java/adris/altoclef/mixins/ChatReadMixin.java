package adris.altoclef.mixins;

import adris.altoclef.eventbus.EventBus;
import adris.altoclef.eventbus.events.ChatMessageEvent;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.message.MessageHandler;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(MessageHandler.class)
public final class ChatReadMixin {
    @Inject(
            method = "onChatMessage",
            at = @At("HEAD")
    )
    private void onChatMessage(SignedMessage message, GameProfile sender, MessageType.Parameters params, CallbackInfo ci) {
        ChatMessageEvent evt = new ChatMessageEvent(message, sender, params);
        EventBus.publish(evt);
    }
}