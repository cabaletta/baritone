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

import baritone.bot.InputOverrideHandler;
import baritone.bot.pathing.movement.CalculationContext;
import baritone.bot.pathing.movement.Movement;
import baritone.bot.pathing.movement.MovementHelper;
import baritone.bot.pathing.movement.MovementState;
import baritone.bot.pathing.movement.MovementState.MovementStatus;
import baritone.bot.utils.BlockStateInterface;
import net.minecraft.block.BlockFalling;
import net.minecraft.util.math.BlockPos;

public class MovementAscend extends Movement {

    private BlockPos[] against = new BlockPos[3];

    public MovementAscend(BlockPos src, BlockPos dest) {
        super(src, dest, new BlockPos[] { dest, src.up(2), dest.up() }, new BlockPos[] { dest.down() });

        BlockPos placementLocation = positionsToPlace[0]; // dest.down()
        int i = 0;
        if (!placementLocation.north().equals(src))
            against[i++] = placementLocation.north();

        if (!placementLocation.south().equals(src))
            against[i++] = placementLocation.south();

        if (!placementLocation.east().equals(src))
            against[i++] = placementLocation.east();

        if (!placementLocation.west().equals(src))
            against[i] = placementLocation.west();

        // TODO: add ability to place against .down() as well as the cardinal directions
        // useful for when you are starting a staircase without anything to place against
        // Counterpoint to the above TODO ^ you should move then pillar instead of ascend
    }

    @Override
    protected double calculateCost(CalculationContext context) {
        if (!MovementHelper.canWalkOn(positionsToPlace[0])) {
            if (!BlockStateInterface.isAir(positionsToPlace[0]) && !BlockStateInterface.isWater(positionsToPlace[0])) {
                return COST_INF;
            }
            if (true) {
                return COST_INF;
            }
            for (BlockPos against1 : against) {
                if (BlockStateInterface.get(against1).isBlockNormalCube()) {
                    return JUMP_ONE_BLOCK_COST + WALK_ONE_BLOCK_COST + PLACE_ONE_BLOCK_COST + getTotalHardnessOfBlocksToBreak(context.getToolSet());
                }
            }
            return COST_INF;
        }
        if (BlockStateInterface.get(src.up(3)).getBlock() instanceof BlockFalling) {//it would fall on us and possibly suffocate us
            return COST_INF;
        }
        // we walk half the block to get to the edge, then we walk the other half while simultaneously jumping (math.max because of how it's in parallel)
        return WALK_ONE_BLOCK_COST / 2 + Math.max(JUMP_ONE_BLOCK_COST, WALK_ONE_BLOCK_COST / 2) + getTotalHardnessOfBlocksToBreak(context.getToolSet());
    }

    @Override
    public MovementState updateState(MovementState state) {
        super.updateState(state);
        System.out.println("Ticking with state " + state.getStatus());
        // TODO incorporate some behavior from ActionClimb (specifically how it waited until it was at most 1.2 blocks away before starting to jump
        // for efficiency in ascending minimal height staircases, which is just repeated MovementAscend, so that it doesn't bonk its head on the ceiling repeatedly)
        switch (state.getStatus()) {
            case PREPPING:
            case UNREACHABLE:
            case FAILED:
                return state;
            case WAITING:
            case RUNNING:
                if (playerFeet().equals(dest)) {
                    state.setStatus(MovementStatus.SUCCESS);
                    return state;
                }
                MovementHelper.moveTowards(state, positionsToBreak[0]);
                return state.setInput(InputOverrideHandler.Input.JUMP, true);
            default:
                return state;
        }
    }
}
