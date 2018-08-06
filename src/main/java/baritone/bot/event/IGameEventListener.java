package baritone.bot.event;

import baritone.bot.event.events.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.settings.GameSettings;

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
    void onTick(TickEvent event);

    /**
     * Run once per game tick from before the player rotation is sent to the server.
     * @see EntityPlayerSP#onUpdate()
     */
    void onPlayerUpdate();

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
     * Runs once per world render pass. Two passes are made when {@link GameSettings#anaglyph} is on.
     * <p>
     * <b>Note:</b> {@link GameSettings#anaglyph} has been removed in Minecraft 1.13
     *
     * @see EntityRenderer#renderWorldPass(int, float, long)
     */
    void onRenderPass(RenderEvent event);

    /**
     * Runs before and after whenever a new world is loaded
     *
     * @see Minecraft#loadWorld(WorldClient, String)
     */
    void onWorldEvent(WorldEvent event);
}
