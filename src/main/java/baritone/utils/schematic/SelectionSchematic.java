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

package baritone.utils.schematic;

import baritone.api.schematic.ISchematic;
import baritone.api.schematic.MaskSchematic;
import baritone.api.selection.ISelection;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

import java.util.stream.Stream;

public class SelectionSchematic extends MaskSchematic {

    private final ISelection[] selections;

    public SelectionSchematic(ISchematic schematic, BlockPos origin, ISelection[] selections) {
        super(schematic);
        baritone.api.utils.Helper.HELPER.logDirect(String.format("%s", selections[0].min()));
        this.selections = Stream.of(selections).map(
                sel -> sel
                    .shift(EnumFacing.WEST, origin.getX())
                    .shift(EnumFacing.DOWN, origin.getY())
                    .shift(EnumFacing.NORTH, origin.getZ()))
                .toArray(ISelection[]::new);
        baritone.api.utils.Helper.HELPER.logDirect(String.format("%s", this.selections[0].min()));
        baritone.api.utils.Helper.HELPER.logDirect(String.format("%s", origin));
    }

    @Override
    protected boolean partOfMask(int x, int y, int z, IBlockState currentState) {
        for (ISelection selection : selections) {
            if (x >= selection.min().x && y >= selection.min().y && z >= selection.min().z
             && x <= selection.max().x && y <= selection.max().y && z <= selection.max().z) {
                return true;
            }
        }
        return false;
    }
}
