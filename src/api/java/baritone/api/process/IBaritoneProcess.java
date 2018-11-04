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
    // javadocs small brain, // comment large brain

    boolean isActive(); // would you like to be in control?

    PathingCommand onTick(); // you're in control, what should baritone do?

    boolean isTemporary(); // CombatPauserProcess should return isTemporary true always, and isActive true only when something is in range

    void onLostControl(); // called if isActive returned true, but another non-temporary process has control. effectively the same as cancel.

    double priority(); // tenor be like

    IBaritone associatedWith(); // which bot is this associated with (5000000iq forward thinking)

    String displayName();
}
