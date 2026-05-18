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
import baritone.api.BaritoneAPI;
import baritone.api.event.events.RenderEvent;
import baritone.api.event.events.TickEvent;
import baritone.pathing.path.PathExecutor;
import baritone.utils.CustomConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

/**
 * Registered as a Baritone behavior with two responsibilities:
 *
 * <ol>
 *   <li><b>Hotkey handler (onTick)</b> — polls GLFW on every client tick for
 *       the four rebindable keys defined in {@link CustomConfig}:
 *       <ul>
 *         <li>Cancel pathing</li>
 *         <li>Toggle path rendering</li>
 *         <li>Force-recompute path</li>
 *         <li>Toggle this HUD</li>
 *       </ul>
 *       Uses rising-edge detection so a held key fires only once.</li>
 *   <li><b>HUD overlay (onRenderPass)</b> — temporarily switches the OpenGL
 *       projection to orthographic, draws a compact status panel (step index /
 *       total, distance to next node) via {@code Font.drawShadow}, then
 *       restores the original 3-D projection.</li>
 * </ol>
 *
 * <p>Note on MC version: {@code RenderSystem.setProjectionMatrix} takes just a
 * {@code Matrix4f} in 1.19.4.  If you later target 1.20+ you must add a
 * {@code VertexSorting} second argument
 * ({@code ORTHOGRAPHIC_Z} / {@code DISTANCE_TO_ORIGIN}).
 */
public final class HUDBehavior extends Behavior {

    private final boolean[] prevKey = new boolean[4]; // cancel, render, recompute, hud

    public HUDBehavior(Baritone baritone) {
        super(baritone);
    }

    // ── Hotkey handler ────────────────────────────────────────────────────────

    @Override
    public void onTick(TickEvent event) {
        if (event.getType() != TickEvent.Type.IN || ctx.player() == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) return; // suppress hotkeys while a screen is open

        long win = mc.getWindow().getWindow();
        CustomConfig cfg = CustomConfig.get();

        boolean k0 = isDown(win, cfg.keyCancel);
        boolean k1 = isDown(win, cfg.keyToggleRender);
        boolean k2 = isDown(win, cfg.keyRecompute);
        boolean k3 = isDown(win, cfg.keyToggleHud);

        if (k0 && !prevKey[0]) {
            baritone.getPathingBehavior().cancelEverything();
            logDirect("Baritone cancelled (hotkey).");
        }
        if (k1 && !prevKey[1]) {
            boolean on = !BaritoneAPI.getSettings().renderPath.value;
            BaritoneAPI.getSettings().renderPath.value = on;
            logDirect("Path rendering " + (on ? "on" : "off") + " (hotkey).");
        }
        if (k2 && !prevKey[2]) {
            // Cancelling the current executor forces a fresh A* calculation;
            // the active process (follow/mine/…) will re-request pathing.
            baritone.getPathingBehavior().cancelEverything();
            logDirect("Path recompute triggered (hotkey).");
        }
        if (k3 && !prevKey[3]) {
            cfg.hudEnabled = !cfg.hudEnabled;
            CustomConfig.save();
            logDirect("HUD " + (cfg.hudEnabled ? "on" : "off") + " (hotkey).");
        }

        prevKey[0] = k0;
        prevKey[1] = k1;
        prevKey[2] = k2;
        prevKey[3] = k3;
    }

    // ── 2-D HUD overlay ───────────────────────────────────────────────────────

    @Override
    public void onRenderPass(RenderEvent event) {
        CustomConfig cfg = CustomConfig.get();
        if (!cfg.hudEnabled) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.options.renderDebug) return; // hide while F3 screen is open

        String[] lines = buildHudLines();
        if (lines.length == 0) return;

        // Save the 3-D projection so we can restore it after drawing.
        Matrix4f savedProj = new Matrix4f(event.getProjectionMatrix());

        // Reset modelview to identity (screen space).
        PoseStack mv = RenderSystem.getModelViewStack();
        mv.pushPose();
        mv.setIdentity();
        RenderSystem.applyModelViewMatrix();

        // Orthographic projection matching Minecraft's GUI coordinate system.
        // In MC 1.20+ add second arg: VertexSorting.ORTHOGRAPHIC_Z
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        Matrix4f ortho = new Matrix4f().ortho(0.0F, (float) w, (float) h, 0.0F, -1000.0F, 1000.0F);
        RenderSystem.setProjectionMatrix(ortho);

        // Draw text.
        PoseStack ps = new PoseStack();
        ps.translate(0.0, 0.0, 50.0); // z-offset above world geometry

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        Font font = mc.font;
        int x  = cfg.hudX;
        int y  = cfg.hudY;
        int lh = font.lineHeight + 2;
        for (String line : lines) {
            font.drawShadow(ps, line, (float) x, (float) y, 0xFF000000 | cfg.hudColor);
            y += lh;
        }

        RenderSystem.disableBlend();

        // Restore 3-D projection.
        // In MC 1.20+ add second arg: VertexSorting.DISTANCE_TO_ORIGIN
        RenderSystem.setProjectionMatrix(savedProj);
        mv.popPose();
        RenderSystem.applyModelViewMatrix();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String[] buildHudLines() {
        PathExecutor current = baritone.getPathingBehavior().getCurrent();
        if (current == null || current.getPath() == null) return new String[0];

        int pos       = current.getPosition();
        int total     = current.getPath().positions().size();
        int remaining = Math.max(0, total - pos);

        String distStr = "—"; // em-dash placeholder
        if (ctx.player() != null && pos + 1 < total) {
            Vec3 playerPos = ctx.player().position();
            var  nextNode  = current.getPath().positions().get(pos + 1);
            double dx = nextNode.getX() + 0.5 - playerPos.x;
            double dz = nextNode.getZ() + 0.5 - playerPos.z;
            distStr = String.format("%.1f", Math.sqrt(dx * dx + dz * dz));
        }

        return new String[]{
            "§aBaritone §7| §fStep §e" + pos
                + " §7/ §e" + total
                + " §7(§f" + remaining + " §7left)",
            "§7Next node dist: §f" + distStr + "§7m"
        };
    }

    private static boolean isDown(long win, int key) {
        return GLFW.glfwGetKey(win, key) == GLFW.GLFW_PRESS;
    }
}
