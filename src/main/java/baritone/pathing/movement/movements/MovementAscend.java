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
import baritone.api.IBaritone;
import baritone.api.pathing.movement.MovementStatus;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.RotationUtils;
import baritone.api.utils.input.Input;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.Movement;
import baritone.pathing.movement.MovementHelper;
import baritone.pathing.movement.MovementState;
import baritone.utils.BlockStateInterface;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Objects;

public class MovementAscend extends Movement {

    private int ticksWithoutPlacement = 0;

    public MovementAscend(IBaritone baritone, BetterBlockPos src, BetterBlockPos dest) {
        super(baritone, src, dest, new BetterBlockPos[]{dest, src.up(2), dest.up()}, dest.down());
    }

    @Override
    public void reset() {
        super.reset();
        ticksWithoutPlacement = 0;
    }

    @Override
    public double calculateCost(CalculationContext context) {
        return cost(context, src.x, src.y, src.z, dest.x, dest.z);
    }

    public static double cost(CalculationContext context, int x, int y, int z, int destX, int destZ) {
        IBlockState toPlace = context.get(destX, y, destZ);
        double additionalPlacementCost = 0;
        if (!MovementHelper.canWalkOn(context.bsi, destX, y, destZ, toPlace)) {
            additionalPlacementCost = context.costOfPlacingAt(destX, y, destZ);
            if (additionalPlacementCost >= COST_INF) {
                return COST_INF;
            }
            if (!MovementHelper.isReplacable(destX, y, destZ, toPlace, context.bsi)) {
                return COST_INF;
            }
            boolean foundPlaceOption = false;
            for (int i = 0; i < 5; i++) {
                int againstX = destX + HORIZONTALS_BUT_ALSO_DOWN____SO_EVERY_DIRECTION_EXCEPT_UP[i].getXOffset();
                int againstY = y + HORIZONTALS_BUT_ALSO_DOWN____SO_EVERY_DIRECTION_EXCEPT_UP[i].getYOffset();
                int againstZ = destZ + HORIZONTALS_BUT_ALSO_DOWN____SO_EVERY_DIRECTION_EXCEPT_UP[i].getZOffset();
                if (againstX == x && againstZ == z) { // we might be able to backplace now, but it doesn't matter because it will have been broken by the time we'd need to use it
                    continue;
                }
                if (MovementHelper.canPlaceAgainst(context.bsi, againstX, againstY, againstZ)) {
                    foundPlaceOption = true;
                    break;
                }
            }
            if (!foundPlaceOption) { // didn't find a valid place =(
                return COST_INF;
            }
        }
        IBlockState srcUp2 = context.get(x, y + 2, z); // used lower down anyway
        if (context.get(x, y + 3, z).getBlock() instanceof BlockFalling && (MovementHelper.canWalkThrough(context.bsi, x, y + 1, z) || !(srcUp2.getBlock() instanceof BlockFalling))) {//it would fall on us and possibly suffocate us
            // HOWEVER, we assume that we're standing in the start position
            // that means that src and src.up(1) are both air
            // maybe they aren't now, but they will be by the time this starts
            // if the lower one is can't walk through and the upper one is falling, that means that by standing on src
            // (the presupposition of this Movement)
            // we have necessarily already cleared the entire BlockFalling stack
            // on top of our head

            // as in, if we have a block, then two BlockFallings on top of it
            // and that block is x, y+1, z, and we'd have to clear it to even start this movement
            // we don't need to worry about those BlockFallings because we've already cleared them
            return COST_INF;
            // you may think we only need to check srcUp2, not srcUp
            // however, in the scenario where glitchy world gen where unsupported sand / gravel generates
            // it's possible srcUp is AIR from the start, and srcUp2 is falling
            // and in that scenario, when we arrive and break srcUp2, that lets srcUp3 fall on us and suffocate us
        }
        IBlockState srcDown = context.get(x, y - 1, z);
        if (srcDown.getBlock() == Blocks.LADDER || srcDown.getBlock() == Blocks.VINE) {
            return COST_INF;
        }
        // we can jump from soul sand, but not from a bottom slab
        boolean jumpingFromBottomSlab = MovementHelper.isBottomSlab(srcDown);
        boolean jumpingToBottomSlab = MovementHelper.isBottomSlab(toPlace);
        if (jumpingFromBottomSlab && !jumpingToBottomSlab) {
            return COST_INF;// the only thing we can ascend onto from a bottom slab is another bottom slab
        }
        double walk;
        if (jumpingToBottomSlab) {
            if (jumpingFromBottomSlab) {
                walk = Math.max(JUMP_ONE_BLOCK_COST, WALK_ONE_BLOCK_COST); // we hit space immediately on entering this action
                walk += context.jumpPenalty;
            } else {
                walk = WALK_ONE_BLOCK_COST; // we don't hit space we just walk into the slab
            }
        } else {
            // jumpingFromBottomSlab must be false
            if (toPlace.getBlock() == Blocks.SOUL_SAND) {
                walk = WALK_ONE_OVER_SOUL_SAND_COST;
            } else {
                walk = Math.max(JUMP_ONE_BLOCK_COST, WALK_ONE_BLOCK_COST);
            }
            walk += context.jumpPenalty;
        }

        double totalCost = walk + additionalPlacementCost;
        // start with srcUp2 since we already have its state
        // includeFalling isn't needed because of the falling check above -- if srcUp3 is falling we will have already exited with COST_INF if we'd actually have to break it
        totalCost += MovementHelper.getMiningDurationTicks(context, x, y + 2, z, srcUp2, false);
        if (totalCost >= COST_INF) {
            return COST_INF;
        }
        totalCost += MovementHelper.getMiningDurationTicks(context, destX, y + 1, destZ, false);
        if (totalCost >= COST_INF) {
            return COST_INF;
        }
        totalCost += MovementHelper.getMiningDurationTicks(context, destX, y + 2, destZ, true);
        return totalCost;
    }

