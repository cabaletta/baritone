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

package baritone;

import baritone.api.BaritoneAPI;
import baritone.behavior.Behavior;
import baritone.api.event.listener.IGameEventListener;
import baritone.behavior.*;
import baritone.event.GameEventHandler;
import baritone.utils.InputOverrideHandler;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

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
    private ThreadPoolExecutor threadPool;


    /**
     * List of consumers to be called after Baritone has initialized
     */
    private List<Consumer<Baritone>> onInitConsumers;

    /**
     * Whether or not Baritone is active
     */
    private boolean active;

    Baritone() {
        this.onInitConsumers = new ArrayList<>();
    }

    public synchronized void init() {
        if (initialized) {
            return;
        }
        this.threadPool = new ThreadPoolExecutor(4, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<>());
        this.gameEventHandler = new GameEventHandler();
        this.inputOverrideHandler = new InputOverrideHandler();
        this.settings = new Settings();
        this.behaviors = new ArrayList<>();
        {
            registerBehavior(PathingBehavior.INSTANCE);
            registerBehavior(LookBehavior.INSTANCE);
            registerBehavior(MemoryBehavior.INSTANCE);
            registerBehavior(LocationTrackingBehavior.INSTANCE);
            registerBehavior(FollowBehavior.INSTANCE);
            registerBehavior(MineBehavior.INSTANCE);

            // TODO: Clean this up
            // Maybe combine this call in someway with the registerBehavior calls?
            BaritoneAPI.registerDefaultBehaviors(
                    FollowBehavior.INSTANCE,
                    LookBehavior.INSTANCE,
                    MemoryBehavior.INSTANCE,
                    MineBehavior.INSTANCE,
                    PathingBehavior.INSTANCE
            );
        }
        this.dir = new File(Minecraft.getMinecraft().gameDir, "baritone");
        if (!Files.exists(dir.toPath())) {
            try {
                Files.createDirectories(dir.toPath());
            } catch (IOException ignored) {}
        }

        this.active = true;
        this.initialized = true;

        this.onInitConsumers.forEach(consumer -> consumer.accept(this));
    }

    public boolean isInitialized() {
        return this.initialized;
    }

    public IGameEventListener getGameEventHandler() {
        return this.gameEventHandler;
    }

    public InputOverrideHandler getInputOverrideHandler() {
        return this.inputOverrideHandler;
    }

    public List<Behavior> getBehaviors() {
        return this.behaviors;
    }

    public Executor getExecutor() {
        return threadPool;
    }

    public void registerBehavior(Behavior behavior) {
        this.behaviors.add(behavior);
        this.registerEventListener(behavior);
    }

    public void registerEventListener(IGameEventListener listener) {
        this.gameEventHandler.registerEventListener(listener);
    }

    public boolean isActive() {
        return this.active;
    }

    public Settings getSettings() {
        return this.settings;
    }

    public static Settings settings() {
        return Baritone.INSTANCE.settings; // yolo
    }

    public File getDir() {
        return this.dir;
    }

    public void registerInitListener(Consumer<Baritone> runnable) {
        this.onInitConsumers.add(runnable);
    }
}
