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
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3i;

/**
 * A selection is an immutable object representing the current selection. The selection is commonly used for certain
 * types of build commands, however it can be used for anything.
 */
public interface ISelection {

    /**
     * @return The first corner of this selection. This is meant to preserve the user's original first corner.
     */
    BetterBlockPos pos1();

    /**
     * @return The second corner of this selection. This is meant to preserve the user's original second corner.
     */
    BetterBlockPos pos2();

    /**
     * @return The {@link BetterBlockPos} with the lowest x, y, and z position in the selection.
     */
    BetterBlockPos min();

    /**
     * @return The opposite corner from the {@link #min()}.
     */
    BetterBlockPos max();

    /**
     * @return The size of this ISelection.
     */
    Vec3i size();

    /**
     * @return An {@link AxisAlignedBB} encompassing all blocks in this selection.
     */
    AxisAlignedBB aabb();

    /**
     * Returns a new {@link ISelection} expanded in the specified direction by the specified number of blocks.
     *
     * @param direction The direction to expand the selection.
     * @param blocks    How many blocks to expand it.
     * @return A new selection, expanded as specified.
     */
    ISelection expand(EnumFacing direction, int blocks);

    /**
     * Returns a new {@link ISelection} contracted in the specified direction by the specified number of blocks.
     * <p>
     * Note that, for example, if the direction specified is UP, the bottom of the selection will be shifted up. If it
     * is DOWN, the top of the selection will be shifted down.
     *
     * @param direction The direction to contract the selection.
     * @param blocks    How many blocks to contract it.
     * @return A new selection, contracted as specified.
     */
    ISelection contract(EnumFacing direction, int blocks);

    /**
     * Returns a new {@link ISelection} shifted in the specified direction by the specified number of blocks. This moves
     * the whole selection.
     *
     * @param direction The direction to shift the selection.
     * @param blocks    How many blocks to shift it.
     * @return A new selection, shifted as specified.
     */
    ISelection shift(EnumFacing direction, int blocks);
}
