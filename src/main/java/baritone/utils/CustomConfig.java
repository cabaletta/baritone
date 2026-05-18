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

import com.google.gson.*;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/**
 * Lightweight JSON-backed configuration for customisation settings that live
 * outside of Baritone's standard {@link baritone.api.Settings} system:
 * rebindable hotkeys and HUD display preferences.
 *
 * <p>Config file: {@code .minecraft/baritone/custom_config.json}<br>
 * Loaded once at startup via {@link baritone.Baritone} constructor;
 * saved whenever a hotkey changes {@link #hudEnabled} or when
 * {@link #save()} is called explicitly.
 */
public final class CustomConfig {

    // ── Singleton ────────────────────────────────────────────────────────────

    private static final CustomConfig INSTANCE = new CustomConfig();
    private static final Gson         GSON     = new GsonBuilder().setPrettyPrinting().create();

    private CustomConfig() {}

    public static CustomConfig get() { return INSTANCE; }

    // ── Keybind GLFW key-codes (edit custom_config.json to rebind) ────────────

    /** GLFW key code for "cancel pathing".         Default: K (75) */
    public int keyCancel       = GLFW.GLFW_KEY_K;

    /** GLFW key code for "toggle path rendering".  Default: R (82) */
    public int keyToggleRender = GLFW.GLFW_KEY_R;

    /** GLFW key code for "force-recompute path".   Default: N (78) */
    public int keyRecompute    = GLFW.GLFW_KEY_N;

    /** GLFW key code for "toggle HUD overlay".     Default: H (72) */
    public int keyToggleHud    = GLFW.GLFW_KEY_H;

    // ── HUD preferences ───────────────────────────────────────────────────────

    /** Whether the custom status HUD is visible. */
    public boolean hudEnabled  = true;

    /** Screen X of the HUD top-left corner (GUI-scaled pixels). */
    public int hudX            = 4;

    /** Screen Y of the HUD top-left corner (GUI-scaled pixels). */
    public int hudY            = 4;

    /** Base text colour (packed RGB, no alpha byte). */
    public int hudColor        = 0xFFFFFF;

    // ── Persistence ───────────────────────────────────────────────────────────

    public static Path configPath() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("baritone").resolve("custom_config.json");
    }

    /** Load from disk; writes defaults if the file does not yet exist. */
    public static void load() {
        Path path = configPath();
        if (!Files.exists(path)) {
            save();
            return;
        }
        try (Reader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
            CustomConfig c = INSTANCE;
            c.keyCancel       = readInt (o, "keyCancel",       c.keyCancel);
            c.keyToggleRender = readInt (o, "keyToggleRender", c.keyToggleRender);
            c.keyRecompute    = readInt (o, "keyRecompute",    c.keyRecompute);
            c.keyToggleHud    = readInt (o, "keyToggleHud",    c.keyToggleHud);
            c.hudEnabled      = readBool(o, "hudEnabled",      c.hudEnabled);
            c.hudX            = readInt (o, "hudX",            c.hudX);
            c.hudY            = readInt (o, "hudY",            c.hudY);
            c.hudColor        = readInt (o, "hudColor",        c.hudColor);
        } catch (Exception e) {
            System.err.println("[Baritone/CustomConfig] load failed: " + e.getMessage());
        }
    }

    /** Persist current values to disk. */
    public static void save() {
        Path path = configPath();
        try {
            Files.createDirectories(path.getParent());
            CustomConfig c = INSTANCE;
            JsonObject o = new JsonObject();
            o.addProperty("keyCancel",       c.keyCancel);
            o.addProperty("keyToggleRender", c.keyToggleRender);
            o.addProperty("keyRecompute",    c.keyRecompute);
            o.addProperty("keyToggleHud",    c.keyToggleHud);
            o.addProperty("hudEnabled",      c.hudEnabled);
            o.addProperty("hudX",            c.hudX);
            o.addProperty("hudY",            c.hudY);
            o.addProperty("hudColor",        c.hudColor);
            try (Writer w = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(o, w);
            }
        } catch (Exception e) {
            System.err.println("[Baritone/CustomConfig] save failed: " + e.getMessage());
        }
    }

    // ── JSON helpers ──────────────────────────────────────────────────────────

    private static int     readInt (JsonObject o, String k, int     d) { return o.has(k) ? o.get(k).getAsInt()     : d; }
    private static boolean readBool(JsonObject o, String k, boolean d) { return o.has(k) ? o.get(k).getAsBoolean() : d; }
}
