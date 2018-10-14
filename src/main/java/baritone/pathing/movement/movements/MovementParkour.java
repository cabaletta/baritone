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
import baritone.api.pathing.movement.MovementStatus;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.Rotation;
import baritone.behavior.LookBehaviorUtils;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.Movement;
import baritone.pathing.movement.MovementHelper;
import baritone.pathing.movement.MovementState;
import baritone.utils.*;
import baritone.utils.pathing.MutableMoveResult;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

import java.util.Objects;

public class MovementParkour extends Movement {

    private static final EnumFacing[] HORIZONTALS_BUT_ALSO_DOWN____SO_EVERY_DIRECTION_EXCEPT_UP = {EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.EAST, EnumFacing.WEST, EnumFacing.DOWN};
    private static final BetterBlockPos[] EMPTY = new BetterBlockPos[]{};

    private final EnumFacing direction;
    private final int dist;

    private MovementParkour(BetterBlockPos src, int dist, EnumFacing dir) {
        super(src, src.offset(dir, dist), EMPTY, src.offset(dir, dist).down());
        this.direction = dir;
        this.dist = dist;
    }

    public static MovementParkour cost(CalculationContext context, BetterBlockPos src, EnumFacing direction) {
        MutableMoveResult res = new MutableMoveResult();
        cost(context, src.x, src.y, src.z, direction, res);
        int dist = Math.abs(res.x - src.x) + Math.abs(res.z - src.z);
        return new MovementParkour(src, dist, direction);
    }

    public static void cost(CalculationContext context, int x, int y, int z, EnumFacing dir, MutableMoveResult res) {
        if (!Baritone.settings().allowParkour.get()) {
            return;
        }
        IBlockState standingOn = BlockStateInterface.get(x, y - 1, z);
        if (standingOn.getBlock() == Blocks.VINE || standingOn.getBlock() == Blocks.LADDER || MovementHelper.isBottomSlab(standingOn)) {
            return;
        }
        int xDiff = dir.getXOffset();
        int zDiff = dir.getZOffset();
        IBlockState adj = BlockStateInterface.get(x + xDiff, y - 1, z + zDiff);
        if (MovementHelper.avoidWalkingInto(adj.getBlock()) && adj.getBlock() != Blocks.WATER && adj.getBlock() != Blocks.FLOWING_WATER) { // magma sucks
            return;
        }
        if (MovementHelper.canWalkOn(x + xDiff, y - 1, z + zDiff, adj)) { // don't parkour if we could just traverse (for now)
            return;
        }

        if (!MovementHelper.fullyPassable(x + xDiff, y, z + zDiff)) {
            return;
        }
        if (!MovementHelper.fullyPassable(x + xDiff, y + 1, z + zDiff)) {
            return;
        }
        if (!MovementHelper.fullyPassable(x + xDiff, y + 2, z + zDiff)) {
            return;
        }
        if (!MovementHelper.fullyPassable(x, y + 2, z)) {
            return;
        }
        for (int i = 2; i <= (context.canSprint() ? 4 : 3); i++) {
            // TODO perhaps dest.up(3) doesn't need to be fullyPassable, just canWalkThrough, possibly?
            for (int y2 = 0; y2 < 4; y2++) {
                if (!MovementHelper.fullyPassable(x + xDiff * i, y + y2, z + zDiff * i)) {
                    return;
                }
            }
            if (MovementHelper.canWalkOn(x + xDiff * i, y - 1, z + zDiff * i)) {
                res.x = x + xDiff * i;
                res.y = y;
                res.z = z + zDiff * i;
                res.cost = costFromJumpDistance(i);
                return;
            }
        }
        if (!context.canSprint()) {
            return;
        }
        if (!Baritone.settings().allowParkourPlace.get()) {
            return;
        }
        if (!Baritone.settings().allowPlace.get()) {
            Helper.HELPER.logDirect("allowParkourPlace enabled but allowPlace disabled?");
            return;
        }
        int destX = x + 4 * xDiff;
        int destZ = z + 4 * zDiff;
        IBlockState toPlace = BlockStateInterface.get(destX, y - 1, destZ);
        if (!context.canPlaceThrowawayAt(destX, y - 1, destZ)) {
            return;
        }
        if (toPlace.getBlock() != Blocks.AIR && !BlockStateInterface.isWater(toPlace.getBlock()) && !MovementHelper.isReplacable(destX, y - 1, destZ, toPlace)) {
            return;
        }
        for (int i = 0; i < 5; i++) {
            int againstX = destX + HORIZONTALS_BUT_ALSO_DOWN____SO_EVERY_DIRECTION_EXCEPT_UP[i].getXOffset();
            int againstZ = destZ + HORIZONTALS_BUT_ALSO_DOWN____SO_EVERY_DIRECTION_EXCEPT_UP[i].getZOffset();
            if (againstX == x + xDiff * 3 && againstZ == z + zDiff * 3) { // we can't turn around that fast
                continue;
            }
            if (MovementHelper.canPlaceAgainst(againstX, y - 1, againstZ)) {
                res.x = destX;
                res.y = y;
                res.z = destZ;
                res.cost = costFromJumpDistance(4) + context.placeBlockCost();
                return;
            }
        }
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
        MutableMoveResult res = new MutableMoveResult();
        cost(context, src.x, src.y, src.z, direction, res);
        if (res.x != dest.x || res.z != dest.z) {
            return COST_INF;
        }
        return res.cost;
    }

