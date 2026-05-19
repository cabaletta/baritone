package adris.altoclef.eventbus.events;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;

/**
 * Whenever chat appears
 */
public class ChatMessageEvent {
    SignedMessage message;
    GameProfile sender;
    MessageType.Parameters messageType;

    public ChatMessageEvent(SignedMessage message, GameProfile sender, MessageType.Parameters messageType) {
        this.message = message;
        this.sender = sender;
        this.messageType = messageType;
    }
    public String messageContent() {
        return message.getContent().getString();
    }

    public String senderName() {
        return sender.getName();
    }

    public MessageType messageType() {
        return messageType.type().value();
    }
}
