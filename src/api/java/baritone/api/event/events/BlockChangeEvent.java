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
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * @author Brady
 */
public final class BlockChangeEvent {

    private final ChunkPos chunk;
    private final List<Pair<BlockPos, BlockState>> blocks;

    public BlockChangeEvent(ChunkPos pos, List<Pair<BlockPos, BlockState>> blocks) {
        this.chunk = pos;
        this.blocks = blocks;
    }

    public ChunkPos getChunkPos() {
        return this.chunk;
    }

    public List<Pair<BlockPos, BlockState>> getBlocks() {
        return this.blocks;
    }
}
