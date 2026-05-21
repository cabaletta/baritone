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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class HUDBehavior extends Behavior {

    private static final int MAX_THREATS_SHOWN = 5;

    private final boolean[] prevKey = new boolean[4];
    private final AwarenessContext awarenessContext;

    public HUDBehavior(Baritone baritone) {
        super(baritone);
        this.awarenessContext = baritone.getAwarenessContext();
    }

    @Override
    public void onTick(TickEvent event) {
        if (event.getType() != TickEvent.Type.IN || ctx.player() == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) return;

        long win = mc.getWindow().getWindowHandle();
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

    @Override
    public void onRenderPass(RenderEvent event) {
        CustomConfig cfg = CustomConfig.get();
        if (!cfg.hudEnabled) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.options.renderDebug) return;

        String[] lines = buildHudLines();
        if (lines.length == 0) return;

        GuiGraphics gg = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
        Font font = mc.font;
        int x  = cfg.hudX;
        int y  = cfg.hudY;
        int lh = font.lineHeight + 2;
        for (String line : lines) {
            if (!line.isEmpty()) {
                gg.drawString(font, line, x, y, 0xFF000000 | cfg.hudColor, true);
            }
            y += lh;
        }
        mc.renderBuffers().bufferSource().endBatch();
    }

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
        float danger              = awarenessContext.getOverallDangerLevel();
        SelfState self            = awarenessContext.getSelf();
        TerrainSnapshot terr      = awarenessContext.getTerrain();
        ActionMode mode           = awarenessContext.getIntent().mode;
        List<ThreatEntry> threats = awarenessContext.getThreats();

        if (!lines.isEmpty()) lines.add("");

        if (danger < 0.05f) {
            lines.add("§a● §7Awareness: §aSafe");
        } else {
            String dc = danger > 0.6f ? "§c" : danger > 0.3f ? "§e" : "§a";
            lines.add(dc + "● §7Danger: "
                + buildBar(danger, 10, dc)
                + " " + dc + String.format("%.2f", danger)
                + " §7→ " + modeColor(mode) + mode.name()
                + (threats.size() > 1 ? " §8(" + threats.size() + " threats)" : ""));
        }

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

        if (!threats.isEmpty()) {
            lines.add("§7Threats §8(" + threats.size() + " total):");
            int shown = Math.min(threats.size(), MAX_THREATS_SHOWN);
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
    }

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
