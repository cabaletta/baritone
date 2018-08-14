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

package baritone.bot.pathing.movement.movements;

import baritone.bot.pathing.movement.CalculationContext;
import baritone.bot.pathing.movement.Movement;
import baritone.bot.pathing.movement.MovementHelper;
import baritone.bot.pathing.movement.MovementState;
import baritone.bot.utils.BlockStateInterface;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.BlockVine;
import net.minecraft.util.math.BlockPos;

public class MovementDownward extends Movement {

    private int numTicks = 0;

    public MovementDownward(BlockPos start) {
        super(start, start.down(), new BlockPos[]{start.down()}, new BlockPos[0]);
    }

    @Override
    protected double calculateCost(CalculationContext context) {
        if (!MovementHelper.canWalkOn(dest.down())) {
            return COST_INF;
        }
        Block td = BlockStateInterface.get(dest).getBlock();
        boolean ladder = td instanceof BlockLadder || td instanceof BlockVine;
        if (ladder) {
            return LADDER_DOWN_ONE_COST;
        } else {
            return FALL_N_BLOCKS_COST[1] + getTotalHardnessOfBlocksToBreak(context.getToolSet());
        }
    }

    @Override
    public MovementState updateState(MovementState state) {
        super.updateState(state);
        switch (state.getStatus()) {
            case PREPPING:
            case UNREACHABLE:
            case FAILED:
                return state;
            case WAITING:
            case RUNNING:
                break;
            default:
                return state;
        }
        if (playerFeet().equals(dest)) {
            state.setStatus(MovementState.MovementStatus.SUCCESS);
            return state;
        }
        double diffX = player().posX - (dest.getX() + 0.5);
        double diffZ = player().posZ - (dest.getZ() + 0.5);
        double ab = Math.sqrt(diffX * diffX + diffZ * diffZ);

        if (numTicks++ < 10 && ab < 0.2) {
            return state;
        }
        MovementHelper.moveTowards(state, positionsToBreak[0]);
        return state;
    }
}
