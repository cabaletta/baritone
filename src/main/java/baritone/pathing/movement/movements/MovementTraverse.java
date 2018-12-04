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
import baritone.api.utils.Rotation;
import baritone.api.utils.RotationUtils;
import baritone.api.utils.VecUtils;
import baritone.api.utils.input.Input;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.Movement;
import baritone.pathing.movement.MovementHelper;
import baritone.pathing.movement.MovementState;
import baritone.utils.BlockStateInterface;
import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Objects;

public class MovementTraverse extends Movement {

    /**
     * Did we have to place a bridge block or was it always there
     */
    private boolean wasTheBridgeBlockAlwaysThere = true;

    public MovementTraverse(IBaritone baritone, BetterBlockPos from, BetterBlockPos to) {
        super(baritone, from, to, new BetterBlockPos[]{to.up(), to}, to.down());
    }

    @Override
    public void reset() {
        super.reset();
        wasTheBridgeBlockAlwaysThere = true;
    }

    @Override
    protected double calculateCost(CalculationContext context) {
        return cost(context, src.x, src.y, src.z, dest.x, dest.z);
    }

    public static double cost(CalculationContext context, int x, int y, int z, int destX, int destZ) {
        IBlockState pb0 = context.get(destX, y + 1, destZ);
        IBlockState pb1 = context.get(destX, y, destZ);
        IBlockState destOn = context.get(destX, y - 1, destZ);
        Block srcDown = context.getBlock(x, y - 1, z);
        if (MovementHelper.canWalkOn(context.bsi(), destX, y - 1, destZ, destOn)) {//this is a walk, not a bridge
            double WC = WALK_ONE_BLOCK_COST;
            boolean water = false;
            if (MovementHelper.isWater(pb0.getBlock()) || MovementHelper.isWater(pb1.getBlock())) {
                WC = context.waterWalkSpeed();
                water = true;
            } else {
                if (destOn.getBlock() == Blocks.SOUL_SAND) {
                    WC += (WALK_ONE_OVER_SOUL_SAND_COST - WALK_ONE_BLOCK_COST) / 2;
                }
                if (srcDown == Blocks.SOUL_SAND) {
                    WC += (WALK_ONE_OVER_SOUL_SAND_COST - WALK_ONE_BLOCK_COST) / 2;
                }
            }
            double hardness1 = MovementHelper.getMiningDurationTicks(context, destX, y, destZ, pb1, false);
            if (hardness1 >= COST_INF) {
                return COST_INF;
            }
            double hardness2 = MovementHelper.getMiningDurationTicks(context, destX, y + 1, destZ, pb0, true); // only include falling on the upper block to break
            if (hardness1 == 0 && hardness2 == 0) {
                if (!water && context.canSprint()) {
                    // If there's nothing in the way, and this isn't water, and we aren't sneak placing
                    // We can sprint =D
                    // Don't check for soul sand, since we can sprint on that too
                    WC *= SPRINT_MULTIPLIER;
                }
                return WC;
            }
            if (srcDown == Blocks.LADDER || srcDown == Blocks.VINE) {
                hardness1 *= 5;
                hardness2 *= 5;
            }
            return WC + hardness1 + hardness2;
        } else {//this is a bridge, so we need to place a block
            if (srcDown == Blocks.LADDER || srcDown == Blocks.VINE) {
                return COST_INF;
            }
            if (MovementHelper.isReplacable(destX, y - 1, destZ, destOn, context.bsi())) {
                boolean throughWater = MovementHelper.isWater(pb0.getBlock()) || MovementHelper.isWater(pb1.getBlock());
                if (MovementHelper.isWater(destOn.getBlock()) && throughWater) {
                    // this happens when assume walk on water is true and this is a traverse in water, which isn't allowed
                    return COST_INF;
                }
                if (!context.canPlaceThrowawayAt(destX, y - 1, destZ)) {
                    return COST_INF;
                }
                double hardness1 = MovementHelper.getMiningDurationTicks(context, destX, y, destZ, pb1, false);
                if (hardness1 >= COST_INF) {
                    return COST_INF;
                }
                double hardness2 = MovementHelper.getMiningDurationTicks(context, destX, y + 1, destZ, pb0, true); // only include falling on the upper block to break
                double WC = throughWater ? context.waterWalkSpeed() : WALK_ONE_BLOCK_COST;
                for (int i = 0; i < 4; i++) {
                    int againstX = destX + HORIZONTALS[i].getXOffset();
                    int againstZ = destZ + HORIZONTALS[i].getZOffset();
                    if (againstX == x && againstZ == z) { // this would be a backplace
                        continue;
                    }
                    if (MovementHelper.canPlaceAgainst(context.bsi(), againstX, y - 1, againstZ)) { // found a side place option
                        return WC + context.placeBlockCost() + hardness1 + hardness2;
                    }
                }
                // now that we've checked all possible directions to side place, we actually need to backplace
                if (srcDown == Blocks.SOUL_SAND || (srcDown instanceof BlockSlab && !((BlockSlab) srcDown).isDouble())) {
                    return COST_INF; // can't sneak and backplace against soul sand or half slabs =/
                }
                if (srcDown == Blocks.FLOWING_WATER || srcDown == Blocks.WATER) {
                    return COST_INF; // this is obviously impossible
                }
                WC = WC * SNEAK_ONE_BLOCK_COST / WALK_ONE_BLOCK_COST;//since we are sneak backplacing, we are sneaking lol
                return WC + context.placeBlockCost() + hardness1 + hardness2;
            }
            return COST_INF;
        }
    }

