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

import baritone.Baritone;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.Movement;
import baritone.pathing.movement.MovementHelper;
import baritone.pathing.movement.MovementState;
import baritone.utils.BlockStateInterface;
import baritone.utils.InputOverrideHandler;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

public class MovementParkour extends Movement {

    final EnumFacing direction;
    final int dist;

    private MovementParkour(BlockPos src, int dist, EnumFacing dir) {
        super(src, src.offset(dir, dist), new BlockPos[]{});
        this.direction = dir;
        this.dist = dist;
        super.override(costFromJumpDistance(dist));
    }

    public static MovementParkour calculate(BlockPos src, EnumFacing dir) {
        if (!Baritone.settings().allowParkour.get()) {
            return null;
        }
        IBlockState standingOn = BlockStateInterface.get(src.down());
        if (standingOn.getBlock() == Blocks.VINE || standingOn.getBlock() == Blocks.LADDER || MovementHelper.isBottomSlab(standingOn)) {
            return null;
        }
        BlockPos adjBlock = src.down().offset(dir);
        IBlockState adj = BlockStateInterface.get(adjBlock);
        if (MovementHelper.avoidWalkingInto(adj.getBlock())) { // magma sucks
            return null;
        }
        if (MovementHelper.canWalkOn(adjBlock, adj)) { // don't parkour if we could just traverse (for now)
            return null;
        }

        if (!MovementHelper.fullyPassable(src.offset(dir))) {
            return null;
        }
        if (!MovementHelper.fullyPassable(src.up().offset(dir))) {
            return null;
        }
        for (int i = 2; i <= 4; i++) {
            BlockPos dest = src.offset(dir, i);
            if (!MovementHelper.fullyPassable(dest)) {
                return null;
            }
            if (!MovementHelper.fullyPassable(dest.up())) {
                return null;
            }
            if (MovementHelper.canWalkOn(dest.down())) {
                return new MovementParkour(src, i, dir);
            }
        }
        return null;
    }

    private static double costFromJumpDistance(int dist) {
        switch (dist) {
            case 2:
                return WALK_ONE_BLOCK_COST * 2; // IDK LOL
            case 3:
                return WALK_ONE_BLOCK_COST * 3;
            case 4:
                return SPRINT_ONE_BLOCK_COST * 4;
        }
        throw new IllegalStateException("LOL");
    }


    @Override
    protected double calculateCost(CalculationContext context) {
        if (!MovementHelper.canWalkOn(dest.down())) {
            return COST_INF;
        }
        if (MovementHelper.avoidWalkingInto(BlockStateInterface.get(src.down().offset(direction)).getBlock())) {
            return COST_INF;
        }
        for (int i = 1; i <= 4; i++) {
            BlockPos d = src.offset(direction, i);
            if (!MovementHelper.fullyPassable(d)) {
                return COST_INF;
            }
            if (!MovementHelper.fullyPassable(d.up())) {
                return COST_INF;
            }
            if (d.equals(dest)) {
                return costFromJumpDistance(i);
            }
        }
        throw new IllegalStateException("invalid jump distance?");
    }

    @Override
    public MovementState updateState(MovementState state) {
        super.updateState(state);
        switch (state.getStatus()) {
            case WAITING:
                state.setStatus(MovementState.MovementStatus.RUNNING);
            case RUNNING:
                break;
            default:
                return state;
        }
        if (dist >= 4) {
            state.setInput(InputOverrideHandler.Input.SPRINT, true);
        }
        MovementHelper.moveTowards(state, dest);
        if (playerFeet().equals(dest)) {
            if (player().posY - playerFeet().getY() < 0.01) {
                state.setStatus(MovementState.MovementStatus.SUCCESS);
            }
        } else if (!playerFeet().equals(src)) {
            if (playerFeet().equals(src.offset(direction)) || player().posY - playerFeet().getY() > 0.0001) {
                state.setInput(InputOverrideHandler.Input.JUMP, true);
            } else {
                state.setInput(InputOverrideHandler.Input.SPRINT, false);
                if (playerFeet().equals(src.offset(direction, -1))) {
                    MovementHelper.moveTowards(state, src);
                } else {
                    MovementHelper.moveTowards(state, src.offset(direction, -1));
                }
            }
        }
        return state;
    }
}