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

package baritone.pathing.movement.movements;

import baritone.api.IBaritone;
import baritone.api.pathing.movement.MovementStatus;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.input.Input;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.Movement;
import baritone.pathing.movement.MovementHelper;
import baritone.pathing.movement.MovementState;
import baritone.utils.pathing.LineBlockIterator;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;

import java.util.HashSet;
import java.util.Set;

public final class MovementStraight extends Movement {

    public MovementStraight(IBaritone baritone, BetterBlockPos src, BetterBlockPos dest) {
        super(baritone, src, dest, new BetterBlockPos[]{});
    }

    // for now we only support blocks with the normal walk cost
    private boolean isGoodForWalk(CalculationContext context, int x, int y, int z) {
        IBlockState state = context.get(x, y, z);
        Block block = state.getBlock();
        return MovementHelper.canWalkOn(context.bsi, x, y, z, state) &&
                block.slipperiness == 0.6F &&
                block != Blocks.SOUL_SAND;
    }

    private LineBlockIterator getHorizontalLineIterator() {
        return new LineBlockIterator(src.x, src.z, dest.x, dest.z);
    }

    private boolean checkIfPossible(CalculationContext context) {
        // can only fall and there must be a block under the player
        if (dest.y > src.y || src.y < 1) {
            return false;
        }

        int y = src.y;

        LineBlockIterator iterator = getHorizontalLineIterator();
        while (iterator.next()) {
            if (!MovementHelper.canWalkThrough(context.bsi, iterator.currX, y, iterator.currY) ||
                    !MovementHelper.canWalkThrough(context.bsi, iterator.currX, y + 1, iterator.currY)) {
                return false;
            }

            // Make sure that we can walk on the path, and if we can't, then
            // try again but after falling down:
            // ______
            // [][][]\
            // [][][]|____
            // [][][][][][]
            while (!isGoodForWalk(context, iterator.currX, y - 1, iterator.currY)) {
                y--;

                if (y < 1) {
                    return false;
                }

                // make sure that we can fall
                if (!MovementHelper.canWalkThrough(context.bsi, iterator.currX, y, iterator.currY)) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public double calculateCost(CalculationContext context) {
        if (!checkIfPossible(context)) {
            return COST_INF;
        }

        double xDiff = dest.x - src.x;
        double zDiff = dest.z - src.z;
        double dist = Math.sqrt(xDiff * xDiff + zDiff * zDiff);
        double cost = dist * WALK_ONE_BLOCK_COST;

        if (context.canSprint) {
            cost *= SPRINT_MULTIPLIER;
        }

        return cost;
    }

    @Override
    protected Set<BetterBlockPos> calculateValidPositions() {
        HashSet<BetterBlockPos> positions = new HashSet<>();

        LineBlockIterator iterator = getHorizontalLineIterator();
        while (iterator.next()) {
            // TODO: only add the valid positions to use less memory
            for (int y = dest.y; y <= src.y; y++) {
                positions.add(new BetterBlockPos(iterator.currX, y, iterator.currY));
            }
        }

        return positions;
    }

    @Override
    protected boolean prepared(MovementState state) {
        if (!super.prepared(state)) {
            return false;
        }

        // TODO: check if the path is still possible
        return ctx.player().posY >= dest.y;
    }

    @Override
    public MovementState updateState(MovementState state) {
        super.updateState(state);

        if (state.getStatus() == MovementStatus.RUNNING) {
            if (ctx.playerFeet().equals(dest)) {
                return state.setStatus(MovementStatus.SUCCESS);
            } else {
                if (ctx.player().onGround) {
                    state.setInput(Input.SPRINT, false);
                    MovementHelper.moveTowards(ctx, state, dest);
                } else {
                    // Wait until we fall to prevent ending on blocks in the air
                    // that were not expected.
                    // TODO: improve this because it doesn't always work
                    state.setInput(Input.MOVE_FORWARD, false)
                            .setInput(Input.SPRINT, false)
                            .setTarget(new MovementState.MovementTarget());
                }
            }
        }

        return state;
    }

}
