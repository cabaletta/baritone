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
import baritone.api.utils.BetterBlockPos;
import baritone.awareness.AwarenessContext;
import baritone.awareness.model.ActionMode;
import baritone.awareness.model.EntityCategory;
import baritone.awareness.model.SelfState;
import baritone.awareness.model.TerrainSnapshot;
import baritone.awareness.model.ThreatEntry;
import baritone.pathing.path.PathExecutor;
import baritone.utils.CustomConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Registered as a Baritone behavior with two responsibilities:
 *
 * <ol>
 *   <li><b>Hotkey handler (onTick)</b> — polls GLFW on every client tick for
 *       the four rebindable keys defined in {@link CustomConfig}:
 *       cancel pathing, toggle path rendering, force-recompute, toggle HUD.
 *       Uses rising-edge detection so a held key fires only once.</li>
 *   <li><b>HUD overlay (onRenderPass)</b> — temporarily switches the OpenGL
 *       projection to orthographic and draws two panels:
 *       <ul>
 *         <li>Path panel — step index / total, distance to next node.</li>
 *         <li>Awareness panel — danger bar, action mode, self state (HP /
 *             armour / totems / pearls), terrain warnings, and the top-3
 *             scored threats read from {@link AwarenessContext}.</li>
 *       </ul>
 *   </li>
 * </ol>
 *
 * <p>Note on MC version: {@code RenderSystem.setProjectionMatrix} takes just a
 * {@code Matrix4f} in 1.19.4. In 1.20+ you must add a {@code VertexSorting}
 * second argument ({@code ORTHOGRAPHIC_Z} / {@code DISTANCE_TO_ORIGIN}).
 */
public final class HUDBehavior extends Behavior {

    private final boolean[] prevKey = new boolean[4]; // cancel, render, recompute, hud
    private final AwarenessContext awarenessContext;

    public HUDBehavior(Baritone baritone) {
        super(baritone);
        this.awarenessContext = baritone.getAwarenessContext();
    }