    @Override
    public boolean safeToCancel(MovementState state) {
        // once this movement is instantiated, the state is default to PREPPING
        // but once it's ticked for the first time it changes to RUNNING
        // since we don't really know anything about momentum, it suffices to say Parkour can only be canceled on the 0th tick
        return state.getStatus() != MovementStatus.RUNNING;
    }

    @Override
    public MovementState updateState(MovementState state) {
        super.updateState(state);
        if (state.getStatus() != MovementStatus.RUNNING) {
            return state;
        }
        if (dist >= 4) {
            state.setInput(InputOverrideHandler.Input.SPRINT, true);
        }
        MovementHelper.moveTowards(state, dest);
        if (playerFeet().equals(dest)) {
            Block d = BlockStateInterface.getBlock(dest);
            if (d == Blocks.VINE || d == Blocks.LADDER) {
                // it physically hurt me to add support for parkour jumping onto a vine
                // but i did it anyway
                return state.setStatus(MovementStatus.SUCCESS);
            }
            if (player().posY - playerFeet().getY() < 0.094) { // lilypads
                state.setStatus(MovementStatus.SUCCESS);
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
                                return state.setStatus(MovementStatus.UNREACHABLE);
                            }
                            double faceX = (dest.getX() + against1.getX() + 1.0D) * 0.5D;
                            double faceY = (dest.getY() + against1.getY()) * 0.5D;
                            double faceZ = (dest.getZ() + against1.getZ() + 1.0D) * 0.5D;
                            Rotation place = Utils.calcRotationFromVec3d(playerHead(), new Vec3d(faceX, faceY, faceZ), playerRotations());
                            RayTraceResult res = RayTraceUtils.rayTraceTowards(place);
                            if (res != null && res.typeOfHit == RayTraceResult.Type.BLOCK && res.getBlockPos().equals(against1) && res.getBlockPos().offset(res.sideHit).equals(dest.down())) {
                                state.setTarget(new MovementState.MovementTarget(place, true));
                            }
                            LookBehaviorUtils.getSelectedBlock().ifPresent(selectedBlock -> {
                                EnumFacing side = Minecraft.getMinecraft().objectMouseOver.sideHit;
                                if (Objects.equals(selectedBlock, against1) && selectedBlock.offset(side).equals(dest.down())) {
                                    state.setInput(InputOverrideHandler.Input.CLICK_RIGHT, true);
                                }
                            });
                        }
                    }
                }
                if (dist == 3) { // this is a 2 block gap, dest = src + direction * 3
                    double xDiff = (src.x + 0.5) - player().posX;
                    double zDiff = (src.z + 0.5) - player().posZ;
                    double distFromStart = Math.max(Math.abs(xDiff), Math.abs(zDiff));
                    if (distFromStart < 0.7) {
                        return state;
                    }
                }

                state.setInput(InputOverrideHandler.Input.JUMP, true);
            } else if (!playerFeet().equals(dest.offset(direction, -1))) {
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