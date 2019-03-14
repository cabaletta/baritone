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

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;

import java.util.OptionalInt;
import java.util.function.Predicate;

public class MapArtSchematic extends Schematic {

    private final int[][] heightMap;

    public MapArtSchematic(NBTTagCompound schematic) {
        super(schematic);
        heightMap = new int[widthX][lengthZ];

        for (int x = 0; x < widthX; x++) {
            for (int z = 0; z < lengthZ; z++) {
                IBlockState[] column = states[x][z];

                OptionalInt lowestBlockY = lastIndexMatching(column, block -> block != Blocks.AIR);
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
    public boolean inSchematic(int x, int y, int z) {
        // in map art, we only care about coordinates in or above the art
        return super.inSchematic(x, y, z) && y >= heightMap[x][z];
    }
}
