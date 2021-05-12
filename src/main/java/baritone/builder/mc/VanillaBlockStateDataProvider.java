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

import baritone.builder.BlockStateCachedData;
import baritone.builder.IBlockStateDataProvider;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;

public class VanillaBlockStateDataProvider implements IBlockStateDataProvider {

    @Override
    public int numStates() {
        return Block.BLOCK_STATE_IDS.size();
    }

    @Override
    public BlockStateCachedData getNullable(int i) {
        IBlockState state = Block.BLOCK_STATE_IDS.getByValue(i);
        if (state == null) {
            return null;
        }
        try {
            return new BlockStateCachedData(BlockStatePropertiesExtractor.getData(state));
        } catch (Throwable th) {
            throw new RuntimeException("Exception while extracting " + state + " ID " + i, th);
        }
    }
}
