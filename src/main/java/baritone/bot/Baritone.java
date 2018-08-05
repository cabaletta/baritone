package baritone.bot;

import baritone.bot.behavior.Behavior;
import baritone.bot.behavior.impl.PathingBehavior;

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

    private Memory memory;
    private HookStateManager hookStateManager;
    private GameActionHandler actionHandler;
    private GameEventHandler gameEventHandler;
    private InputOverrideHandler inputOverrideHandler;
    private List<Behavior> behaviors;

    /**
     * Whether or not Baritone is active
     */
    private boolean active;

    public void init() {
        this.memory = new Memory();
        this.hookStateManager = new HookStateManager();
        this.actionHandler = new GameActionHandler();
        this.gameEventHandler = new GameEventHandler();
        this.inputOverrideHandler = new InputOverrideHandler();
        this.behaviors = new ArrayList<>();
        behaviors.add(PathingBehavior.INSTANCE);

        this.active = true;
        this.initialized = true;
    }

    public final boolean isInitialized() {
        return this.initialized;
    }

    public final Memory getMemory() {
        return this.memory;
    }

    public final HookStateManager getHookStateManager() {
        return this.hookStateManager;
    }

    public final GameActionHandler getActionHandler() {
        return this.actionHandler;
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

    public final boolean isActive() {
        return this.active;
    }
}
