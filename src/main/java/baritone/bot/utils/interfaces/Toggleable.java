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

package baritone.bot.utils.interfaces;

/**
 * @author Brady
 * @since 8/20/2018
 */
public interface Toggleable {

    /**
     * Toggles the enabled state of this {@link Toggleable}.
     *
     * @return The new state.
     */
    boolean toggle();

    /**
     * Sets the enabled state of this {@link Toggleable}.
     *
     * @return The new state.
     */
    boolean setEnabled(boolean enabled);

    /**
     * @return Whether or not this {@link Toggleable} object is enabled
     */
    boolean isEnabled();
}
