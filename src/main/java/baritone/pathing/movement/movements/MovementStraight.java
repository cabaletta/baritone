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

    public MovementStraight(IBaritone baritone, BetterBlockPos src, int destX, int destZ) {
        super(baritone, src, new BetterBlockPos(destX, src.y, destZ), new BetterBlockPos[]{});
    }

    // for now we only support blocks with the normal walk cost
    private boolean isGoodForWalk(CalculationContext context, int x, int z) {
        int y = src.y - 1;
        IBlockState state = context.get(x, y, z);
        Block block = state.getBlock();
        return MovementHelper.canWalkOn(context.bsi, x, y, z, state) &&
                block.slipperiness == 0.6F &&
                block != Blocks.SOUL_SAND;
    }

    private LineBlockIterator getLineBlockIterator() {
        return new LineBlockIterator(src.x, src.z, dest.x, dest.z);
    }

    private boolean checkIfPossible(CalculationContext context) {
        LineBlockIterator iterator = getLineBlockIterator();
        while (iterator.next()) {
            if (!isGoodForWalk(context, iterator.currX, iterator.currY) ||
                    !MovementHelper.canWalkThrough(context.bsi, iterator.currX, src.y, iterator.currY) ||
                    !MovementHelper.canWalkThrough(context.bsi, iterator.currX, src.y + 1, iterator.currY)) {
                return false;
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
        return dist * WALK_ONE_BLOCK_COST;
    }

    @Override
    protected Set<BetterBlockPos> calculateValidPositions() {
        HashSet<BetterBlockPos> positions = new HashSet<>();

        LineBlockIterator iterator = getLineBlockIterator();
        while (iterator.next()) {
            positions.add(new BetterBlockPos(iterator.currX, src.y, iterator.currY));
        }

        return positions;
    }

    @Override
    protected boolean prepared(MovementState state) {
        return super.prepared(state) && ctx.playerFeet().y == src.y;
    }

    @Override
    public MovementState updateState(MovementState state) {
        super.updateState(state);

        if (state.getStatus() == MovementStatus.RUNNING) {
            if (ctx.playerFeet().equals(dest)) {
                return state.setStatus(MovementStatus.SUCCESS);
            } else {
                state.setInput(Input.SPRINT, true);
                MovementHelper.moveTowards(ctx, state, dest);
            }
        }

        return state;
    }

}
