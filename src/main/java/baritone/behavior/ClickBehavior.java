/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.behavior;

import baritone.Baritone;
import baritone.api.Settings;
import baritone.api.event.events.RenderEvent;
import baritone.api.event.events.TickEvent;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.Helper;
import baritone.utils.IRenderer;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.awt.*;

import static baritone.api.command.IBaritoneChatControl.FORCE_COMMAND_PREFIX;

public final class ClickBehavior extends Behavior implements Helper {
    private boolean active;
    private BetterBlockPos pos1;
    private BetterBlockPos hovered;

    public ClickBehavior(Baritone baritone) {
        super(baritone);
    }

    public void toggle() {
        if (active) {
            reset();
            logDirect("Click mode cancelled");
        } else {
            active = true;
            logDirect("Click mode enabled, click two blocks to select an area, or run click again to cancel");
        }
    }

    private void reset() {
        active = false;
        pos1 = null;
        hovered = null;
    }

    @Override
    public void onTick(TickEvent event) {
        if (!active) {
            return;
        }
        if (event.getType() == TickEvent.Type.OUT) {
            reset();
            return;
        }
        Minecraft mc = ctx.minecraft();
        if (mc.screen != null) {
            return;
        }

        boolean clicked = false;
        while (mc.options.keyAttack.consumeClick()) {
            clicked = true;
        }
        while (mc.options.keyUse.consumeClick()) {
            clicked = true;
        }
        mc.options.keyAttack.setDown(false);
        mc.options.keyUse.setDown(false);
        if (clicked) {
            onClick();
        }
    }

    private void onClick() {
        if (hovered == null) {
            logDirect("No block in sight");
            return;
        }
        if (pos1 == null) {
            pos1 = hovered;
            logDirect("Position 1 set now click a second block");
            return;
        }
        baritone.getSelectionManager().addSelection(pos1, hovered);
        MutableComponent component = Component.literal("Selection made! For usage: " + Baritone.settings().prefix.value + "help sel");
        component.setStyle(component.getStyle()
                .withColor(ChatFormatting.WHITE)
                .withClickEvent(new ClickEvent(
                        ClickEvent.Action.RUN_COMMAND,
                        FORCE_COMMAND_PREFIX + "help sel"
                )));
        logDirect(component);
        reset();
    }

    @Override
    public void onRenderPass(RenderEvent event) {
        if (!active || ctx.world() == null) {
            return;
        }

        boolean holdingBlock = ctx.player().isHolding(i -> i.getItem() instanceof BlockItem);
        hovered = raycast(event.getPartialTicks(), holdingBlock);

        Settings settings = Baritone.settings();
        PoseStack stack = event.getModelViewStack();
        if (pos1 != null) {
            BetterBlockPos to = hovered != null ? hovered : pos1;
            drawBox(stack, settings.colorSelection.value, new AABB(
                    Math.min(pos1.x, to.x), Math.min(pos1.y, to.y), Math.min(pos1.z, to.z),
                    Math.max(pos1.x, to.x) + 1, Math.max(pos1.y, to.y) + 1, Math.max(pos1.z, to.z) + 1
            ));
            drawBox(stack, settings.colorSelectionPos1.value, new AABB(pos1));
            if (hovered != null) {
                drawBox(stack, settings.colorSelectionPos2.value, new AABB(hovered));
            }
        } else if (hovered != null) {
            drawBox(stack, settings.colorSelectionPos1.value, new AABB(hovered));
        }
    }

    private static void drawBox(PoseStack stack, Color color, AABB box) {
        Settings settings = Baritone.settings();
        boolean ignoreDepth = settings.renderSelectionIgnoreDepth.value;

        BufferBuilder bufferBuilder = IRenderer.startLines(color, settings.selectionOpacity.value, settings.selectionLineWidth.value, ignoreDepth);
        IRenderer.emitAABB(bufferBuilder, stack, box);
        IRenderer.endLines(bufferBuilder, ignoreDepth);
    }

    private BetterBlockPos raycast(float partialTicks, boolean shouldOffset) {
        Entity camera = ctx.minecraft().getCameraEntity();
        if (camera == null) {
            return null;
        }
        Vec3 start = camera.getEyePosition(partialTicks);
        Vec3 end = start.add(camera.getViewVector(partialTicks).scale(ctx.playerController().getBlockReachDistance()));
        BlockHitResult result = ctx.world().clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, camera));

        if (shouldOffset && result.getType() == HitResult.Type.BLOCK)
            return BetterBlockPos.from(result.getBlockPos().relative(result.getDirection()));
        return BetterBlockPos.from(result.getBlockPos());
    }
}
