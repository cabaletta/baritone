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

package baritone.bot;

import baritone.bot.behavior.Behavior;
import baritone.bot.behavior.impl.LookBehavior;
import baritone.bot.behavior.impl.MemoryBehavior;
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

    private GameEventHandler gameEventHandler;
    private InputOverrideHandler inputOverrideHandler;
    private List<Behavior> behaviors;

    /**
     * Whether or not Baritone is active
     */
    private boolean active;

    public void init() {
        this.gameEventHandler = new GameEventHandler();
        this.inputOverrideHandler = new InputOverrideHandler();
        this.behaviors = new ArrayList<>();
        behaviors.add(PathingBehavior.INSTANCE);
        behaviors.add(LookBehavior.INSTANCE);
        behaviors.add(MemoryBehavior.INSTANCE);

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

    public final boolean isActive() {
        return this.active;
    }
}
