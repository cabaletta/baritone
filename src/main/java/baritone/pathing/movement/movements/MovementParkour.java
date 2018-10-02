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
import baritone.utils.pathing.MoveResult;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Objects;

import static baritone.utils.pathing.MoveResult.IMPOSSIBLE;

public class MovementParkour extends Movement {

    private static final EnumFacing[] HORIZONTALS_BUT_ALSO_DOWN____SO_EVERY_DIRECTION_EXCEPT_UP = {EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.EAST, EnumFacing.WEST, EnumFacing.DOWN};
    private static final BetterBlockPos[] EMPTY = new BetterBlockPos[]{};

    private final EnumFacing direction;
    private final int dist;

    private MovementParkour(BetterBlockPos src, int dist, EnumFacing dir) {
        super(src, src.offset(dir, dist), EMPTY);
        this.direction = dir;
        this.dist = dist;
    }

    public static MovementParkour cost(CalculationContext context, BetterBlockPos src, EnumFacing direction) {
        MoveResult res = cost(context, src.x, src.y, src.z, direction);
        int dist = Math.abs(res.destX - src.x) + Math.abs(res.destZ - src.z);
        return new MovementParkour(src, dist, direction);
    }

    public static MoveResult cost(CalculationContext context, int x, int y, int z, EnumFacing dir) {
        if (!Baritone.settings().allowParkour.get()) {
            return IMPOSSIBLE;
        }
        IBlockState standingOn = BlockStateInterface.get(x, y - 1, z);
        if (standingOn.getBlock() == Blocks.VINE || standingOn.getBlock() == Blocks.LADDER || MovementHelper.isBottomSlab(standingOn)) {
            return IMPOSSIBLE;
        }
        int xDiff = dir.getXOffset();
        int zDiff = dir.getZOffset();
        IBlockState adj = BlockStateInterface.get(x + xDiff, y - 1, z + zDiff);
        if (MovementHelper.avoidWalkingInto(adj.getBlock()) && adj.getBlock() != Blocks.WATER && adj.getBlock() != Blocks.FLOWING_WATER) { // magma sucks
            return IMPOSSIBLE;
        }
        if (MovementHelper.canWalkOn(x + xDiff, y - 1, z + zDiff, adj)) { // don't parkour if we could just traverse (for now)
            return IMPOSSIBLE;
        }

        if (!MovementHelper.fullyPassable(x + xDiff, y, z + zDiff)) {
            return IMPOSSIBLE;
        }
        if (!MovementHelper.fullyPassable(x + xDiff, y + 1, z + zDiff)) {
            return IMPOSSIBLE;
        }
        if (!MovementHelper.fullyPassable(x + xDiff, y + 2, z + zDiff)) {
            return IMPOSSIBLE;
        }
        if (!MovementHelper.fullyPassable(x, y + 2, z)) {
            return IMPOSSIBLE;
        }
        for (int i = 2; i <= (context.canSprint() ? 4 : 3); i++) {
            // TODO perhaps dest.up(3) doesn't need to be fullyPassable, just canWalkThrough, possibly?
            for (int y2 = 0; y2 < 4; y2++) {
                if (!MovementHelper.fullyPassable(x + xDiff * i, y + y2, z + zDiff * i)) {
                    return IMPOSSIBLE;
                }
            }
            if (MovementHelper.canWalkOn(x + xDiff * i, y - 1, z + zDiff * i)) {
                return new MoveResult(x + xDiff * i, y, z + zDiff * i, costFromJumpDistance(i));
            }
        }
        if (!context.canSprint()) {
            return IMPOSSIBLE;
        }
        if (!Baritone.settings().allowParkourPlace.get()) {
            return IMPOSSIBLE;
        }
        int destX = x + 4 * xDiff;
        int destZ = z + 4 * zDiff;
        IBlockState toPlace = BlockStateInterface.get(destX, y - 1, destZ);
        if (!context.hasThrowaway()) {
            return IMPOSSIBLE;
        }
        if (toPlace.getBlock() != Blocks.AIR && !BlockStateInterface.isWater(toPlace.getBlock()) && !MovementHelper.isReplacable(destX, y - 1, destZ, toPlace)) {
            return IMPOSSIBLE;
        }
        for (int i = 0; i < 5; i++) {
            int againstX = destX + HORIZONTALS_BUT_ALSO_DOWN____SO_EVERY_DIRECTION_EXCEPT_UP[i].getXOffset();
            int againstZ = destZ + HORIZONTALS_BUT_ALSO_DOWN____SO_EVERY_DIRECTION_EXCEPT_UP[i].getZOffset();
            if (againstX == x + xDiff * 3 && againstZ == z + zDiff * 3) { // we can't turn around that fast
                continue;
            }
            if (MovementHelper.canPlaceAgainst(againstX, y - 1, againstZ)) {
                return new MoveResult(destX, y, destZ, costFromJumpDistance(4) + context.placeBlockCost());
            }
        }
        return IMPOSSIBLE;
    }

    private static double costFromJumpDistance(int dist) {
        switch (dist) {
            case 2:
                return WALK_ONE_BLOCK_COST * 2; // IDK LOL
            case 3:
                return WALK_ONE_BLOCK_COST * 3;
            case 4:
                return SPRINT_ONE_BLOCK_COST * 4;
            default:
                throw new IllegalStateException("LOL " + dist);
        }
    }


    @Override
    protected double calculateCost(CalculationContext context) {
        MoveResult res = cost(context, src.x, src.y, src.z, direction);
        if (res.destX != dest.x || res.destZ != dest.z) {
            return COST_INF;
        }
        return res.cost;
    }

    @Override
    public MovementState updateState(MovementState state) {
        super.updateState(state);
        if (state.getStatus() != MovementState.MovementStatus.RUNNING) {
            return state;
        }
        if (dist >= 4) {
            state.setInput(InputOverrideHandler.Input.SPRINT, true);
        }
        MovementHelper.moveTowards(state, dest);
        if (playerFeet().equals(dest)) {
            if (player().posY - playerFeet().getY() < 0.094) { // lilypads
                state.setStatus(MovementState.MovementStatus.SUCCESS);
            }
        } else if (!playerFeet().equals(src)) {
            if (playerFeet().equals(src.offset(direction)) || player().posY - playerFeet().getY() > 0.0001) {

                if (!MovementHelper.canWalkOn(dest.down())) {
                    BlockPos positionToPlace = dest.down();
                    for (int i = 0; i < 5; i++) {
                        BlockPos against1 = positionToPlace.offset(HORIZONTALS_BUT_ALSO_DOWN____SO_EVERY_DIRECTION_EXCEPT_UP[i]);
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