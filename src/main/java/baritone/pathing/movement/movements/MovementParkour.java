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
import baritone.behavior.LookBehaviorUtils;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.Movement;
import baritone.pathing.movement.MovementHelper;
import baritone.pathing.movement.MovementState;
import baritone.utils.BlockStateInterface;
import baritone.utils.InputOverrideHandler;
import baritone.utils.Utils;
import baritone.utils.pathing.BetterBlockPos;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Objects;

public class MovementParkour extends Movement {
    protected static final EnumFacing[] HORIZONTALS_BUT_ALSO_DOWN_SO_EVERY_DIRECTION_EXCEPT_UP = {EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.EAST, EnumFacing.WEST, EnumFacing.DOWN};


    final EnumFacing direction;
    final int dist;

    private MovementParkour(BetterBlockPos src, int dist, EnumFacing dir) {
        super(src, src.offset(dir, dist), new BlockPos[]{});
        this.direction = dir;
        this.dist = dist;
        super.override(costFromJumpDistance(dist));
    }

    public static MovementParkour generate(BetterBlockPos src, EnumFacing dir, CalculationContext context) {
        // MUST BE KEPT IN SYNC WITH calculateCost
        if (!Baritone.settings().allowParkour.get()) {
            return null;
        }
        IBlockState standingOn = BlockStateInterface.get(src.down());
        if (standingOn.getBlock() == Blocks.VINE || standingOn.getBlock() == Blocks.LADDER || MovementHelper.isBottomSlab(standingOn)) {
            return null;
        }
        BlockPos adjBlock = src.down().offset(dir);
        IBlockState adj = BlockStateInterface.get(adjBlock);
        if (MovementHelper.avoidWalkingInto(adj.getBlock()) && adj.getBlock() != Blocks.WATER && adj.getBlock() != Blocks.FLOWING_WATER) { // magma sucks
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
        if (!MovementHelper.fullyPassable(src.up(2).offset(dir))) {
            return null;
        }
        if (!MovementHelper.fullyPassable(src.up(2))) {
            return null;
        }
        for (int i = 2; i <= (context.canSprint() ? 4 : 3); i++) {
            BlockPos dest = src.offset(dir, i);
            // TODO perhaps dest.up(3) doesn't need to be fullyPassable, just canWalkThrough, possibly?
            for (int y = 0; y < 4; y++) {
                if (!MovementHelper.fullyPassable(dest.up(y))) {
                    return null;
                }
            }
            if (MovementHelper.canWalkOn(dest.down())) {
                return new MovementParkour(src, i, dir);
            }
        }
        if (!context.canSprint()) {
            return null;
        }
        if (!Baritone.settings().allowParkourPlace.get()) {
            return null;
        }
        BlockPos dest = src.offset(dir, 4);
        BlockPos positionToPlace = dest.down();
        IBlockState toPlace = BlockStateInterface.get(positionToPlace);
        if (!context.hasThrowaway()) {
            return null;
        }
        if (toPlace.getBlock() != Blocks.AIR && !BlockStateInterface.isWater(toPlace.getBlock()) && !MovementHelper.isReplacable(positionToPlace, toPlace)) {
            return null;
        }
        for (int i = 0; i < 5; i++) {
            BlockPos against1 = positionToPlace.offset(HORIZONTALS_BUT_ALSO_DOWN_SO_EVERY_DIRECTION_EXCEPT_UP[i]);
            if (against1.up().equals(src.offset(dir, 3))) { // we can't turn around that fast
                continue;
            }
            if (MovementHelper.canPlaceAgainst(against1)) {
                // holy jesus we gonna do it
                MovementParkour ret = new MovementParkour(src, 4, dir);
                ret.override(costFromJumpDistance(4) + context.placeBlockCost());
                return ret;
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
        // MUST BE KEPT IN SYNC WITH generate
        if (!context.canSprint() && dist >= 4) {
            return COST_INF;
        }
        boolean placing = false;
        if (!MovementHelper.canWalkOn(dest.down())) {
            if (dist != 4) {
                return COST_INF;
            }
            if (!Baritone.settings().allowParkourPlace.get()) {
                return COST_INF;
            }
            BlockPos positionToPlace = dest.down();
            IBlockState toPlace = BlockStateInterface.get(positionToPlace);
            if (!context.hasThrowaway()) {
                return COST_INF;
            }
            if (toPlace.getBlock() != Blocks.AIR && !BlockStateInterface.isWater(toPlace.getBlock()) && !MovementHelper.isReplacable(positionToPlace, toPlace)) {
                return COST_INF;
            }
            for (int i = 0; i < 5; i++) {
                BlockPos against1 = positionToPlace.offset(HORIZONTALS_BUT_ALSO_DOWN_SO_EVERY_DIRECTION_EXCEPT_UP[i]);
                if (against1.up().equals(src.offset(direction, 3))) { // we can't turn around that fast
                    continue;
                }
                if (MovementHelper.canPlaceAgainst(against1)) {
                    // holy jesus we gonna do it
                    placing = true;
                    break;
                }
            }
        }
        Block walkOff = BlockStateInterface.get(src.down().offset(direction)).getBlock();
        if (MovementHelper.avoidWalkingInto(walkOff) && walkOff != Blocks.WATER && walkOff != Blocks.FLOWING_WATER) {
            return COST_INF;
        }
        for (int i = 1; i <= 4; i++) {
            BlockPos d = src.offset(direction, i);
            for (int y = 0; y < (i == 1 ? 3 : 4); y++) {
                if (!MovementHelper.fullyPassable(d.up(y))) {
                    return COST_INF;
                }
            }
            if (d.equals(dest)) {
                return costFromJumpDistance(i) + (placing ? context.placeBlockCost() : 0);
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

                if (!MovementHelper.canWalkOn(dest.down())) {
                    BlockPos positionToPlace = dest.down();
                    for (int i = 0; i < 5; i++) {
                        BlockPos against1 = positionToPlace.offset(HORIZONTALS_BUT_ALSO_DOWN_SO_EVERY_DIRECTION_EXCEPT_UP[i]);
                        if (against1.up().equals(src.offset(direction, 3))) { // we can't turn around that fast
                            continue;
                        }
                        if (MovementHelper.canPlaceAgainst(against1)) {
                            if (!MovementHelper.throwaway(true)) {//get ready to place a throwaway block
                                return state.setStatus(MovementState.MovementStatus.UNREACHABLE);
                            }
                            double faceX = (dest.getX() + against1.getX() + 1.0D) * 0.5D;
                            double faceY = (dest.getY() + against1.getY()) * 0.5D;
                            double faceZ = (dest.getZ() + against1.getZ() + 1.0D) * 0.5D;
                            state.setTarget(new MovementState.MovementTarget(Utils.calcRotationFromVec3d(playerHead(), new Vec3d(faceX, faceY, faceZ), playerRotations()), true));
                            EnumFacing side = Minecraft.getMinecraft().objectMouseOver.sideHit;

                            LookBehaviorUtils.getSelectedBlock().ifPresent(selectedBlock -> {
                                if (Objects.equals(selectedBlock, against1) && selectedBlock.offset(side).equals(dest.down())) {
                                    state.setInput(InputOverrideHandler.Input.CLICK_RIGHT, true);
                                }
                            });
                        }
                    }
                }

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