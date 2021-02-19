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

import baritone.api.utils.BlockOptionalMetaLookup;
import net.minecraft.block.state.IBlockState;

public class ReplaceSchematic extends MaskSchematic {

    private final BlockOptionalMetaLookup filter;
    private final Boolean[][][] cache;

    public ReplaceSchematic(ISchematic schematic, BlockOptionalMetaLookup filter) {
        super(schematic);
        this.filter = filter;
        this.cache = new Boolean[widthX()][heightY()][lengthZ()];
    }

    @Override
    public void reset() {
        // it's final, can't use this.cache = new Boolean[widthX()][heightY()][lengthZ()]
        for (int x = 0; x < cache.length; x++) {
            for (int y = 0; y < cache[0].length; y++) {
                for (int z = 0; z < cache[0][0].length; z++) {
                    cache[x][y][z] = null;
                }
            }
        }
    }

    @Override
    protected boolean partOfMask(int x, int y, int z, IBlockState currentState) {
        if (cache[x][y][z] == null) {
            cache[x][y][z] = filter.has(currentState);
        }
        return cache[x][y][z];
    }
}
