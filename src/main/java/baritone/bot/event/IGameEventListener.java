package baritone.bot.event;

import baritone.bot.event.events.ChatEvent;
import baritone.bot.event.events.ChunkEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;

/**
 * @author Brady
 * @since 7/31/2018 11:05 PM
 */
public interface IGameEventListener {

    /**
     * Run once per game tick before screen input is handled.
     *
     * @see Minecraft#runTick()
     */
    void onTick();

    /**
     * Run once per game tick from before keybinds are processed.
     *
     * @see Minecraft#processKeyBinds()
     */
    void onProcessKeyBinds();

    /**
     * Runs whenever the client player sends a message to the server.
     *
     * @see EntityPlayerSP#sendChatMessage(String)
     */
    void onSendChatMessage(ChatEvent event);


    /**
     * Runs before and after whenever a chunk is either loaded, unloaded, or populated.
     *
     * @see WorldClient#doPreChunk(int, int, boolean)
     */
    void onChunkEvent(ChunkEvent event);

    /**
     * Runs once each frame
     */
    void onRenderPass();
}
