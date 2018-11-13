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

    private static ThreadPoolExecutor threadPool;
    private static File dir;

    static {
        threadPool = new ThreadPoolExecutor(4, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<>());

        dir = new File(Minecraft.getMinecraft().gameDir, "baritone");
        if (!Files.exists(dir.toPath())) {
            try {
                Files.createDirectories(dir.toPath());
            } catch (IOException ignored) {}
        }
    }

    /**
     * Whether or not {@link Baritone#init()} has been called yet
     */
    private boolean initialized;

    private GameEventHandler gameEventHandler;

    private List<Behavior> behaviors;
    private PathingBehavior pathingBehavior;
    private LookBehavior lookBehavior;
    private MemoryBehavior memoryBehavior;
    private InputOverrideHandler inputOverrideHandler;

    private FollowProcess followProcess;
    private MineProcess mineProcess;
    private GetToBlockProcess getToBlockProcess;
    private CustomGoalProcess customGoalProcess;

    private PathingControlManager pathingControlManager;

    private WorldProvider worldProvider;

    Baritone() {
        this.gameEventHandler = new GameEventHandler(this);
    }

    public synchronized void init() {
        if (initialized) {
            return;
        }

        this.behaviors = new ArrayList<>();
        {
            // the Behavior constructor calls baritone.registerBehavior(this) so this populates the behaviors arraylist
            pathingBehavior = new PathingBehavior(this);
            lookBehavior = new LookBehavior(this);
            memoryBehavior = new MemoryBehavior(this);
            inputOverrideHandler = new InputOverrideHandler(this);
            new ExampleBaritoneControl(this);
        }

        this.pathingControlManager = new PathingControlManager(this);
        {
            followProcess = new FollowProcess(this);
            mineProcess = new MineProcess(this);
            customGoalProcess = new CustomGoalProcess(this); // very high iq
            getToBlockProcess = new GetToBlockProcess(this);
        }

        this.worldProvider = new WorldProvider();

        if (BaritoneAutoTest.ENABLE_AUTO_TEST) {
            registerEventListener(BaritoneAutoTest.INSTANCE);
        }

        this.initialized = true;
    }

    public PathingControlManager getPathingControlManager() {
        return pathingControlManager;
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

    public void registerBehavior(Behavior behavior) {
        this.behaviors.add(behavior);
        this.registerEventListener(behavior);
    }

    @Override
    public CustomGoalProcess getCustomGoalProcess() { // Iffy
        return customGoalProcess;
    }

    @Override
    public GetToBlockProcess getGetToBlockProcess() {  // Iffy
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
        return worldProvider;
    }

    @Override
    public WorldScanner getWorldScanner() {
        return WorldScanner.INSTANCE;
    }

    @Override
    public void registerEventListener(IGameEventListener listener) {
        this.gameEventHandler.registerEventListener(listener);
    }

    public static Settings settings() {
        return BaritoneAPI.getSettings();
    }

    public static File getDir() {
        return dir;
    }

    public static Executor getExecutor() {
        return threadPool;
    }
}
