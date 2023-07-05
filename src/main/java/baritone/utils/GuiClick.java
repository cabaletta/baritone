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

package baritone.utils;

import baritone.Baritone;
import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.Helper;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector4f;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;

import java.awt.*;
import java.util.Collections;

import static baritone.api.command.IBaritoneChatControl.FORCE_COMMAND_PREFIX;
import static org.lwjgl.opengl.GL11.*;

public class GuiClick extends Screen implements Helper {

    private Matrix4f projectionViewMatrix;

    private BlockPos clickStart;
    private BlockPos currentMouseOver;

    public GuiClick() {
        super(new StringTextComponent("CLICK"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(MatrixStack stack, int mouseX, int mouseY, float partialTicks) {
        double mx = mc.mouseHelper.getMouseX();
        double my = mc.mouseHelper.getMouseY();

        my = mc.getMainWindow().getHeight() - my;
        my *= mc.getMainWindow().getFramebufferHeight() / (double) mc.getMainWindow().getHeight();
        mx *= mc.getMainWindow().getFramebufferWidth() / (double) mc.getMainWindow().getWidth();
        Vector3d near = toWorld(mx, my, 0);
        Vector3d far = toWorld(mx, my, 1); // "Use 0.945 that's what stack overflow says" - leijurv

        if (near != null && far != null) {
            ///
            Vector3d viewerPos = new Vector3d(PathRenderer.posX(), PathRenderer.posY(), PathRenderer.posZ());
            ClientPlayerEntity player = BaritoneAPI.getProvider().getPrimaryBaritone().getPlayerContext().player();
            RayTraceResult result = player.world.rayTraceBlocks(new RayTraceContext(near.add(viewerPos), far.add(viewerPos), RayTraceContext.BlockMode.OUTLINE, RayTraceContext.FluidMode.NONE, player));
            if (result != null && result.getType() == RayTraceResult.Type.BLOCK) {
                currentMouseOver = ((BlockRayTraceResult) result).getPos();
            }
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        if (currentMouseOver != null) { //Catch this, or else a click into void will result in a crash
            if (mouseButton == 0) {
                if (clickStart != null && !clickStart.equals(currentMouseOver)) {
                    BaritoneAPI.getProvider().getPrimaryBaritone().getSelectionManager().removeAllSelections();
                    BaritoneAPI.getProvider().getPrimaryBaritone().getSelectionManager().addSelection(BetterBlockPos.from(clickStart), BetterBlockPos.from(currentMouseOver));
                    TextComponent component = new StringTextComponent("Selection made! For usage: " + Baritone.settings().prefix.value + "help sel");
                    component.setStyle(component.getStyle()
                            .setFormatting(TextFormatting.WHITE)
                            .setClickEvent(new ClickEvent(
                                    ClickEvent.Action.RUN_COMMAND,
                                    FORCE_COMMAND_PREFIX + "help sel"
                            )));
                    Helper.HELPER.logDirect(component);
                    clickStart = null;
                } else {
                    BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(currentMouseOver));
                }
            } else if (mouseButton == 1) {
                BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(currentMouseOver.up()));
            }
        }
        clickStart = null;
        return super.mouseReleased(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        clickStart = currentMouseOver;
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    public void onRender(MatrixStack modelViewStack, Matrix4f projectionMatrix) {
        this.projectionViewMatrix = projectionMatrix.copy();
        this.projectionViewMatrix.mul(modelViewStack.getLast().getMatrix());
        this.projectionViewMatrix.invert();

        if (currentMouseOver != null) {
            Entity e = mc.getRenderViewEntity();
            // drawSingleSelectionBox WHEN?
            PathRenderer.drawManySelectionBoxes(modelViewStack, e, Collections.singletonList(currentMouseOver), Color.CYAN);
            if (clickStart != null && !clickStart.equals(currentMouseOver)) {
                IRenderer.startLines(Color.RED, Baritone.settings().pathRenderLineWidthPixels.value, true);
                BetterBlockPos a = new BetterBlockPos(currentMouseOver);
                BetterBlockPos b = new BetterBlockPos(clickStart);
                IRenderer.emitAABB(modelViewStack, new AxisAlignedBB(Math.min(a.x, b.x), Math.min(a.y, b.y), Math.min(a.z, b.z), Math.max(a.x, b.x) + 1, Math.max(a.y, b.y) + 1, Math.max(a.z, b.z) + 1));
                IRenderer.endLines(true);
            }
        }
    }

    private Vector3d toWorld(double x, double y, double z) {
        if (this.projectionViewMatrix == null) {
            return null;
        }

        x /= mc.getMainWindow().getFramebufferWidth();
        y /= mc.getMainWindow().getFramebufferHeight();
        x = x * 2 - 1;
        y = y * 2 - 1;

        Vector4f pos = new Vector4f((float) x, (float) y, (float) z, 1.0F);
        pos.transform(this.projectionViewMatrix);
        if (pos.getW() == 0) {
            return null;
        }

        pos.perspectiveDivide();
        return new Vector3d(pos.getX(), pos.getY(), pos.getZ());
    }
}
