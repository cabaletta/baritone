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

import baritone.api.selection.ISelection;
import net.minecraft.core.BlockPos;

/**
 * Process for branch mining within a defined area.
 *
 * <p>The bot will excavate a staircase branch-mine pattern through the selected
 * volume, optionally collecting visible ores along the way. Branches are staggered
 * between layers so that lower layers cover the gaps left by upper layers.</p>
 *
 * @author leijurv (adapted)
 * @since 1.0.0
 */
public interface IBranchMineProcess extends IBaritoneProcess {

    /**
     * Begin branch mining the volume defined by the two corner positions.
     *
     * @param pos1 First corner
     * @param pos2 Second corner
     */
    void branchMine(BlockPos pos1, BlockPos pos2);

    /**
     * Begin branch mining the volume defined by the given selection.
     *
     * @param selection The selection to mine within
     */
    void branchMine(ISelection selection);

    /**
     * Cancel the current branch mining task.
     */
    default void cancel() {
        onLostControl();
    }
}
