package baritone.bot;

import baritone.bot.behavior.Behavior;
import baritone.bot.event.IGameEventListener;
import baritone.bot.event.events.*;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;

import java.util.function.Consumer;

/**
 * @author Brady
 * @since 7/31/2018 11:04 PM
 */
public final class GameEventHandler implements IGameEventListener {

    GameEventHandler() {}

    @Override
    public final void onTick(TickEvent event) {
        dispatch(behavior -> behavior.onTick(event));
    }

    @Override
    public void onPlayerUpdate() {
        dispatch(Behavior::onPlayerUpdate);
    }

    @Override
    public void onProcessKeyBinds() {
        InputOverrideHandler inputHandler = Baritone.INSTANCE.getInputOverrideHandler();

        // Simulate the key being held down this tick
        for (InputOverrideHandler.Input input : InputOverrideHandler.Input.values()) {
            KeyBinding keyBinding = input.getKeyBinding();

            if (inputHandler.isInputForcedDown(keyBinding) && !keyBinding.isKeyDown()) {
                int keyCode = keyBinding.getKeyCode();

                if (keyCode < Keyboard.KEYBOARD_SIZE)
                    KeyBinding.onTick(keyCode < 0 ? keyCode + 100 : keyCode);
            }
        }

        dispatch(Behavior::onProcessKeyBinds);
    }

    @Override
    public void onSendChatMessage(ChatEvent event) {
        dispatch(behavior -> behavior.onSendChatMessage(event));
    }

    @Override
    public void onChunkEvent(ChunkEvent event) {
        /*

        EventState state = event.getState();
        ChunkEvent.Type type = event.getType();

        boolean isPostPopulate = state == EventState.POST
                && type == ChunkEvent.Type.POPULATE;

        // Whenever the server sends us to another dimension, chunks are unloaded
        // technically after the new world has been loaded, so we perform a check
        // to make sure the chunk being unloaded is already loaded.
        boolean isPreUnload = state == EventState.PRE
                && type == ChunkEvent.Type.UNLOAD
                && mc.world.getChunkProvider().isChunkGeneratedAt(event.getX(), event.getZ());

        if (isPostPopulate || isPreUnload) {
            CachedWorldProvider.INSTANCE.ifWorldLoaded(world ->
                    world.updateCachedChunk(event.getX(), event.getZ(),
                            ChunkPacker.createPackedChunk(mc.world.getChunk(event.getX(), event.getZ()))));
        }
        */

        dispatch(behavior -> behavior.onChunkEvent(event));
    }

    @Override
    public void onRenderPass(RenderEvent event) {
        dispatch(behavior -> behavior.onRenderPass(event));
    }

    @Override
    public void onWorldEvent(WorldEvent event) {
        /*
        CachedWorldProvider cache = CachedWorldProvider.INSTANCE;

        switch (event.getState()) {
            case PRE:
                cache.ifWorldLoaded(CachedWorld::save);
                break;
            case POST:
                cache.closeWorld();
                if (event.getWorld() != null)
                    cache.initWorld(event.getWorld());
                break;
        }
        */

        dispatch(behavior -> behavior.onWorldEvent(event));
    }

    private void dispatch(Consumer<Behavior> dispatchFunction) {
        Baritone.INSTANCE.getBehaviors().stream().filter(Behavior::isEnabled).forEach(dispatchFunction);
    }
}
