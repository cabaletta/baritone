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

import java.util.stream.Stream;

/**
 * This process manages the inventory of a player. 
 *  - Check if inventory if full (TODO: what defines full?)
 *      - If so, place in shulkers
 *      - or find the nearest inventory that has space
 *      - TODO: idea = tries to dump unrelated items that it doesn't care about?
 * 
 * @author matthewfcarlson
 * @since 4/16/2020
 */
public interface IInventoryStoreProcess extends IBaritoneProcess {

    /**
     * Attempts to store certain blocks in shulker boxes and 
     * remembered inventories
     */
    void storeBlocks(int quantity, BlockOptionalMetaLookup filter);

    /**
     * Tries to store all the blocks in the inventory
     */
    default void storeBlocks(BlockOptionalMetaLookup filter) {
        storeBlocks(0, filter);
    }

    /**
     * Stores blocks by name
     */
    void storeBlocksByName(int quantity, String... blocks);

    /**
     * Stores all the blocks of a type in chest systems
     *
     * @param blocks The blocks to mine
     */
    default void storeBlocksByName(String... blocks) {
        storeBlocksByName(0, blocks);
    }

    /**
     * Stores the blocks requested
     */
    default void storeBlocks(int quantity, Block... blocks) {
        storeBlocks(quantity, new BlockOptionalMetaLookup(
                Stream.of(blocks)
                        .map(BlockOptionalMeta::new)
                        .toArray(BlockOptionalMeta[]::new)
        ));
    }

    /**
     * Cancels the current task
     */
    void cancel();
}