    @Override
    public MovementState updateState(MovementState state) {
        super.updateState(state);
        // TODO incorporate some behavior from ActionClimb (specifically how it waited until it was at most 1.2 blocks away before starting to jump
        // for efficiency in ascending minimal height staircases, which is just repeated MovementAscend, so that it doesn't bonk its head on the ceiling repeatedly)
        if (state.getStatus() != MovementStatus.RUNNING) {
            return state;
        }

        if (ctx.playerFeet().equals(dest)) {
            return state.setStatus(MovementStatus.SUCCESS);
        }

        IBlockState jumpingOnto = BlockStateInterface.get(ctx, positionToPlace);
        if (!MovementHelper.canWalkOn(ctx, positionToPlace, jumpingOnto)) {
            for (int i = 0; i < 5; i++) {
                BlockPos anAgainst = positionToPlace.offset(HORIZONTALS_BUT_ALSO_DOWN____SO_EVERY_DIRECTION_EXCEPT_UP[i]);
                if (anAgainst.equals(src)) {
                    continue;
                }
                if (MovementHelper.canPlaceAgainst(ctx, anAgainst)) {
                    if (!MovementHelper.throwaway(ctx, true)) {//get ready to place a throwaway block
                        return state.setStatus(MovementStatus.UNREACHABLE);
                    }
                    double faceX = (dest.getX() + anAgainst.getX() + 1.0D) * 0.5D;
                    double faceY = (dest.getY() + anAgainst.getY()) * 0.5D;
                    double faceZ = (dest.getZ() + anAgainst.getZ() + 1.0D) * 0.5D;
                    state.setTarget(new MovementState.MovementTarget(RotationUtils.calcRotationFromVec3d(ctx.playerHead(), new Vec3d(faceX, faceY, faceZ), ctx.playerRotations()), true));
                    EnumFacing side = ctx.objectMouseOver().sideHit;

                    ctx.getSelectedBlock().ifPresent(selectedBlock -> {
                        if (Objects.equals(selectedBlock, anAgainst) && selectedBlock.offset(side).equals(positionToPlace)) {
                            ticksWithoutPlacement++;
                            state.setInput(Input.SNEAK, true);
                            if (ctx.player().isSneaking()) {
                                state.setInput(Input.CLICK_RIGHT, true);
                            }
                            if (ticksWithoutPlacement > 10) {
                                // After 10 ticks without placement, we might be standing in the way, move back
                                state.setInput(Input.MOVE_BACK, true);
                            }
                        } else {
                            state.setInput(Input.CLICK_LEFT, true); // break whatever replaceable block is in the way
                        }
                        //System.out.println("Trying to look at " + anAgainst + ", actually looking at" + selectedBlock);
                    });
                    return state;
                }
            }
            return state.setStatus(MovementStatus.UNREACHABLE);
        }
        MovementHelper.moveTowards(ctx, state, dest);
        if (MovementHelper.isBottomSlab(jumpingOnto) && !MovementHelper.isBottomSlab(BlockStateInterface.get(ctx, src.down()))) {
            return state; // don't jump while walking from a non double slab into a bottom slab
        }

        if (Baritone.settings().assumeStep.get()) {
            return state;
        }

        if (ctx.playerFeet().equals(src.up())) {
            return state; // no need to hit space if we're already jumping
        }

        if (headBonkClear()) {
            return state.setInput(Input.JUMP, true);
        }

        int xAxis = Math.abs(src.getX() - dest.getX()); // either 0 or 1
        int zAxis = Math.abs(src.getZ() - dest.getZ()); // either 0 or 1
        double flatDistToNext = xAxis * Math.abs((dest.getX() + 0.5D) - ctx.player().posX) + zAxis * Math.abs((dest.getZ() + 0.5D) - ctx.player().posZ);
        double sideDist = zAxis * Math.abs((dest.getX() + 0.5D) - ctx.player().posX) + xAxis * Math.abs((dest.getZ() + 0.5D) - ctx.player().posZ);
        // System.out.println(flatDistToNext + " " + sideDist);
        if (flatDistToNext > 1.2 || sideDist > 0.2) {
            return state;
        }

        // Once we are pointing the right way and moving, start jumping
        // This is slightly more efficient because otherwise we might start jumping before moving, and fall down without moving onto the block we want to jump onto
        // Also wait until we are close enough, because we might jump and hit our head on an adjacent block
        return state.setInput(Input.JUMP, true);
    }

    private boolean headBonkClear() {
        BetterBlockPos startUp = src.up(2);
        for (int i = 0; i < 4; i++) {
            BetterBlockPos check = startUp.offset(EnumFacing.byHorizontalIndex(i));
            if (!MovementHelper.canWalkThrough(ctx, check)) {
                // We might bonk our head
                return false;
            }
        }
        return true;
    }
}
