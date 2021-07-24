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

import baritone.api.schematic.IStaticSchematic;
import baritone.api.schematic.MaskSchematic;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockCarpet;
import net.minecraft.block.state.IBlockState;

import java.util.OptionalInt;
import java.util.function.Predicate;

public class MapArtSchematic extends MaskSchematic {

    private final int[][] heightMap;

    public MapArtSchematic(IStaticSchematic schematic) {
        super(schematic);
        this.heightMap = generateHeightMap(schematic);
    }

    @Override
    protected boolean partOfMask(int x, int y, int z, IBlockState currentState)
    {
        return y >= heightMap[x][z];
    }

    private static int[][] generateHeightMap(IStaticSchematic schematic)
    {
        //int[][] heightMap = new int[schematic.widthX()][schematic.lengthZ()];
        int[][] heightMap = new int[schematic.widthX()][schematic.lengthZ()];

        for (int x = 0; x < schematic.widthX(); x++) {
            for (int z = 0; z < schematic.lengthZ(); z++) {
                IBlockState[] column = schematic.getColumn(x, z);

                heightMap[x][z] = firstNonAir(column);
            }
        }
        return heightMap;
    }

    /*
     * Old MapArtSchematic only cared about the above blocks, but if a block needed an adjacent block(carpets for instance) then we would have an issue,
     * so I changed it to care from the first non air block to the top
     */
    private static int firstNonAir(IBlockState[] arr)
    {
        for(int i = 0; i < arr.length; ++i)
        {
            if(!(arr[i].getBlock() instanceof BlockAir))
                return i;
        }

        return 256;
    }
}
