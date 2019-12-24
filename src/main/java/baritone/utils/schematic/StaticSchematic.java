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
import net.minecraft.block.BlockAir;
import net.minecraft.block.state.IBlockState;

import java.util.List;
import java.util.OptionalInt;
import java.util.function.Predicate;

/**
 * @author Brady
 * @since 12/23/2019
 */
public abstract class StaticSchematic extends AbstractSchematic {

    /**
     * Block states for this schematic stored in [x, z, y] indexing order
     */
    protected IBlockState[][][] states;

    /**
     * The maximum height of a given block in this schematic, indexed as [x, z].
     * This is lazily initialized by {@link #getHeightMap()}.
     */
    protected int[][] heightMap;

    public StaticSchematic() {
        super();
    }

    public StaticSchematic(int x, int y, int z) {
        super(x, y, z);
    }

    @Override
    public IBlockState desiredState(int x, int y, int z, IBlockState current, List<IBlockState> approxPlaceable) {
        return this.states[x][z][y];
    }

    public final int[][] getHeightMap() {
        if (this.heightMap == null) {
            this.heightMap = new int[this.x][this.z];

            for (int x = 0; x < this.x; x++) {
                for (int z = 0; z < this.z; z++) {
                    IBlockState[] column = states[x][z];

                    OptionalInt lowestBlockY = lastIndexMatching(column, state -> !(state.getBlock() instanceof BlockAir));
                    if (lowestBlockY.isPresent()) {
                        this.heightMap[x][z] = lowestBlockY.getAsInt();
                    } else {
                        System.out.println("Column " + x + "," + z + " has no blocks, but it's apparently map art? wtf");
                        System.out.println("Letting it be whatever");
                        this.heightMap[x][z] = 256;
                    }
                }
            }
        }
        return this.heightMap;
    }

    private static <T> OptionalInt lastIndexMatching(T[] arr, Predicate<? super T> predicate) {
        for (int y = arr.length - 1; y >= 0; y--) {
            if (predicate.test(arr[y])) {
                return OptionalInt.of(y);
            }
        }
        return OptionalInt.empty();
    }
}