    // ── Hotkey handler ───────────────────────────────────────────────────────────────────

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
            log("Baritone cancelled.");
        }
        if (k1 && !prevKey[1]) {
            boolean on = !BaritoneAPI.getSettings().renderPath.value;
            BaritoneAPI.getSettings().renderPath.value = on;
            log("Path rendering " + (on ? "on" : "off") + ".");
        }
        if (k2 && !prevKey[2]) {
            baritone.getPathingBehavior().cancelEverything();
            log("Path recompute triggered.");
        }
        if (k3 && !prevKey[3]) {
            cfg.hudEnabled = !cfg.hudEnabled;
            CustomConfig.save();
            log("HUD " + (cfg.hudEnabled ? "on" : "off") + ".");
        }

        prevKey[0] = k0;
        prevKey[1] = k1;
        prevKey[2] = k2;
        prevKey[3] = k3;
    }

    // ── 2-D HUD overlay ────────────────────────────────────────────────────────────────────

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

        PoseStack ps = new PoseStack();
        ps.translate(0.0, 0.0, 50.0); // z-offset above world geometry

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        Font font = mc.font;
        int x  = cfg.hudX;
        int y  = cfg.hudY;
        int lh = font.lineHeight + 2;
        for (String line : lines) {
            if (!line.isEmpty()) {
                font.drawShadow(ps, line, (float) x, (float) y, 0xFF000000 | cfg.hudColor);
            }
            y += lh;
        }

        RenderSystem.disableBlend();

        // Restore 3-D projection.
        // In MC 1.20+ add second arg: VertexSorting.DISTANCE_TO_ORIGIN
        RenderSystem.setProjectionMatrix(savedProj);
        mv.popPose();
        RenderSystem.applyModelViewMatrix();
    }

    // ── Line builders ────────────────────────────────────────────────────────────────────

    private String[] buildHudLines() {
        List<String> lines = new ArrayList<>();
        buildPathLines(lines);
        buildAwarenessLines(lines);
        return lines.toArray(new String[0]);
    }

    private void buildPathLines(List<String> lines) {
        PathExecutor current = baritone.getPathingBehavior().getCurrent();
        if (current == null || current.getPath() == null) return;

        int pos       = current.getPosition();
        int total     = current.getPath().positions().size();
        int remaining = Math.max(0, total - pos);

        String distStr = "—";
        if (ctx.player() != null && pos + 1 < total) {
            Vec3 playerPos = ctx.player().position();
            BetterBlockPos nextNode = current.getPath().positions().get(pos + 1);
            double dx = nextNode.getX() + 0.5 - playerPos.x;
            double dz = nextNode.getZ() + 0.5 - playerPos.z;
            distStr = String.format("%.1f", Math.sqrt(dx * dx + dz * dz));
        }

        lines.add("§aBaritone §7| §fStep §e" + pos
            + " §7/ §e" + total
            + " §7(§f" + remaining + " §7left)");
        lines.add("§7Next node dist: §f" + distStr + "§7m");
    }

    private void buildAwarenessLines(List<String> lines) {
        float danger         = awarenessContext.getOverallDangerLevel();
        SelfState self       = awarenessContext.getSelf();
        TerrainSnapshot terr = awarenessContext.getTerrain();
        ActionMode mode      = awarenessContext.getIntent().mode;
        List<ThreatEntry> threats = awarenessContext.getThreats();

        if (!lines.isEmpty()) lines.add(""); // blank separator

        // — Line 1: danger bar + action mode ——————————————————————————————
        if (danger < 0.05f) {
            lines.add("§a● §7Awareness: §aSafe");
        } else {
            String dangerColor = danger > 0.6f ? "§c" : danger > 0.3f ? "§e" : "§a";
            lines.add(dangerColor + "● §7Danger: "
                + buildBar(danger, 10, dangerColor)
                + " " + dangerColor + String.format("%.2f", danger)
                + " §7→ " + modeColor(mode) + mode.name());
        }

        // — Line 2: self state —————————————————————————————————————————
        if (self.maxHealth > 0) {
            String hpColor  = self.health < 8f  ? "§c"
                            : self.health < 14f ? "§e" : "§a";
            String armColor = self.totalArmorDurability < 0.3f ? "§c"
                            : self.totalArmorDurability < 0.6f ? "§e" : "§f";
            StringBuilder selfLine = new StringBuilder("§7HP ");
            selfLine.append(hpColor).append(String.format("%.1f", self.health));
            selfLine.append("§7/§f").append(String.format("%.0f", self.maxHealth));
            selfLine.append("  §7Arm ").append(armColor);
            selfLine.append(self.totalArmorDurability > 0
                ? String.format("%.0f%%", self.totalArmorDurability * 100)
                : "§8none");
            if (self.hasTotem) selfLine.append("  §7Tot §e").append(self.totemCount);
            if (self.hasPearl) selfLine.append("  §7Pearl §b").append(self.pearlCount);
            lines.add(selfLine.toString());
        }

        // — Line 3 (optional): terrain warnings ——————————————————————————
        if (terr.inBlastZone || terr.fallDanger
                || terr.nearestHazardType != TerrainSnapshot.HazardType.NONE) {
            StringBuilder warn = new StringBuilder("§7Terrain:");
            if (terr.inBlastZone) warn.append(" §c[BLAST]");
            if (terr.fallDanger)  warn.append(" §e[FALL]");
            if (terr.nearestHazardType == TerrainSnapshot.HazardType.LAVA)
                warn.append(String.format(" §6[LAVA %.1fm]", terr.nearestHazardDistance));
            else if (terr.nearestHazardType == TerrainSnapshot.HazardType.WATER)
                warn.append(" §9[WATER]");
            lines.add(warn.toString());
        }

        // — Lines 4+: top-3 threat list ———————————————————————————————
        int shown = Math.min(threats.size(), 3);
        for (int i = 0; i < shown; i++) {
            ThreatEntry t   = threats.get(i);
            String prefix   = i == 0 ? "§c▸ " : "§7▸ ";
            String catColor = categoryColor(t.tracked.category);
            String name     = t.tracked.entity.getName().getString();
            String losTag   = t.tracked.hasLineOfSight ? " §a[LOS]" : "";
            lines.add(prefix + catColor + name
                + " §7" + String.format("%.1f", t.tracked.distance) + "m"
                + losTag
                + " §8[" + String.format("%.2f", t.score) + "]");
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────────────

    /** Sends a grey-prefixed message to the local player's chat. */
    private void log(String msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.sendSystemMessage(
                Component.literal("§8[Baritone] §7" + msg));
        }
    }

    private static String buildBar(float fraction, int width, String activeColor) {
        int filled = Math.round(fraction * width);
        StringBuilder sb = new StringBuilder(activeColor);
        for (int i = 0; i < width; i++) {
            if (i == filled) sb.append("§8");
            sb.append(i < filled ? '▊' : '░');
        }
        return sb.toString();
    }

    private static String modeColor(ActionMode mode) {
        switch (mode) {
            case ATTACK:     return "§c";
            case RETREAT:    return "§6";
            case REPOSITION: return "§e";
            case HEAL:       return "§a";
            case SHIELD:     return "§b";
            case ESCAPE:     return "§d";
            case DEFEND:     return "§9";
            default:         return "§7";
        }
    }

    private static String categoryColor(EntityCategory category) {
        switch (category) {
            case ENEMY_PLAYER: return "§c";
            case EXPLOSIVE:    return "§4";
            case PROJECTILE:   return "§6";
            case HOSTILE_MOB:  return "§e";
            default:           return "§f";
        }
    }

    private static boolean isDown(long win, int key) {
        return GLFW.glfwGetKey(win, key) == GLFW.GLFW_PRESS;
    }
}
