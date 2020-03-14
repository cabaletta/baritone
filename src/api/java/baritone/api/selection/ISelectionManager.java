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

package baritone.api.selection;

import baritone.api.utils.BetterBlockPos;
import net.minecraft.util.EnumFacing;

/**
 * The selection manager handles setting Baritone's selections. You can set the selection here, as well as retrieving
 * the current selection.
 */
public interface ISelectionManager {

    /**
     * Adds a new selection. The added selection is returned.
     *
     * @param selection The new selection to add.
     */
    ISelection addSelection(ISelection selection);

    /**
     * Adds a new {@link ISelection} constructed from the given block positions. The new selection is returned.
     *
     * @param pos1 One corner of the selection
     * @param pos2 The new corner of the selection
     */
    ISelection addSelection(BetterBlockPos pos1, BetterBlockPos pos2);

    /**
     * Removes the selection from the current selections.
     *
     * @param selection The selection to remove.
     * @return The removed selection.
     */
    ISelection removeSelection(ISelection selection);

    /**
     * Removes all selections.
     *
     * @return The selections that were removed, sorted from oldest to newest..
     */
    ISelection[] removeAllSelections();

    /**
     * @return The current selections, sorted from oldest to newest.
     */
    ISelection[] getSelections();

    /**
     * For anything expecting only one selection, this method is provided. However, to enforce multi-selection support,
     * this method will only return a selection if there is ONLY one.
     *
     * @return The only selection, or null if there isn't only one.
     */
    ISelection getOnlySelection();

    /**
     * This method will always return the last selection. ONLY use this if you want to, for example, modify the most
     * recent selection based on user input. ALWAYS use {@link #getOnlySelection()} or, ideally,
     * {@link #getSelections()} for retrieving the content of selections.
     *
     * @return The last selection, or null if it doesn't exist.
     */
    ISelection getLastSelection();

    /**
     * Replaces the specified {@link ISelection} with one expanded in the specified direction by the specified number of
     * blocks. Returns the new selection.
     *
     * @param selection The selection to expand.
     * @param direction The direction to expand the selection.
     * @param blocks    How many blocks to expand it.
     * @return The new selection, expanded as specified.
     */
    ISelection expand(ISelection selection, EnumFacing direction, int blocks);

    /**
     * Replaces the specified {@link ISelection} with one contracted in the specified direction by the specified number
     * of blocks.
     * <p>
     * Note that, for example, if the direction specified is UP, the bottom of the selection will be shifted up. If it
     * is DOWN, the top of the selection will be shifted down.
     *
     * @param selection The selection to contract.
     * @param direction The direction to contract the selection.
     * @param blocks    How many blocks to contract it.
     * @return The new selection, contracted as specified.
     */
    ISelection contract(ISelection selection, EnumFacing direction, int blocks);

    /**
     * Replaces the specified {@link ISelection} with one shifted in the specified direction by the specified number of
     * blocks. This moves the whole selection.
     *
     * @param selection The selection to shift.
     * @param direction The direction to shift the selection.
     * @param blocks    How many blocks to shift it.
     * @return The new selection, shifted as specified.
     */
    ISelection shift(ISelection selection, EnumFacing direction, int blocks);
}
