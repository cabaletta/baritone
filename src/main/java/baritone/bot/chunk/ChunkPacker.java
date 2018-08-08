/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.bot.chunk;

import baritone.bot.pathing.movement.MovementHelper;
import baritone.bot.pathing.util.PathingBlockType;
import baritone.bot.utils.BlockStateInterface;
import baritone.bot.utils.Helper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;

import java.util.BitSet;

import static net.minecraft.block.Block.NULL_AABB;

/**
 * @author Brady
 * @since 8/3/2018 1:09 AM
 */
public final class ChunkPacker implements Helper {

    private ChunkPacker() {}

    public static BitSet createPackedChunk(Chunk chunk) {
        BitSet bitSet = new BitSet(CachedChunk.SIZE);
        try {
            for (int y = 0; y < 256; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        int index = CachedChunk.getPositionIndex(x, y, z);
                        boolean[] bits = getPathingBlockType(new BlockPos(x, y, z), chunk.getBlockState(x, y, z)).getBits();
                        bitSet.set(index, bits[0]);
                        bitSet.set(index + 1, bits[1]);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bitSet;
    }

    private static PathingBlockType getPathingBlockType(BlockPos pos, IBlockState state) {
        Block block = state.getBlock();

        if (BlockStateInterface.isWater(block)) {
            return PathingBlockType.WATER;
        }

        if (MovementHelper.avoidWalkingInto(block)) {
            return PathingBlockType.AVOID;
        }

        if (block instanceof BlockAir || state.getCollisionBoundingBox(mc.world, pos) == NULL_AABB) {
            return PathingBlockType.AIR;
        }

        return PathingBlockType.SOLID;
    }
}
