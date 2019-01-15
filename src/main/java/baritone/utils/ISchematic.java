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

package baritone.utils;

import net.minecraft.block.state.IBlockState;

public interface ISchematic {
    /**
     * Does the block at this coordinate matter to the schematic?
     * <p>
     * Normally just a check for if the coordinate is in the cube.
     * <p>
     * However, in the case of something like a map art, anything that's below the level of the map art doesn't matter,
     * so this function should return false in that case. (i.e. it doesn't really have to be air below the art blocks)
     *
     * @param x
     * @param y
     * @param z
     * @return
     */
    default boolean inSchematic(int x, int y, int z) {
        return x >= 0 && x < widthX() && y >= 0 && y < heightY() && z >= 0 && z < lengthZ();
    }

    IBlockState desiredState(int x, int y, int z);

    int widthX();

    int heightY();

    int lengthZ();
}