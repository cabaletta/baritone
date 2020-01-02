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

public class WallsSchematic extends MaskSchematic {

    public WallsSchematic(ISchematic schematic) {
        super(schematic);
    }

    @Override
    protected boolean partOfMask(int x, int y, int z, IBlockState currentState) {
        return x == 0 || z == 0 || x == widthX() - 1 || z == lengthZ() - 1;
    }
}
