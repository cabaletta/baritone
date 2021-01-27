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
import baritone.api.utils.BlockOptionalMetaLookup;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * @author Brady
 * @since 9/23/2018
 */
public interface IMineProcess extends IBaritoneProcess {

    /**
     * Begin to search for and mine the specified blocks until
     * the number of specified items to get from the blocks that
     * are mined.
     *
     * @param quantity The number of items to get per block
     * @param blocks   The blocks to mine
     */
    void mineByName(Map<BlockOptionalMeta, Integer> quantity, String... blocks);

    /**
     * Begin to search for and mine the specified blocks in a specified radius until
     * the number of specified items to get from the blocks that
     * are mined. This is based on the first target block to mine.
     *
     * @param startPos center of the circle
     * @param radius   radius of the circle that is used for mining
     * @param quantity The number of items to get for a block
     * @param filter   The blocks to mine
     */
    void mine(BlockPos startPos, int radius, Map<BlockOptionalMeta, Integer> quantity, BlockOptionalMetaLookup filter);

    /**
     * Begin to search for and mine the specified blocks.
     *
     * @param filter The blocks to mine
     */
    default void mine(BlockOptionalMetaLookup filter) {
        Map<BlockOptionalMeta, Integer> quantity = new HashMap<>();
        for (BlockOptionalMeta bom : filter.blocks()) {
            quantity.put(bom, 0);
        }
        mine(null, 0, quantity, filter);
    }

    /**
     * Begin to search for and mine the specified blocks.
     *
     * @param blocks The blocks to mine
     */
    default void mineByName(String... blocks) {
        mine(new BlockOptionalMetaLookup(blocks));
    }

    /**
     * Begin to search for and mine the specified blocks.
     *
     * @param boms The blocks to mine
     */
    default void mine(Map<BlockOptionalMeta, Integer> quantity, BlockOptionalMeta... boms) {
        mine(null, 0, quantity, new BlockOptionalMetaLookup(boms));
    }

    /**
     * Begin to search for and mine the specified blocks.
     *
     * @param boms The blocks to mine
     */
    default void mine(BlockOptionalMeta... boms) {
        Map<BlockOptionalMeta, Integer> quantity = new HashMap<>();
        for (BlockOptionalMeta bom : boms) {
            quantity.put(bom, 0);
        }
        mine(quantity, boms);
    }

    /**
     * Begin to search for and mine the specified blocks.
     *
     * @param quantity The number of items to get for a block block
     * @param blocks   The blocks to mine
     */
    default void mine(Map<BlockOptionalMeta, Integer> quantity, Block... blocks) {
        mine(null, 0, quantity, new BlockOptionalMetaLookup(
                Stream.of(blocks)
                        .map(BlockOptionalMeta::new)
                        .toArray(BlockOptionalMeta[]::new)
        ));
    }

    /**
     * Begin to search for and mine the specified blocks.
     *
     * @param blocks The blocks to mine
     */
    default void mine(Block... blocks) {
        BlockOptionalMetaLookup boml = new BlockOptionalMetaLookup(Stream.of(blocks).map(BlockOptionalMeta::new).toArray(BlockOptionalMeta[]::new));
        Map<BlockOptionalMeta, Integer> quantity = new HashMap<>();
        for (BlockOptionalMeta bom : boml.blocks()) {
            quantity.put(bom, 0);
        }
        mine(null, 0, quantity, boml);
    }

    /**
     * Cancels the current mining task
     */
    default void cancel() {
        onLostControl();
    }
}
