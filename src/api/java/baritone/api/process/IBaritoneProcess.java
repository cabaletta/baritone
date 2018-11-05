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

package baritone.api.process;

import baritone.api.IBaritone;

/**
 * A process that can control the PathingBehavior.
 * <p>
 * Differences between a baritone process and a behavior:
 * Only one baritone process can be active at a time
 * PathingBehavior can only be controlled by a process
 * <p>
 * That's it actually
 *
 * @author leijurv
 */
public interface IBaritoneProcess {
    /**
     * Would this process like to be in control?
     *
     * @return
     */
    boolean isActive();

    /**
     * This process is in control of pathing. What should Baritone do?
     *
     * @param calcFailed     true if this specific process was in control last tick, and there was a CALC_FAILED event last tick
     * @param isSafeToCancel true if a REQUEST_PAUSE would happen this tick, and PathingBehavior wouldn't actually tick.
     *                       false if the PathExecutor reported pausing would be unsafe at the end of the last tick
     * @return what the PathingBehavior should do
     */
    PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel);

    /**
     * Is this control temporary?
     * If a process is temporary, it doesn't call onLostControl on the processes that aren't execute because of it
     * For example, CombatPauserProcess and PauseForAutoEatProcess should return isTemporary true always,
     * and should return isActive true only if there's something in range this tick, or if the player would like to start eating this tick.
     * PauseForAutoEatProcess should only actually right click once onTick is called with isSafeToCancel true though.
     *
     * @return
     */
    boolean isTemporary();

    /**
     * Called if isActive returned true, but another non-temporary process has control. Effectively the same as cancel.
     * You want control but you don't get it.
     */
    void onLostControl();

    /**
     * How to decide which Process gets control if they all report isActive? It's the one with the highest priority.
     *
     * @return
     */
    double priority();

    /**
     * which bot is this associated with (5000000iq forward thinking)
     *
     * @return
     */
    IBaritone associatedWith();

    /**
     * What this process should be displayed to the user as (maybe in a HUD? hint hint)
     *
     * @return
     */
    String displayName();
}
