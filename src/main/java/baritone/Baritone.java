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
import baritone.api.IBaritone;
import baritone.api.Settings;
import baritone.api.event.listener.IGameEventListener;
import baritone.behavior.Behavior;
import baritone.behavior.LookBehavior;
import baritone.behavior.MemoryBehavior;
import baritone.behavior.PathingBehavior;
import baritone.cache.WorldProvider;
import baritone.cache.WorldScanner;
import baritone.event.GameEventHandler;
import baritone.process.CustomGoalProcess;
import baritone.process.FollowProcess;
import baritone.process.GetToBlockProcess;
import baritone.process.MineProcess;
import baritone.utils.BaritoneAutoTest;
import baritone.utils.ExampleBaritoneControl;
import baritone.utils.InputOverrideHandler;
import baritone.utils.PathingControlManager;
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

/**
 * @author Brady
 * @since 7/31/2018 10:50 PM
 */
public enum Baritone implements IBaritone {

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
    private File dir;
    private ThreadPoolExecutor threadPool;

    private List<Behavior> behaviors;
    private PathingBehavior pathingBehavior;
    private LookBehavior lookBehavior;
    private MemoryBehavior memoryBehavior;

    private FollowProcess followProcess;
    private MineProcess mineProcess;
    private GetToBlockProcess getToBlockProcess;
    private CustomGoalProcess customGoalProcess;

    private PathingControlManager pathingControlManager;

    Baritone() {
        this.gameEventHandler = new GameEventHandler();
    }

    public synchronized void init() {
        if (initialized) {
            return;
        }
        this.threadPool = new ThreadPoolExecutor(4, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<>());
        this.inputOverrideHandler = new InputOverrideHandler();

        // Acquire the "singleton" instance of the settings directly from the API
        // We might want to change this...
        this.settings = BaritoneAPI.getSettings();

        this.pathingControlManager = new PathingControlManager(this);

        this.behaviors = new ArrayList<>();
        {
            // the Behavior constructor calls baritone.registerBehavior(this) so this populates the behaviors arraylist
            pathingBehavior = new PathingBehavior(this);
            lookBehavior = new LookBehavior(this);
            memoryBehavior = new MemoryBehavior(this);
            followProcess = new FollowProcess(this);
            mineProcess = new MineProcess(this);
            new ExampleBaritoneControl(this);
            customGoalProcess = new CustomGoalProcess(this); // very high iq
            getToBlockProcess = new GetToBlockProcess(this);
        }
        if (BaritoneAutoTest.ENABLE_AUTO_TEST) {
            registerEventListener(BaritoneAutoTest.INSTANCE);
        }
        this.dir = new File(Minecraft.getMinecraft().gameDir, "baritone");
        if (!Files.exists(dir.toPath())) {
            try {
                Files.createDirectories(dir.toPath());
            } catch (IOException ignored) {}
        }

        this.initialized = true;
    }

    public PathingControlManager getPathingControlManager() {
        return pathingControlManager;
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

    @Override
    public CustomGoalProcess getCustomGoalProcess() {
        return customGoalProcess;
    }

    @Override
    public GetToBlockProcess getGetToBlockProcess() { // very very high iq
        return getToBlockProcess;
    }

    @Override
    public FollowProcess getFollowProcess() {
        return followProcess;
    }

    @Override
    public LookBehavior getLookBehavior() {
        return lookBehavior;
    }

    @Override
    public MemoryBehavior getMemoryBehavior() {
        return memoryBehavior;
    }

    @Override
    public MineProcess getMineProcess() {
        return mineProcess;
    }

    @Override
    public PathingBehavior getPathingBehavior() {
        return pathingBehavior;
    }

    @Override
    public WorldProvider getWorldProvider() {
        return WorldProvider.INSTANCE;
    }

    @Override
    public WorldScanner getWorldScanner() {
        return WorldScanner.INSTANCE;
    }

    @Override
    public void registerEventListener(IGameEventListener listener) {
        this.gameEventHandler.registerEventListener(listener);
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
}
