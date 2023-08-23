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

package baritone.api.process;

import baritone.api.utils.BlockOptionalMeta;
import net.minecraft.block.Block;
import java.util.List;
import java.util.Arrays;

/**
 * but it rescans the world every once in a while so it doesn't get fooled by its cache
 */
public interface IGetToBlockProcess extends IBaritoneProcess {

    default void getToBlock(BlockOptionalMeta block) {
        getToBlock(Arrays.asList(block));
    }
    default void getToBlock(Block block) {
        getToBlock(new BlockOptionalMeta(block));
    }
    void getToBlock(List<BlockOptionalMeta> blocks);

    boolean blacklistClosest();
}
