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

import baritone.api.schematic.mask.Mask;
import net.minecraft.block.state.IBlockState;

import java.util.List;

public abstract class MaskSchematic extends AbstractSchematic {

    private final ISchematic schematic;

    public MaskSchematic(ISchematic schematic) {
        super(schematic.widthX(), schematic.heightY(), schematic.lengthZ());
        this.schematic = schematic;
    }

    protected abstract boolean partOfMask(int x, int y, int z, IBlockState currentState);

    @Override
    public boolean inSchematic(int x, int y, int z, IBlockState currentState) {
        return schematic.inSchematic(x, y, z, currentState) && partOfMask(x, y, z, currentState);
    }

    @Override
    public IBlockState desiredState(int x, int y, int z, IBlockState current, List<IBlockState> approxPlaceable) {
        return schematic.desiredState(x, y, z, current, approxPlaceable);
    }

    public static MaskSchematic create(ISchematic schematic, Mask function) {
        return new MaskSchematic(schematic) {

            @Override
            protected boolean partOfMask(int x, int y, int z, IBlockState currentState) {
                return function.partOfMask(x, y, z, currentState);
            }
        };
    }
}
