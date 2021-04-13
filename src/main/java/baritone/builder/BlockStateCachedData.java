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

package baritone.builder;

import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Information about an IBlockState
 * <p>
 * There will be exactly one of these per valid IBlockState in the game
 */
public final class BlockStateCachedData {

    public boolean canWalkOn;
    public boolean isAir;

    public boolean canPlaceAgainstAtAll;

    private final List<BlockStatePlacementOption> options;


    public BlockStateCachedData(IBlockState state) {
        isAir = state.getBlock() instanceof BlockAir;
        canPlaceAgainstAtAll = state.getBlock() == Blocks.COBBLESTONE || state.getBlock() == Blocks.DIRT;
        canWalkOn = canPlaceAgainstAtAll;
        this.options = Collections.unmodifiableList(calcOptions());
    }

    private List<BlockStatePlacementOption> calcOptions() {
        if (canPlaceAgainstAtAll) {
            List<BlockStatePlacementOption> ret = new ArrayList<>();
            for (Face face : Face.VALUES) {
                if (Main.STRICT_Y && face == Face.UP) {
                    continue;
                }
                ret.add(new BlockStatePlacementOption(face));
            }
            return ret;
        }
        return Collections.emptyList();
    }


    public boolean canBeDoneAgainstMe(BlockStatePlacementOption placement) {
        // if i am a bottom slab, and the placement option is Face.DOWN, it's impossible
        // if i am a top slab, and the placement option is Face.UP, it's impossible
        // if i am a bottom slab, and the placement option is Half.TOP, it's impossible
        // if i am a top slab, and the placement option is Half.BOTTOM, it's impossible

        // if i am a stair then in newer versions of minecraft its complicated because voxel shapes and stuff blegh

        if (!canPlaceAgainstAtAll) {
            return false;
        }
        //if (Main.DEBUG) {
        return Main.RAND.nextInt(10) < 8;
        //}
        //throw new UnsupportedOperationException();
    }

    public List<BlockStatePlacementOption> placementOptions() {
        return options;
    }

    private static final BlockStateCachedData[] PER_STATE = new BlockStateCachedData[Block.BLOCK_STATE_IDS.size()];
    public static final BlockStateCachedData SCAFFOLDING;

    static {
        Block.BLOCK_STATE_IDS.forEach(state -> PER_STATE[Block.BLOCK_STATE_IDS.get(state)] = new BlockStateCachedData(state));
        SCAFFOLDING = get(Blocks.COBBLESTONE.getDefaultState());
    }

    public static BlockStateCachedData get(int state) {
        return PER_STATE[state];
    }

    public static BlockStateCachedData get(IBlockState state) {
        return get(Block.BLOCK_STATE_IDS.get(state));
    }
}
