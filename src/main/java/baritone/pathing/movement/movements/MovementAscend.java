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
import baritone.utils.RayTraceUtils;
import baritone.utils.RotationUtils;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.Movement;
import baritone.pathing.movement.MovementHelper;
import baritone.pathing.movement.MovementState;
import baritone.utils.BlockStateInterface;
import baritone.utils.InputOverrideHandler;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Objects;

public class MovementAscend extends Movement {

    private int ticksWithoutPlacement = 0;

    public MovementAscend(BetterBlockPos src, BetterBlockPos dest) {
        super(src, dest, new BetterBlockPos[]{dest, src.up(2), dest.up()}, dest.down());
    }

    @Override
    public void reset() {
        super.reset();
        ticksWithoutPlacement = 0;
    }

    @Override
    protected double calculateCost(CalculationContext context) {
        return cost(context, src.x, src.y, src.z, dest.x, dest.z);
    }

    public static double cost(CalculationContext context, int x, int y, int z, int destX, int destZ) {
        IBlockState srcDown = BlockStateInterface.get(x, y - 1, z);
        if (srcDown.getBlock() == Blocks.LADDER || srcDown.getBlock() == Blocks.VINE) {
            return COST_INF;
        }
        // we can jump from soul sand, but not from a bottom slab
        boolean jumpingFromBottomSlab = MovementHelper.isBottomSlab(srcDown);
        IBlockState toPlace = BlockStateInterface.get(destX, y, destZ);
        boolean jumpingToBottomSlab = MovementHelper.isBottomSlab(toPlace);

        if (jumpingFromBottomSlab && !jumpingToBottomSlab) {
            return COST_INF;// the only thing we can ascend onto from a bottom slab is another bottom slab
        }
        boolean hasToPlace = false;
        if (!MovementHelper.canWalkOn(destX, y, destZ, toPlace)) {
            if (!context.canPlaceThrowawayAt(destX, y, destZ)) {
                return COST_INF;
            }
            if (toPlace.getBlock() != Blocks.AIR && !MovementHelper.isWater(toPlace.getBlock()) && !MovementHelper.isReplacable(destX, y, destZ, toPlace)) {
                return COST_INF;
            }
            // TODO: add ability to place against .down() as well as the cardinal directions
            // useful for when you are starting a staircase without anything to place against
            // Counterpoint to the above TODO ^ you should move then pillar instead of ascend
            for (int i = 0; i < 4; i++) {
                int againstX = destX + HORIZONTALS[i].getXOffset();
                int againstZ = destZ + HORIZONTALS[i].getZOffset();
                if (againstX == x && againstZ == z) {
                    continue;
                }
                if (MovementHelper.canPlaceAgainst(againstX, y, againstZ)) {
                    hasToPlace = true;
                    break;
                }
            }
            if (!hasToPlace) { // didn't find a valid place =(
                return COST_INF;
            }
        }
        IBlockState srcUp2 = null;
        if (BlockStateInterface.get(x, y + 3, z).getBlock() instanceof BlockFalling && (MovementHelper.canWalkThrough(x, y + 1, z) || !((srcUp2 = BlockStateInterface.get(x, y + 2, z)).getBlock() instanceof BlockFalling))) {//it would fall on us and possibly suffocate us
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
        double walk;
        if (jumpingToBottomSlab) {
            if (jumpingFromBottomSlab) {
                walk = Math.max(JUMP_ONE_BLOCK_COST, WALK_ONE_BLOCK_COST); // we hit space immediately on entering this action
            } else {
                walk = WALK_ONE_BLOCK_COST; // we don't hit space we just walk into the slab
            }
        } else {
            if (toPlace.getBlock() == Blocks.SOUL_SAND) {
                walk = WALK_ONE_OVER_SOUL_SAND_COST;
            } else {
                walk = WALK_ONE_BLOCK_COST;
            }
        }

        // cracks knuckles

        double totalCost = 0;
        totalCost += walk;
        if (hasToPlace) {
            totalCost += context.placeBlockCost();
        }
        if (srcUp2 == null) {
            srcUp2 = BlockStateInterface.get(x, y + 2, z);
        }
        totalCost += MovementHelper.getMiningDurationTicks(context, x, y + 2, z, srcUp2, false); // TODO MAKE ABSOLUTELY SURE we don't need includeFalling here, from the falling check above
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

        if (playerFeet().equals(dest)) {
            return state.setStatus(MovementStatus.SUCCESS);
        }

        IBlockState jumpingOnto = BlockStateInterface.get(positionToPlace);
        if (!MovementHelper.canWalkOn(positionToPlace, jumpingOnto)) {
            for (int i = 0; i < 4; i++) {
                BlockPos anAgainst = positionToPlace.offset(HORIZONTALS[i]);
                if (anAgainst.equals(src)) {
                    continue;
                }
                if (MovementHelper.canPlaceAgainst(anAgainst)) {
                    if (!MovementHelper.throwaway(true)) {//get ready to place a throwaway block
                        return state.setStatus(MovementStatus.UNREACHABLE);
                    }
                    double faceX = (dest.getX() + anAgainst.getX() + 1.0D) * 0.5D;
                    double faceY = (dest.getY() + anAgainst.getY()) * 0.5D;
                    double faceZ = (dest.getZ() + anAgainst.getZ() + 1.0D) * 0.5D;
                    state.setTarget(new MovementState.MovementTarget(RotationUtils.calcRotationFromVec3d(playerHead(), new Vec3d(faceX, faceY, faceZ), playerRotations()), true));
                    EnumFacing side = Minecraft.getMinecraft().objectMouseOver.sideHit;

                    RayTraceUtils.getSelectedBlock().ifPresent(selectedBlock -> {
                        if (Objects.equals(selectedBlock, anAgainst) && selectedBlock.offset(side).equals(positionToPlace)) {
                            ticksWithoutPlacement++;
                            state.setInput(InputOverrideHandler.Input.SNEAK, true);
                            if (player().isSneaking()) {
                                state.setInput(InputOverrideHandler.Input.CLICK_RIGHT, true);
                            }
                            if (ticksWithoutPlacement > 10) {
                                // After 10 ticks without placement, we might be standing in the way, move back
                                state.setInput(InputOverrideHandler.Input.MOVE_BACK, true);
                            }
                        } else {
                            state.setInput(InputOverrideHandler.Input.CLICK_LEFT, true); // break whatever replaceable block is in the way
                        }
                        //System.out.println("Trying to look at " + anAgainst + ", actually looking at" + selectedBlock);
                    });
                    return state;
                }
            }
            return state.setStatus(MovementStatus.UNREACHABLE);
        }
        MovementHelper.moveTowards(state, dest);
        if (MovementHelper.isBottomSlab(jumpingOnto) && !MovementHelper.isBottomSlab(src.down())) {
            return state; // don't jump while walking from a non double slab into a bottom slab
        }

        if (Baritone.settings().assumeStep.get()) {
            return state;
        }

        if (headBonkClear()) {
            return state.setInput(InputOverrideHandler.Input.JUMP, true);
        }

        int xAxis = Math.abs(src.getX() - dest.getX()); // either 0 or 1
        int zAxis = Math.abs(src.getZ() - dest.getZ()); // either 0 or 1
        double flatDistToNext = xAxis * Math.abs((dest.getX() + 0.5D) - player().posX) + zAxis * Math.abs((dest.getZ() + 0.5D) - player().posZ);
        double sideDist = zAxis * Math.abs((dest.getX() + 0.5D) - player().posX) + xAxis * Math.abs((dest.getZ() + 0.5D) - player().posZ);
        // System.out.println(flatDistToNext + " " + sideDist);
        if (flatDistToNext > 1.2 || sideDist > 0.2) {
            return state;
        }

        // Once we are pointing the right way and moving, start jumping
        // This is slightly more efficient because otherwise we might start jumping before moving, and fall down without moving onto the block we want to jump onto
        // Also wait until we are close enough, because we might jump and hit our head on an adjacent block
        return state.setInput(InputOverrideHandler.Input.JUMP, true);
    }

    private boolean headBonkClear() {
        BetterBlockPos startUp = src.up(2);
        for (int i = 0; i < 4; i++) {
            BetterBlockPos check = startUp.offset(EnumFacing.byHorizontalIndex(i));
            if (!MovementHelper.canWalkThrough(check)) {
                // We might bonk our head
                return false;
            }
        }
        return true;
    }
}
