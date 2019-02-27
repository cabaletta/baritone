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
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.Display;
import org.lwjgl.util.glu.GLU;

import java.awt.*;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;

public class GuiClickMeme extends GuiScreen {
    private final FloatBuffer MODELVIEW = BufferUtils.createFloatBuffer(16);
    private final FloatBuffer PROJECTION = BufferUtils.createFloatBuffer(16);
    private final IntBuffer VIEWPORT = BufferUtils.createIntBuffer(16);
    private final FloatBuffer TO_SCREEN_BUFFER = BufferUtils.createFloatBuffer(3);
    private final FloatBuffer TO_WORLD_BUFFER = BufferUtils.createFloatBuffer(3);
    private Vec3d meme;

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        System.out.println("Screen " + mouseX + " " + mouseY);
        System.out.println(toWorld(mouseX, mouseY, 0));
        System.out.println(toWorld(mouseX, mouseY, 0.1));
        System.out.println(toWorld(mouseX, mouseY, 1));
        System.out.println(VIEWPORT.get(3) + " " + Display.getHeight());
        meme = toWorld(mouseX, Display.getHeight() - mouseY, 0);
        System.out.println(toScreen(mc.player.posX + 1, mc.player.posY, mc.player.posZ));
        System.out.println(toScreen(1, 0, 0));
    }

    public void onRender(float partialTicks) {
        System.out.println("on render");
        GlStateManager.getFloat(GL_MODELVIEW_MATRIX, (FloatBuffer) MODELVIEW.clear());
        GlStateManager.getFloat(GL_PROJECTION_MATRIX, (FloatBuffer) PROJECTION.clear());
        GlStateManager.glGetInteger(GL_VIEWPORT, (IntBuffer) VIEWPORT.clear());
        if (meme != null) {
            Entity e = mc.getRenderViewEntity();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO);
            GlStateManager.color(Color.RED.getColorComponents(null)[0], Color.RED.getColorComponents(null)[1], Color.RED.getColorComponents(null)[2], 0.4F);
            GlStateManager.glLineWidth(Baritone.settings().pathRenderLineWidthPixels.get());
            GlStateManager.disableTexture2D();
            GlStateManager.depthMask(false);
            PathRenderer.drawLine(e, e.posX + 1, e.posY, e.posZ, e.posX + meme.x + 1, e.posY + meme.y, e.posZ + meme.z, partialTicks);
            Tessellator.getInstance().draw();
            GlStateManager.depthMask(true);
            GlStateManager.enableTexture2D();
            GlStateManager.disableBlend();
        }
    }

    public Vec3d toWorld(double x, double y, double z) {
        boolean result = GLU.gluUnProject((float) x, (float) y, (float) z, MODELVIEW, PROJECTION, VIEWPORT, (FloatBuffer) TO_WORLD_BUFFER.clear());
        if (result) {
            return new Vec3d(TO_WORLD_BUFFER.get(0), TO_WORLD_BUFFER.get(1), TO_WORLD_BUFFER.get(2));
        }
        return null;
    }

    public Vec3d toScreen(double x, double y, double z) {
        boolean result = GLU.gluProject((float) x, (float) y, (float) z, MODELVIEW, PROJECTION, VIEWPORT, (FloatBuffer) TO_SCREEN_BUFFER.clear());
        if (result) {
            return new Vec3d(TO_SCREEN_BUFFER.get(0), Display.getHeight() - TO_SCREEN_BUFFER.get(1), TO_SCREEN_BUFFER.get(2));
        }
        return null;
    }
}
