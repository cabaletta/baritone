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

package baritone.builder.mc;

import baritone.builder.BlockStateCachedDataBuilder;
import baritone.builder.Face;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;

/**
 * I expect this class to get extremely complicated.
 * <p>
 * Thankfully, all of it will be confined to this class, so when there's changes to new versions of Minecraft, e.g. new blocks, there will be only one place to look.
 */
public class BlockStatePropertiesExtractor {

    public static BlockStateCachedDataBuilder getData(IBlockState state) {
        Block block = state.getBlock();
        BlockStateCachedDataBuilder builder = new BlockStateCachedDataBuilder();

        if (block instanceof BlockAir) {
            return builder.air();
        }
        if (block instanceof BlockStairs) {
            boolean rightsideUp = state.getValue(BlockStairs.HALF) == BlockStairs.EnumHalf.BOTTOM; // true if normal stair, false if upside down stair
            EnumFacing facing = state.getValue(BlockStairs.FACING);
            return builder.stair(rightsideUp, Face.fromMC(facing));
        }
        if (block instanceof BlockSlab) {
            if (((BlockSlab) block).isDouble()) {
                return builder.normalFullBlock();
            }
            return builder.slab(state.getValue(BlockSlab.HALF) == BlockSlab.EnumBlockHalf.BOTTOM);
        }
        if (block == Blocks.COBBLESTONE || block == Blocks.DIRT) {
            builder.normalFullBlock();
        }
        return builder;
    }
}
