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

package baritone.pathing.movement.movements;

import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.Movement;
import baritone.pathing.movement.MovementHelper;
import baritone.pathing.movement.MovementState;
import baritone.pathing.movement.MovementState.MovementStatus;
import baritone.utils.BlockStateInterface;
import baritone.utils.InputOverrideHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;

public class MovementDescend extends Movement {

    public MovementDescend(BlockPos start, BlockPos end) {
        super(start, end, new BlockPos[]{end.up(2), end.up(), end}, end.down());
    }

    @Override
    public void reset() {
        super.reset();
        numTicks = 0;
    }

    @Override
    protected double calculateCost(CalculationContext context) {
        Block fromDown = BlockStateInterface.get(src.down()).getBlock();
        if (fromDown == Blocks.LADDER || fromDown == Blocks.VINE) {
            return COST_INF;
        }
        if (!MovementHelper.canWalkOn(positionToPlace)) {
            return COST_INF;
        }
        Block tmp1 = BlockStateInterface.get(dest).getBlock();
        if (tmp1 == Blocks.LADDER || tmp1 == Blocks.VINE) {
            return COST_INF;
        }
        // we walk half the block plus 0.3 to get to the edge, then we walk the other 0.2 while simultaneously falling (math.max because of how it's in parallel)
        double walk = WALK_OFF_BLOCK_COST;
        if (fromDown == Blocks.SOUL_SAND) {
            // use this ratio to apply the soul sand speed penalty to our 0.8 block distance
            walk = WALK_ONE_OVER_SOUL_SAND_COST;
        }
        return walk + Math.max(FALL_N_BLOCKS_COST[1], CENTER_AFTER_FALL_COST) + getTotalHardnessOfBlocksToBreak(context);
    }

    int numTicks = 0;

    @Override
    public MovementState updateState(MovementState state) {
        super.updateState(state);
        if (state.getStatus() != MovementStatus.RUNNING) {
            return state;
        }

        BlockPos playerFeet = playerFeet();
        if (playerFeet.equals(dest)) {
            if (BlockStateInterface.isLiquid(dest) || player().posY - playerFeet.getY() < 0.094) { // lilypads
                // Wait until we're actually on the ground before saying we're done because sometimes we continue to fall if the next action starts immediately
                state.setStatus(MovementStatus.SUCCESS);
                return state;
            } else {
                System.out.println(player().posY + " " + playerFeet.getY() + " " + (player().posY - playerFeet.getY()));
            }
        }
        double diffX = player().posX - (dest.getX() + 0.5);
        double diffZ = player().posZ - (dest.getZ() + 0.5);
        double ab = Math.sqrt(diffX * diffX + diffZ * diffZ);
        double x = player().posX - (src.getX() + 0.5);
        double z = player().posZ - (src.getZ() + 0.5);
        double fromStart = Math.sqrt(x * x + z * z);
        if (!playerFeet.equals(dest) || ab > 0.25) {
            BlockPos fakeDest = new BlockPos(dest.getX() * 2 - src.getX(), dest.getY(), dest.getZ() * 2 - src.getZ());
            if (numTicks++ < 20) {
                MovementHelper.moveTowards(state, fakeDest);
                if (fromStart > 1.25) {
                    state.setInput(InputOverrideHandler.Input.MOVE_FORWARD, false);
                    state.setInput(InputOverrideHandler.Input.MOVE_BACK, true);
                }
            } else {
                MovementHelper.moveTowards(state, dest);
            }
        }
        return state;
    }
}
