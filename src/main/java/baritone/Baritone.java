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
import baritone.api.event.listener.IEventBus;
import baritone.api.utils.ExampleBaritoneControl;
import baritone.api.utils.Helper;
import baritone.api.utils.IPlayerContext;
import baritone.behavior.*;
import baritone.cache.WorldProvider;
import baritone.event.GameEventHandler;
import baritone.process.*;
import baritone.utils.*;
import baritone.utils.player.PrimaryPlayerContext;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.Executor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Brady
 * @since 7/31/2018
 */
public class Baritone implements IBaritone {

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

    private PathingBehavior pathingBehavior;
    private LookBehavior lookBehavior;
    private MemoryBehavior memoryBehavior;
    private InventoryBehavior inventoryBehavior;
    private InputOverrideHandler inputOverrideHandler;

    private FollowProcess followProcess;
    private MineProcess mineProcess;
    private GetToBlockProcess getToBlockProcess;
    private CustomGoalProcess customGoalProcess;
    private BuilderProcess builderProcess;
    private ExploreProcess exploreProcess;
    private BackfillProcess backfillProcess;
    private FarmProcess farmProcess;
    private ChestSortProcess sortProcess;

    private PathingControlManager pathingControlManager;

    private IPlayerContext playerContext;
    private WorldProvider worldProvider;

    public BlockStateInterface bsi;

    Baritone() {
        this.gameEventHandler = new GameEventHandler(this);
    }

    @Override
    public synchronized void init() {
        if (initialized) {
            return;
        }

        // Define this before behaviors try and get it, or else it will be null and the builds will fail!
        this.playerContext = PrimaryPlayerContext.INSTANCE;

        {
            // the Behavior constructor calls baritone.registerBehavior(this) so this populates the behaviors arraylist
            pathingBehavior = new PathingBehavior(this);
            lookBehavior = new LookBehavior(this);
            memoryBehavior = new MemoryBehavior(this);
            inventoryBehavior = new InventoryBehavior(this);
            inputOverrideHandler = new InputOverrideHandler(this);
            new ExampleBaritoneControl(this);
        }

        this.pathingControlManager = new PathingControlManager(this);
        {
            followProcess = new FollowProcess(this);
            mineProcess = new MineProcess(this);
            customGoalProcess = new CustomGoalProcess(this); // very high iq
            getToBlockProcess = new GetToBlockProcess(this);
            builderProcess = new BuilderProcess(this);
            exploreProcess = new ExploreProcess(this);
            backfillProcess = new BackfillProcess(this);
            farmProcess = new FarmProcess(this);
            sortProcess = new ChestSortProcess(this); this.getGameEventHandler().registerEventListener(sortProcess);
        }

        this.worldProvider = new WorldProvider();

        if (BaritoneAutoTest.ENABLE_AUTO_TEST) {
            this.gameEventHandler.registerEventListener(BaritoneAutoTest.INSTANCE);
        }

        this.initialized = true;
    }

    @Override
    public PathingControlManager getPathingControlManager() {
        return this.pathingControlManager;
    }

    public void registerBehavior(Behavior behavior) {
        this.gameEventHandler.registerEventListener(behavior);
    }

    @Override
    public InputOverrideHandler getInputOverrideHandler() {
        return this.inputOverrideHandler;
    }

    @Override
    public CustomGoalProcess getCustomGoalProcess() { // Iffy
        return this.customGoalProcess;
    }

    @Override
    public GetToBlockProcess getGetToBlockProcess() {  // Iffy
        return this.getToBlockProcess;
    }

    @Override
    public IPlayerContext getPlayerContext() {
        return this.playerContext;
    }

    public MemoryBehavior getMemoryBehavior() {
        return this.memoryBehavior;
    }

    @Override
    public FollowProcess getFollowProcess() {
        return this.followProcess;
    }

    @Override
    public BuilderProcess getBuilderProcess() {
        return this.builderProcess;
    }

    public InventoryBehavior getInventoryBehavior() {
        return this.inventoryBehavior;
    }

    @Override
    public LookBehavior getLookBehavior() {
        return this.lookBehavior;
    }

    public ExploreProcess getExploreProcess() {
        return this.exploreProcess;
    }

    @Override
    public MineProcess getMineProcess() {
        return this.mineProcess;
    }

    public FarmProcess getFarmProcess() {
        return this.farmProcess;
    }

    @Override
    public ChestSortProcess getChestSortProcess() {
        return this.sortProcess;
    }

    @Override
    public PathingBehavior getPathingBehavior() {
        return this.pathingBehavior;
    }

    @Override
    public WorldProvider getWorldProvider() {
        return this.worldProvider;
    }

    @Override
    public IEventBus getGameEventHandler() {
        return this.gameEventHandler;
    }

    @Override
    public void openClick() {
        new Thread(() -> {
            try {
                Thread.sleep(100);
                Helper.mc.addScheduledTask(() -> Helper.mc.displayGuiScreen(new GuiClick()));
            } catch (Exception ignored) {}
        }).start();
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