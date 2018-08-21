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

package baritone.bot.event.events;

import baritone.bot.event.listener.IGameEventListener;

/**
 * Called in some cases where a player's inventory has it's current slot queried.
 * <p>
 * @see IGameEventListener#onQueryItemSlotForBlocks()
 *
 * @author Brady
 * @since 8/20/2018
 */
public final class ItemSlotEvent {

    /**
     * The current slot index
     */
    private int slot;

    public ItemSlotEvent(int slot) {
        this.slot = slot;
    }

    /**
     * Sets the new slot index that will be used
     *
     * @param slot The slot index
     */
    public final void setSlot(int slot) {
        this.slot = slot;
    }

    /**
     * @return The current slot index
     */
    public final int getSlot() {
        return this.slot;
    }
}
