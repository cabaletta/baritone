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

import baritone.api.schematic.AbstractSchematic;
import baritone.api.schematic.ISchematic;
import net.minecraft.block.BlockAir;
import net.minecraft.block.state.IBlockState;

import java.util.List;
import java.util.OptionalInt;
import java.util.function.Predicate;

public class MapArtSchematic extends AbstractSchematic {

    private final ISchematic child;
    private final int[][] heightMap;

    public MapArtSchematic(ISchematic schematic) {
        super(schematic.widthX(), schematic.heightY(), schematic.lengthZ());
        this.child = schematic;

        heightMap = new int[schematic.widthX()][schematic.lengthZ()];

        for (int x = 0; x < schematic.widthX(); x++) {
            for (int z = 0; z < schematic.lengthZ(); z++) {
                IBlockState[] column = /*states[x][z]*/null;

                OptionalInt lowestBlockY = lastIndexMatching(column, state -> !(state.getBlock() instanceof BlockAir));
                if (lowestBlockY.isPresent()) {
                    heightMap[x][z] = lowestBlockY.getAsInt();
                } else {
                    System.out.println("Column " + x + "," + z + " has no blocks, but it's apparently map art? wtf");
                    System.out.println("Letting it be whatever");
                    heightMap[x][z] = 256;
                }
            }
        }
    }

    private static <T> OptionalInt lastIndexMatching(T[] arr, Predicate<? super T> predicate) {
        for (int y = arr.length - 1; y >= 0; y--) {
            if (predicate.test(arr[y])) {
                return OptionalInt.of(y);
            }
        }
        return OptionalInt.empty();
    }

    @Override
    public boolean inSchematic(int x, int y, int z, IBlockState currentState) {
        // in map art, we only care about coordinates in or above the art
        return super.inSchematic(x, y, z, currentState) && y >= heightMap[x][z];
    }

    @Override
    public IBlockState desiredState(int x, int y, int z, IBlockState current, List<IBlockState> approxPlaceable) {
        return this.child.desiredState(x, y, z, current, approxPlaceable);
    }

    @Override
    public int widthX() {
        return this.child.widthX();
    }

    @Override
    public int heightY() {
        return this.child.heightY();
    }

    @Override
    public int lengthZ() {
        return this.child.lengthZ();
    }
}
