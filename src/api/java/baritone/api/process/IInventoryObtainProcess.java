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
 *   - It can be requested to get items (by build process)
 *      - first looks in inventory (shulker boxes)
 *      - next looks in immediate area for item entities that can be picked up
 *      - next looks in nearby remembered inventories
 *      - next tries to craft the item? (this is a planned feature?)
 * 
 * @author matthewfcarlson
 * @since 4/16/2020
 */
public interface IInventoryObtainProcess extends IBaritoneProcess {

    /**
     * Begin to search for the specified blocks until the number of
     * specified items to get. Shulker chests are checked first,
     * then remembered chests/inventories.
     *
     * @param quantity The number of items to attempt to get
     * @param filter   The blocks to obtain
     */
    void obtainBlocks(int quantity, BlockOptionalMetaLookup filter);

    /**
     * Begin to search for the specified blocks, try to get as 
     * many as you can?
     *
     * @param filter The blocks to mine
     */
    default void obtainBlocks(BlockOptionalMetaLookup filter){
        obtainBlocks(0, filter);
    }

    /**
     * Begin to search for the specified blocks until the number of
     * specified items to get. Shulker chests are checked first,
     * then remembered chests/inventories.
     *
     * @param quantity The number of items to attempt to get
     * @param blocks   The names of the blocks to obtain
     */
    void obtainBlocksByName(int quantity, String... blocks);

    /**
     * Tries to fill the inventory with blocks of these names
     * @param blocks  The name of the blocks
     */
    default void obtainBlocksByName(String... blocks) {
        obtainBlocksByName(0, blocks);
    }

    /**
     * Cancels the current mining task
     */
    default void cancel() {
        onLostControl();
    }
}
