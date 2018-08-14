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

package baritone.bot.event;

import baritone.bot.Baritone;
import baritone.bot.InputOverrideHandler;
import baritone.bot.behavior.Behavior;
import baritone.bot.chunk.CachedWorld;
import baritone.bot.chunk.CachedWorldProvider;
import baritone.bot.chunk.ChunkPacker;
import baritone.bot.event.listener.IGameEventListener;
import baritone.bot.event.events.*;
import baritone.bot.event.events.type.EventState;
import baritone.bot.utils.Helper;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;

import java.util.function.Consumer;

/**
 * @author Brady
 * @since 7/31/2018 11:04 PM
 */
public final class GameEventHandler implements IGameEventListener, Helper {

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

        if (Baritone.settings().chuckCaching) {
            if (isPostPopulate || isPreUnload) {
                CachedWorldProvider.INSTANCE.ifWorldLoaded(world ->
                        world.updateCachedChunk(event.getX(), event.getZ(),
                                ChunkPacker.createPackedChunk(mc.world.getChunk(event.getX(), event.getZ()))));
            }
        }

        dispatch(behavior -> behavior.onChunkEvent(event));
    }

    @Override
    public void onRenderPass(RenderEvent event) {
        /*
        CachedWorldProvider.INSTANCE.ifWorldLoaded(world -> world.forEachRegion(region -> region.forEachChunk(chunk -> {
            drawChunkLine(region.getX() * 512 + chunk.getX() * 16, region.getZ() * 512 + chunk.getZ() * 16, event.getPartialTicks());
        })));
        */

        dispatch(behavior -> behavior.onRenderPass(event));
    }

    @Override
    public void onWorldEvent(WorldEvent event) {
        if (Baritone.settings().chuckCaching) {
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
        }

        dispatch(behavior -> behavior.onWorldEvent(event));
    }

    @Override
    public void onSendPacket(PacketEvent event) {
        dispatch(behavior -> behavior.onSendPacket(event));
    }

    @Override
    public void onReceivePacket(PacketEvent event) {
        dispatch(behavior -> behavior.onReceivePacket(event));
    }

    private void dispatch(Consumer<Behavior> dispatchFunction) {
        Baritone.INSTANCE.getBehaviors().stream().filter(Behavior::isEnabled).forEach(dispatchFunction);
    }

    private void drawChunkLine(int posX, int posZ, float partialTicks) {
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(1.0F, 1.0F, 0.0F, 0.4F);
        GlStateManager.glLineWidth(2.0F);
        GlStateManager.disableTexture2D();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        double d0 = mc.getRenderManager().viewerPosX;
        double d1 = mc.getRenderManager().viewerPosY;
        double d2 = mc.getRenderManager().viewerPosZ;
        buffer.begin(3, DefaultVertexFormats.POSITION);
        buffer.pos(posX - d0, 0 - d1, posZ - d2).endVertex();
        buffer.pos(posX - d0, 256 - d1, posZ - d2).endVertex();
        tessellator.draw();

        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }
}
