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

package baritone.api.schematic;

import net.minecraft.block.state.IBlockState;

/**
 * A static schematic is capable of providing the desired state at a given position without
 * additional context. Schematics of this type are expected to have non-varying contents.
 *
 * @author Brady
 * @see #getDirect(int, int, int)
 * @since 12/24/2019
 */
public interface IStaticSchematic extends ISchematic {

    /**
     * Gets the {@link IBlockState} for a given position in this schematic. It should be guaranteed
     * that the return value of this method will not change given that the parameters are the same.
     *
     * @param x The X block position
     * @param y The Y block position
     * @param z The Z block position
     * @return The desired state at the specified position.
     */
    IBlockState getDirect(int x, int y, int z);

    /**
     * Returns an {@link IBlockState} array of size {@link #heightY()} which contains all
     * desired block states in the specified vertical column. The index of {@link IBlockState}s
     * in the array are equivalent to their Y position in the schematic.
     *
     * @param x The X column position
     * @param z The Z column position
     * @return An {@link IBlockState} array
     */
    default IBlockState[] getColumn(int x, int z) {
        IBlockState[] column = new IBlockState[this.heightY()];
        for (int i = 0; i < this.heightY(); i++) {
            column[i] = getDirect(x, i, z);
        }
        return column;
    }
}
