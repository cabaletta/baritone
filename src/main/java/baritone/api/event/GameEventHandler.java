/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.api.event;

import baritone.Baritone;
import baritone.api.event.events.*;
import baritone.api.event.events.type.EventState;
import baritone.api.event.listener.IGameEventListener;
import baritone.chunk.WorldProvider;
import baritone.utils.Helper;
import baritone.utils.InputOverrideHandler;
import baritone.utils.interfaces.Toggleable;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.world.chunk.Chunk;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author Brady
 * @since 7/31/2018 11:04 PM
 */
public final class GameEventHandler implements IGameEventListener, Helper {

    private final List<IGameEventListener> listeners = new ArrayList<>();

    @Override
    public final void onTick(TickEvent event) {
        dispatch(listener -> listener.onTick(event));
    }

    @Override
    public final void onPlayerUpdate(PlayerUpdateEvent event) {
        dispatch(listener -> listener.onPlayerUpdate(event));
    }

    @Override
    public final void onProcessKeyBinds() {
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

        dispatch(IGameEventListener::onProcessKeyBinds);
    }

    @Override
    public final void onSendChatMessage(ChatEvent event) {
        dispatch(listener -> listener.onSendChatMessage(event));
    }

    @Override
    public final void onChunkEvent(ChunkEvent event) {
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
            WorldProvider.INSTANCE.ifWorldLoaded(world -> {
                Chunk chunk = mc.world.getChunk(event.getX(), event.getZ());
                world.cache.queueForPacking(chunk);
            });
        }


        dispatch(listener -> listener.onChunkEvent(event));
    }

    @Override
    public final void onRenderPass(RenderEvent event) {
        /*
        WorldProvider.INSTANCE.ifWorldLoaded(world -> world.forEachRegion(region -> region.forEachChunk(chunk -> {
            drawChunkLine(region.getX() * 512 + chunk.getX() * 16, region.getZ() * 512 + chunk.getZ() * 16, event.getPartialTicks());
        })));
        */

        dispatch(listener -> listener.onRenderPass(event));
    }

    @Override
    public final void onWorldEvent(WorldEvent event) {
        WorldProvider cache = WorldProvider.INSTANCE;

        switch (event.getState()) {
            case PRE:
                break;
            case POST:
                cache.closeWorld();
                if (event.getWorld() != null)
                    cache.initWorld(event.getWorld());
                break;
        }

        dispatch(listener -> listener.onWorldEvent(event));
    }

    @Override
    public final void onSendPacket(PacketEvent event) {
        dispatch(listener -> listener.onSendPacket(event));
    }

    @Override
    public final void onReceivePacket(PacketEvent event) {
        dispatch(listener -> listener.onReceivePacket(event));
    }

    @Override
    public final void onQueryItemSlotForBlocks(ItemSlotEvent event) {
        dispatch(listener -> listener.onQueryItemSlotForBlocks(event));
    }

    @Override
    public void onPlayerRelativeMove(RelativeMoveEvent event) {
        dispatch(listener -> listener.onPlayerRelativeMove(event));
    }

    @Override
    public void onBlockInteract(BlockInteractEvent event) {
        dispatch(listener -> listener.onBlockInteract(event));
    }

    @Override
    public void onPlayerDeath() {
        dispatch(IGameEventListener::onPlayerDeath);
    }

    @Override
    public void onPathEvent(PathEvent event) {
        dispatch(listener -> listener.onPathEvent(event));
    }

    public final void registerEventListener(IGameEventListener listener) {
        this.listeners.add(listener);
    }

    private void dispatch(Consumer<IGameEventListener> dispatchFunction) {
        this.listeners.stream().filter(this::canDispatch).forEach(dispatchFunction);
    }

    private boolean canDispatch(IGameEventListener listener) {
        return !(listener instanceof Toggleable) || ((Toggleable) listener).isEnabled();
    }
}
