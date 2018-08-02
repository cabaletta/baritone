package baritone.bot.event;

import baritone.bot.event.events.ChatEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;

/**
 * @author Brady
 * @since 7/31/2018 11:05 PM
 */
public interface IGameEventListener {

    /**
     * Run once per game tick from {@link Minecraft#runTick()}
     */
    void onTick();

    /**
     * Run once per game tick from {@link Minecraft#processKeyBinds()}
     */
    void onProcessKeyBinds();

    /**
     * Runs whenever the client player sends a message to the server {@link EntityPlayerSP#sendChatMessage(String)}
     */
    void onSendChatMessage(ChatEvent event);
}
