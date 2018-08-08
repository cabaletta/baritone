/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.bot.pathing.goals;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

/**
 * Don't get into the block, but get directly adjacent to it. Useful for chests.
 *
 * @author avecowa
 */
public class GoalGetToBlock extends GoalComposite {

    public GoalGetToBlock(BlockPos pos) {
        super(adjacentBlocks(pos));
    }

    private static BlockPos[] adjacentBlocks(BlockPos pos) {
        BlockPos[] sides = new BlockPos[6];
        for (int i = 0; i < 6; i++) {
            sides[i] = pos.offset(EnumFacing.values()[i]);
        }
        return sides;
    }
}
