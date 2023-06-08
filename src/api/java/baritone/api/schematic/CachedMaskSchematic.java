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
 * @author Brady
 */
public abstract class CachedMaskSchematic extends MaskSchematic {

    /**
     * Mask array with {@code y,z,x} indexing
     */
    private final boolean[][][] mask;

    public CachedMaskSchematic(ISchematic schematic, StaticMaskFunction maskFunction) {
        super(schematic);
        this.mask = new boolean[schematic.heightY()][schematic.lengthZ()][schematic.widthX()];
        for (int y = 0; y < schematic.heightY(); y++) {
            for (int z = 0; z < schematic.lengthZ(); z++) {
                for (int x = 0; x < schematic.widthX(); x++) {
                    this.mask[y][z][x] = maskFunction.partOfMask(x, y, z);
                }
            }
        }
    }

    @Override
    protected final boolean partOfMask(int x, int y, int z, IBlockState currentState) {
        return this.mask[y][z][x];
    }

    @FunctionalInterface
    public interface StaticMaskFunction {
        boolean partOfMask(int x, int y, int z);
    }
}
