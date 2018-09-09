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
import baritone.utils.BlockStateInterface;
import baritone.utils.Helper;
import baritone.utils.InputOverrideHandler;
import baritone.utils.interfaces.Toggleable;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.world.chunk.Chunk;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Brady
 * @since 7/31/2018 11:04 PM
 */
public final class GameEventHandler implements IGameEventListener, Helper {

    private final List<IGameEventListener> listeners = new ArrayList<>();

    @Override
    public final void onTick(TickEvent event) {
        for (IGameEventListener l : listeners) {
            if (canDispatch(l)) {
                l.onTick(event);
            }
        }
    }

    @Override
    public final void onPlayerUpdate(PlayerUpdateEvent event) {
        for (IGameEventListener l : listeners) {
            if (canDispatch(l)) {
                l.onPlayerUpdate(event);
            }
        }
    }

    @Override
    public final void onProcessKeyBinds() {
        InputOverrideHandler inputHandler = Baritone.INSTANCE.getInputOverrideHandler();

        // Simulate the key being held down this tick
        for (InputOverrideHandler.Input input : InputOverrideHandler.Input.values()) {
            KeyBinding keyBinding = input.getKeyBinding();

            if (inputHandler.isInputForcedDown(keyBinding) && !keyBinding.isKeyDown()) {
                int keyCode = keyBinding.getKeyCode();

                if (keyCode < Keyboard.KEYBOARD_SIZE) {
                    KeyBinding.onTick(keyCode < 0 ? keyCode + 100 : keyCode);
                }
            }
        }

        for (IGameEventListener l : listeners) {
            if (canDispatch(l)) {
                l.onProcessKeyBinds();
            }
        }
    }

    @Override
    public final void onSendChatMessage(ChatEvent event) {
        for (IGameEventListener l : listeners) {
            if (canDispatch(l)) {
                l.onSendChatMessage(event);
            }
        }
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


        for (IGameEventListener l : listeners) {
            if (canDispatch(l)) {
                l.onChunkEvent(event);
            }
        }
    }

    @Override
    public final void onRenderPass(RenderEvent event) {
        for (IGameEventListener l : listeners) {
            if (canDispatch(l)) {
                l.onRenderPass(event);
            }
        }
    }

    @Override
    public final void onWorldEvent(WorldEvent event) {
        WorldProvider cache = WorldProvider.INSTANCE;

        BlockStateInterface.clearCachedChunk();

        switch (event.getState()) {
            case PRE:
                break;
            case POST:
                cache.closeWorld();
                if (event.getWorld() != null) {
                    cache.initWorld(event.getWorld());
                }
                break;
        }

        for (IGameEventListener l : listeners) {
            if (canDispatch(l)) {
                l.onWorldEvent(event);
            }
        }
    }

    @Override
    public final void onSendPacket(PacketEvent event) {
        for (IGameEventListener l : listeners) {
            if (canDispatch(l)) {
                l.onSendPacket(event);
            }
        }
    }

    @Override
    public final void onReceivePacket(PacketEvent event) {
        for (IGameEventListener l : listeners) {
            if (canDispatch(l)) {
                l.onReceivePacket(event);
            }
        }
    }

    @Override
    public void onPlayerRelativeMove(RelativeMoveEvent event) {
        for (IGameEventListener l : listeners) {
            if (canDispatch(l)) {
                l.onPlayerRelativeMove(event);
            }
        }
    }

    @Override
    public void onBlockInteract(BlockInteractEvent event) {
        for (IGameEventListener l : listeners) {
            if (canDispatch(l)) {
                l.onBlockInteract(event);
            }
        }
    }

    @Override
    public void onPlayerDeath() {
        for (IGameEventListener l : listeners) {
            if (canDispatch(l)) {
                l.onPlayerDeath();
            }
        }
    }

    @Override
    public void onPathEvent(PathEvent event) {
        for (IGameEventListener l : listeners) {
            if (canDispatch(l)) {
                l.onPathEvent(event);
            }
        }
    }

    public final void registerEventListener(IGameEventListener listener) {
        this.listeners.add(listener);
    }

    private boolean canDispatch(IGameEventListener listener) {
        return !(listener instanceof Toggleable) || ((Toggleable) listener).isEnabled();
    }
}