    @Override
    public MovementState updateState(MovementState state) {
        super.updateState(state);
        if (state.getStatus() != MovementStatus.RUNNING) {
            // if the setting is enabled
            if (!Baritone.settings().walkWhileBreaking.get()) {
                return state;
            }
            // and if we're prepping (aka mining the block in front)
            if (state.getStatus() != MovementStatus.PREPPING) {
                return state;
            }
            // and if it's fine to walk into the blocks in front
            if (MovementHelper.avoidWalkingInto(BlockStateInterface.get(ctx, positionsToBreak[0]).getBlock())) {
                return state;
            }
            if (MovementHelper.avoidWalkingInto(BlockStateInterface.get(ctx, positionsToBreak[1]).getBlock())) {
                return state;
            }
            // and we aren't already pressed up against the block
            double dist = Math.max(Math.abs(ctx.player().posX - (dest.getX() + 0.5D)), Math.abs(ctx.player().posZ - (dest.getZ() + 0.5D)));
            if (dist < 0.83) {
                return state;
            }

            // combine the yaw to the center of the destination, and the pitch to the specific block we're trying to break
            // it's safe to do this since the two blocks we break (in a traverse) are right on top of each other and so will have the same yaw
            float yawToDest = RotationUtils.calcRotationFromVec3d(ctx.playerHead(), VecUtils.calculateBlockCenter(ctx.world(), dest)).getYaw();
            float pitchToBreak = state.getTarget().getRotation().get().getPitch();

            state.setTarget(new MovementState.MovementTarget(new Rotation(yawToDest, pitchToBreak), true));
            return state.setInput(Input.MOVE_FORWARD, true);
        }

        //sneak may have been set to true in the PREPPING state while mining an adjacent block
        state.setInput(Input.SNEAK, false);

        Block fd = BlockStateInterface.get(ctx, src.down()).getBlock();
        boolean ladder = fd instanceof BlockLadder || fd instanceof BlockVine;
        IBlockState pb0 = BlockStateInterface.get(ctx, positionsToBreak[0]);
        IBlockState pb1 = BlockStateInterface.get(ctx, positionsToBreak[1]);

        boolean door = pb0.getBlock() instanceof BlockDoor || pb1.getBlock() instanceof BlockDoor;
        if (door) {
            boolean isDoorActuallyBlockingUs = false;
            if (pb0.getBlock() instanceof BlockDoor && !MovementHelper.isDoorPassable(ctx, src, dest)) {
                isDoorActuallyBlockingUs = true;
            } else if (pb1.getBlock() instanceof BlockDoor && !MovementHelper.isDoorPassable(ctx, dest, src)) {
                isDoorActuallyBlockingUs = true;
            }
            if (isDoorActuallyBlockingUs && !(Blocks.IRON_DOOR.equals(pb0.getBlock()) || Blocks.IRON_DOOR.equals(pb1.getBlock()))) {
                return state.setTarget(new MovementState.MovementTarget(RotationUtils.calcRotationFromVec3d(ctx.playerHead(), VecUtils.calculateBlockCenter(ctx.world(), positionsToBreak[0])), true))
                        .setInput(Input.CLICK_RIGHT, true);
            }
        }

        if (pb0.getBlock() instanceof BlockFenceGate || pb1.getBlock() instanceof BlockFenceGate) {
            BlockPos blocked = null;
            if (!MovementHelper.isGatePassable(ctx, positionsToBreak[0], src.up())) {
                blocked = positionsToBreak[0];
            } else if (!MovementHelper.isGatePassable(ctx, positionsToBreak[1], src)) {
                blocked = positionsToBreak[1];
            }

            if (blocked != null) {
                return state.setTarget(new MovementState.MovementTarget(RotationUtils.calcRotationFromVec3d(ctx.playerHead(), VecUtils.calculateBlockCenter(ctx.world(), blocked)), true))
                        .setInput(Input.CLICK_RIGHT, true);
            }
        }

        boolean isTheBridgeBlockThere = MovementHelper.canWalkOn(ctx, positionToPlace) || ladder;
        BlockPos whereAmI = ctx.playerFeet();
        if (whereAmI.getY() != dest.getY() && !ladder) {
            logDebug("Wrong Y coordinate");
            if (whereAmI.getY() < dest.getY()) {
                state.setInput(Input.JUMP, true);
            }
            return state;
        }

        if (isTheBridgeBlockThere) {
            if (ctx.playerFeet().equals(dest)) {
                return state.setStatus(MovementStatus.SUCCESS);
            }
            BlockPos into = dest.subtract(src).add(dest);
            Block intoBelow = BlockStateInterface.get(ctx, into).getBlock();
            Block intoAbove = BlockStateInterface.get(ctx, into.up()).getBlock();
            if (wasTheBridgeBlockAlwaysThere && !MovementHelper.isLiquid(ctx, ctx.playerFeet()) && !MovementHelper.avoidWalkingInto(intoBelow) && !MovementHelper.avoidWalkingInto(intoAbove)) {
                state.setInput(Input.SPRINT, true);
            }
            Block destDown = BlockStateInterface.get(ctx, dest.down()).getBlock();
            if (whereAmI.getY() != dest.getY() && ladder && (destDown instanceof BlockVine || destDown instanceof BlockLadder)) {
                new MovementPillar(baritone, dest.down(), dest).updateState(state); // i'm sorry
                return state;
            }
            MovementHelper.moveTowards(ctx, state, positionsToBreak[0]);
            return state;
        } else {
            wasTheBridgeBlockAlwaysThere = false;
            for (int i = 0; i < 4; i++) {
                BlockPos against1 = dest.offset(HORIZONTALS[i]);
                if (against1.equals(src)) {
                    continue;
                }
                against1 = against1.down();
                if (MovementHelper.canPlaceAgainst(ctx, against1)) {
                    if (!MovementHelper.throwaway(ctx, true)) { // get ready to place a throwaway block
                        logDebug("bb pls get me some blocks. dirt or cobble");
                        return state.setStatus(MovementStatus.UNREACHABLE);
                    }
                    if (!Baritone.settings().assumeSafeWalk.get()) {
                        state.setInput(Input.SNEAK, true);
                    }
                    Block standingOn = BlockStateInterface.get(ctx, ctx.playerFeet().down()).getBlock();
                    if (standingOn.equals(Blocks.SOUL_SAND) || standingOn instanceof BlockSlab) { // see issue #118
                        double dist = Math.max(Math.abs(dest.getX() + 0.5 - ctx.player().posX), Math.abs(dest.getZ() + 0.5 - ctx.player().posZ));
                        if (dist < 0.85) { // 0.5 + 0.3 + epsilon
                            MovementHelper.moveTowards(ctx, state, dest);
                            return state.setInput(Input.MOVE_FORWARD, false)
                                    .setInput(Input.MOVE_BACK, true);
                        }
                    }
                    state.setInput(Input.MOVE_BACK, false);
                    double faceX = (dest.getX() + against1.getX() + 1.0D) * 0.5D;
                    double faceY = (dest.getY() + against1.getY()) * 0.5D;
                    double faceZ = (dest.getZ() + against1.getZ() + 1.0D) * 0.5D;
                    state.setTarget(new MovementState.MovementTarget(RotationUtils.calcRotationFromVec3d(ctx.playerHead(), new Vec3d(faceX, faceY, faceZ), ctx.playerRotations()), true));

                    EnumFacing side = ctx.objectMouseOver().sideHit;
                    if (Objects.equals(ctx.getSelectedBlock().orElse(null), against1) && (ctx.player().isSneaking() || Baritone.settings().assumeSafeWalk.get()) && ctx.getSelectedBlock().get().offset(side).equals(positionToPlace)) {
                        return state.setInput(Input.CLICK_RIGHT, true);
                    }
                    //System.out.println("Trying to look at " + against1 + ", actually looking at" + RayTraceUtils.getSelectedBlock());
                    return state.setInput(Input.CLICK_LEFT, true);
                }
            }
            if (!Baritone.settings().assumeSafeWalk.get()) {
                state.setInput(Input.SNEAK, true);
            }
            if (whereAmI.equals(dest)) {
                // If we are in the block that we are trying to get to, we are sneaking over air and we need to place a block beneath us against the one we just walked off of
                // Out.log(from + " " + to + " " + faceX + "," + faceY + "," + faceZ + " " + whereAmI);
                if (!MovementHelper.throwaway(ctx, true)) {// get ready to place a throwaway block
                    logDebug("bb pls get me some blocks. dirt or cobble");
                    return state.setStatus(MovementStatus.UNREACHABLE);
                }
                double faceX = (dest.getX() + src.getX() + 1.0D) * 0.5D;
                double faceY = (dest.getY() + src.getY() - 1.0D) * 0.5D;
                double faceZ = (dest.getZ() + src.getZ() + 1.0D) * 0.5D;
                // faceX, faceY, faceZ is the middle of the face between from and to
                BlockPos goalLook = src.down(); // this is the block we were just standing on, and the one we want to place against

                Rotation backToFace = RotationUtils.calcRotationFromVec3d(ctx.playerHead(), new Vec3d(faceX, faceY, faceZ), ctx.playerRotations());
                float pitch = backToFace.getPitch();
                double dist = Math.max(Math.abs(ctx.player().posX - faceX), Math.abs(ctx.player().posZ - faceZ));
                if (dist < 0.29) {
                    float yaw = RotationUtils.calcRotationFromVec3d(VecUtils.getBlockPosCenter(dest), ctx.playerHead(), ctx.playerRotations()).getYaw();
                    state.setTarget(new MovementState.MovementTarget(new Rotation(yaw, pitch), true));
                    state.setInput(Input.MOVE_BACK, true);
                } else {
                    state.setTarget(new MovementState.MovementTarget(backToFace, true));
                }
                state.setInput(Input.SNEAK, true);
                if (Objects.equals(ctx.getSelectedBlock().orElse(null), goalLook)) {
                    return state.setInput(Input.CLICK_RIGHT, true); // wait to right click until we are able to place
                }
                // Out.log("Trying to look at " + goalLook + ", actually looking at" + Baritone.whatAreYouLookingAt());
                return state.setInput(Input.CLICK_LEFT, true);
            } else {
                MovementHelper.moveTowards(ctx, state, positionsToBreak[0]);
                return state;
                // TODO MovementManager.moveTowardsBlock(to); // move towards not look at because if we are bridging for a couple blocks in a row, it is faster if we dont spin around and walk forwards then spin around and place backwards for every block
            }
        }
    }

    @Override
    public boolean safeToCancel(MovementState state) {
        // if we're in the process of breaking blocks before walking forwards
        // or if this isn't a sneak place (the block is already there)
        // then it's safe to cancel this
        return state.getStatus() != MovementStatus.RUNNING || MovementHelper.canWalkOn(ctx, dest.down());
    }

    @Override
    protected boolean prepared(MovementState state) {
        if (ctx.playerFeet().equals(src) || ctx.playerFeet().equals(src.down())) {
            Block block = BlockStateInterface.getBlock(ctx, src.down());
            if (block == Blocks.LADDER || block == Blocks.VINE) {
                state.setInput(Input.SNEAK, true);
            }
        }
        return super.prepared(state);
    }
}
