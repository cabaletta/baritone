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

package baritone;

import baritone.behavior.Behavior;
import baritone.behavior.impl.LookBehavior;
import baritone.behavior.impl.MemoryBehavior;
import baritone.behavior.impl.PathingBehavior;
import baritone.behavior.impl.LocationTrackingBehavior;
import baritone.event.GameEventHandler;
import baritone.map.Map;
import baritone.utils.InputOverrideHandler;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Brady
 * @since 7/31/2018 10:50 PM
 */
public enum Baritone {

    /**
     * Singleton instance of this class
     */
    INSTANCE;

    /**
     * Whether or not {@link Baritone#init()} has been called yet
     */
    private boolean initialized;

    private GameEventHandler gameEventHandler;
    private InputOverrideHandler inputOverrideHandler;
    private Settings settings;
    private List<Behavior> behaviors;
    private File dir;

    /**
     * Whether or not Baritone is active
     */
    private boolean active;

    public synchronized void init() {
        if (initialized) {
            return;
        }
        this.gameEventHandler = new GameEventHandler();
        this.inputOverrideHandler = new InputOverrideHandler();
        this.settings = new Settings();
        this.behaviors = new ArrayList<>();
        {
            registerBehavior(PathingBehavior.INSTANCE);
            registerBehavior(LookBehavior.INSTANCE);
            registerBehavior(MemoryBehavior.INSTANCE);
            registerBehavior(Map.INSTANCE);
            registerBehavior(LocationTrackingBehavior.INSTANCE);
        }
        
        this.dir = new File(Minecraft.getMinecraft().gameDir, "baritone");
        if (!Files.exists(dir.toPath())) {
            try {
                Files.createDirectories(dir.toPath());
            } catch (IOException ignored) {}
        }

        this.active = true;
        this.initialized = true;
    }

    public final boolean isInitialized() {
        return this.initialized;
    }

    public final GameEventHandler getGameEventHandler() {
        return this.gameEventHandler;
    }

    public final InputOverrideHandler getInputOverrideHandler() {
        return this.inputOverrideHandler;
    }

    public final List<Behavior> getBehaviors() {
        return this.behaviors;
    }

    public void registerBehavior(Behavior behavior) {
        this.behaviors.add(behavior);
        this.gameEventHandler.registerEventListener(behavior);
    }

    public final boolean isActive() {
        return this.active;
    }

    public final Settings getSettings() {
        return this.settings;
    }

    public static Settings settings() {
        return Baritone.INSTANCE.settings; // yolo
    }

    public final File getDir() {
        return this.dir;
    }
}
