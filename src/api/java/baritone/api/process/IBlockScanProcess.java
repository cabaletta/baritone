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

import baritone.api.utils.BetterBlockPos;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Map;

/**
 * @author Author
 * @since Date
 */
public interface IBlockScanProcess extends IBaritoneProcess {

    /**
     * Start scanning with a radius around the player
     *
     * @param blocks The blocks to search for
     * @param radius The radius to scan
     * @param scanAll If true, scan entire area; if false, stop at first find
     */
    void startScanRadius(List<Block> blocks, int radius, boolean scanAll);

    /**
     * Start scanning a rectangular region
     *
     * @param blocks The blocks to search for
     * @param pos1 First corner of the region
     * @param pos2 Second corner of the region
     * @param scanAll If true, scan entire area; if false, stop at first find
     */
    void startScanRegion(List<Block> blocks, BlockPos pos1, BlockPos pos2, boolean scanAll);

    /**
     * Stop the current scan
     */
    void stop();
    
    /**
     * Force stop the scan (used by #cancel command)
     */
    void forceStop();

    /**
     * Clear the scan results
     */
    void clearResults();

    /**
     * Get the blocks that have been found so far
     *
     * @return Map of block types to their found positions
     */
    Map<Block, List<BetterBlockPos>> getFoundBlocks();
    
    /**
     * Get scan progress as a percentage (0-100)
     *
     * @return Progress percentage
     */
    int getProgress();
    
    /**
     * Get a formatted progress string
     *
     * @return Progress string with details
     */
    String getProgressString();
}
