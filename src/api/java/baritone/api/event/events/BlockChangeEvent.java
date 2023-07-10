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

package baritone.api.event.events;

import baritone.api.utils.Pair;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author Brady
 */
public final class BlockChangeEvent {

    private final ChunkPos chunk;
    private final List<Pair<BlockPos, IBlockState>> blocks;

    public BlockChangeEvent(ChunkPos pos, List<Pair<BlockPos, IBlockState>> blocks) {
        this.chunk = pos;
        this.blocks = blocks;
    }

    public ChunkPos getChunkPos() {
        return this.chunk;
    }

    public List<Pair<BlockPos, IBlockState>> getBlocks() {
        return this.blocks;
    }
}
