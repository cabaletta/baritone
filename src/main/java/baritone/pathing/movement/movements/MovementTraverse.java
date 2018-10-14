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
import baritone.api.utils.*;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.Movement;
import baritone.pathing.movement.MovementHelper;
import baritone.pathing.movement.MovementState;
import baritone.utils.BlockStateInterface;
import baritone.utils.InputOverrideHandler;
import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
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

    public MovementTraverse(BetterBlockPos from, BetterBlockPos to) {
        super(from, to, new BetterBlockPos[]{to.up(), to}, to.down());
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
        IBlockState pb0 = BlockStateInterface.get(destX, y + 1, destZ);
        IBlockState pb1 = BlockStateInterface.get(destX, y, destZ);
        IBlockState destOn = BlockStateInterface.get(destX, y - 1, destZ);
        Block srcDown = BlockStateInterface.getBlock(x, y - 1, z);
        if (MovementHelper.canWalkOn(destX, y - 1, destZ, destOn)) {//this is a walk, not a bridge
            double WC = WALK_ONE_BLOCK_COST;
            boolean water = false;
            if (BlockStateInterface.isWater(pb0.getBlock()) || BlockStateInterface.isWater(pb1.getBlock())) {
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
            double hardness1 = MovementHelper.getMiningDurationTicks(context, destX, y + 1, destZ, pb0, true);
            if (hardness1 >= COST_INF) {
                return COST_INF;
            }
            double hardness2 = MovementHelper.getMiningDurationTicks(context, destX, y, destZ, pb1, false);
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
            if (destOn.getBlock().equals(Blocks.AIR) || MovementHelper.isReplacable(destX, y - 1, destZ, destOn)) {
                boolean throughWater = BlockStateInterface.isWater(pb0.getBlock()) || BlockStateInterface.isWater(pb1.getBlock());
                if (BlockStateInterface.isWater(destOn.getBlock()) && throughWater) {
                    return COST_INF;
                }
                if (!context.canPlaceThrowawayAt(destX, y - 1, destZ)) {
                    return COST_INF;
                }
                double hardness1 = MovementHelper.getMiningDurationTicks(context, destX, y, destZ, pb0, false);
                if (hardness1 >= COST_INF) {
                    return COST_INF;
                }
                double hardness2 = MovementHelper.getMiningDurationTicks(context, destX, y + 1, destZ, pb1, true);

                double WC = throughWater ? context.waterWalkSpeed() : WALK_ONE_BLOCK_COST;
                for (int i = 0; i < 4; i++) {
                    int againstX = destX + HORIZONTALS[i].getXOffset();
                    int againstZ = destZ + HORIZONTALS[i].getZOffset();
                    if (againstX == x && againstZ == z) {
                        continue;
                    }
                    if (MovementHelper.canPlaceAgainst(againstX, y - 1, againstZ)) {
                        return WC + context.placeBlockCost() + hardness1 + hardness2;
                    }
                }
                if (srcDown == Blocks.SOUL_SAND || (srcDown instanceof BlockSlab && !((BlockSlab) srcDown).isDouble())) {
                    return COST_INF; // can't sneak and backplace against soul sand or half slabs =/
                }
                if (srcDown == Blocks.FLOWING_WATER || srcDown == Blocks.WATER) {
                    return COST_INF; // this is obviously impossible
                }
                WC = WC * SNEAK_ONE_BLOCK_COST / WALK_ONE_BLOCK_COST;//since we are placing, we are sneaking
                return WC + context.placeBlockCost() + hardness1 + hardness2;
            }
            return COST_INF;
            // Out.log("Can't walk on " + Baritone.get(positionsToPlace[0]).getBlock());
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
            if (MovementHelper.avoidWalkingInto(BlockStateInterface.get(positionsToBreak[0]).getBlock())) {
                return state;
            }
            if (MovementHelper.avoidWalkingInto(BlockStateInterface.get(positionsToBreak[1]).getBlock())) {
                return state;
            }
            // and we aren't already pressed up against the block
            double dist = Math.max(Math.abs(player().posX - (dest.getX() + 0.5D)), Math.abs(player().posZ - (dest.getZ() + 0.5D)));
            if (dist < 0.83) {
                return state;
            }

            // combine the yaw to the center of the destination, and the pitch to the specific block we're trying to break
            // it's safe to do this since the two blocks we break (in a traverse) are right on top of each other and so will have the same yaw
            float yawToDest = RotationUtils.calcRotationFromVec3d(playerHead(), VecUtils.calculateBlockCenter(dest)).getYaw();
            float pitchToBreak = state.getTarget().getRotation().get().getPitch();

            state.setTarget(new MovementState.MovementTarget(new Rotation(yawToDest, pitchToBreak), true));
            return state.setInput(InputOverrideHandler.Input.MOVE_FORWARD, true);
        }

        //sneak may have been set to true in the PREPPING state while mining an adjacent block
        state.setInput(InputOverrideHandler.Input.SNEAK, false);

        Block fd = BlockStateInterface.get(src.down()).getBlock();
        boolean ladder = fd instanceof BlockLadder || fd instanceof BlockVine;
        IBlockState pb0 = BlockStateInterface.get(positionsToBreak[0]);
        IBlockState pb1 = BlockStateInterface.get(positionsToBreak[1]);

        boolean door = pb0.getBlock() instanceof BlockDoor || pb1.getBlock() instanceof BlockDoor;
        if (door) {
            boolean isDoorActuallyBlockingUs = false;
            if (pb0.getBlock() instanceof BlockDoor && !MovementHelper.isDoorPassable(src, dest)) {
                isDoorActuallyBlockingUs = true;
            } else if (pb1.getBlock() instanceof BlockDoor && !MovementHelper.isDoorPassable(dest, src)) {
                isDoorActuallyBlockingUs = true;
            }
            if (isDoorActuallyBlockingUs) {
                if (!(Blocks.IRON_DOOR.equals(pb0.getBlock()) || Blocks.IRON_DOOR.equals(pb1.getBlock()))) {
                    return state.setTarget(new MovementState.MovementTarget(RotationUtils.calcRotationFromVec3d(playerHead(), VecUtils.calculateBlockCenter(positionsToBreak[0])), true))
                            .setInput(InputOverrideHandler.Input.CLICK_RIGHT, true);
                }
            }
        }

        if (pb0.getBlock() instanceof BlockFenceGate || pb1.getBlock() instanceof BlockFenceGate) {
            BlockPos blocked = null;
            if (!MovementHelper.isGatePassable(positionsToBreak[0], src.up())) {
                blocked = positionsToBreak[0];
            } else if (!MovementHelper.isGatePassable(positionsToBreak[1], src)) {
                blocked = positionsToBreak[1];
            }

            if (blocked != null) {
                return state.setTarget(new MovementState.MovementTarget(RotationUtils.calcRotationFromVec3d(playerHead(), VecUtils.calculateBlockCenter(blocked)), true))
                        .setInput(InputOverrideHandler.Input.CLICK_RIGHT, true);
            }
        }

        boolean isTheBridgeBlockThere = MovementHelper.canWalkOn(positionToPlace) || ladder;
        BlockPos whereAmI = playerFeet();
        if (whereAmI.getY() != dest.getY() && !ladder) {
            logDebug("Wrong Y coordinate");
            if (whereAmI.getY() < dest.getY()) {
                state.setInput(InputOverrideHandler.Input.JUMP, true);
            }
            return state;
        }

        if (isTheBridgeBlockThere) {
            if (playerFeet().equals(dest)) {
                return state.setStatus(MovementStatus.SUCCESS);
            }
            if (wasTheBridgeBlockAlwaysThere && !BlockStateInterface.isLiquid(playerFeet())) {
                state.setInput(InputOverrideHandler.Input.SPRINT, true);
            }
            Block destDown = BlockStateInterface.get(dest.down()).getBlock();
            if (whereAmI.getY() != dest.getY() && ladder && (destDown instanceof BlockVine || destDown instanceof BlockLadder)) {
                new MovementPillar(dest.down(), dest).updateState(state); // i'm sorry
                return state;
            }
            MovementHelper.moveTowards(state, positionsToBreak[0]);
            return state;
        } else {
            wasTheBridgeBlockAlwaysThere = false;
            for (int i = 0; i < 4; i++) {
                BlockPos against1 = dest.offset(HORIZONTALS[i]);
                if (against1.equals(src)) {
                    continue;
                }
                against1 = against1.down();
                if (MovementHelper.canPlaceAgainst(against1)) {
                    if (!MovementHelper.throwaway(true)) { // get ready to place a throwaway block
                        logDebug("bb pls get me some blocks. dirt or cobble");
                        return state.setStatus(MovementStatus.UNREACHABLE);
                    }
                    if (!Baritone.settings().assumeSafeWalk.get()) {
                        state.setInput(InputOverrideHandler.Input.SNEAK, true);
                    }
                    Block standingOn = BlockStateInterface.get(playerFeet().down()).getBlock();
                    if (standingOn.equals(Blocks.SOUL_SAND) || standingOn instanceof BlockSlab) { // see issue #118
                        double dist = Math.max(Math.abs(dest.getX() + 0.5 - player().posX), Math.abs(dest.getZ() + 0.5 - player().posZ));
                        if (dist < 0.85) { // 0.5 + 0.3 + epsilon
                            MovementHelper.moveTowards(state, dest);
                            return state.setInput(InputOverrideHandler.Input.MOVE_FORWARD, false)
                                    .setInput(InputOverrideHandler.Input.MOVE_BACK, true);
                        }
                    }
                    state.setInput(InputOverrideHandler.Input.MOVE_BACK, false);
                    double faceX = (dest.getX() + against1.getX() + 1.0D) * 0.5D;
                    double faceY = (dest.getY() + against1.getY()) * 0.5D;
                    double faceZ = (dest.getZ() + against1.getZ() + 1.0D) * 0.5D;
                    state.setTarget(new MovementState.MovementTarget(RotationUtils.calcRotationFromVec3d(playerHead(), new Vec3d(faceX, faceY, faceZ), playerRotations()), true));

                    EnumFacing side = Minecraft.getMinecraft().objectMouseOver.sideHit;
                    if (Objects.equals(RayTraceUtils.getSelectedBlock().orElse(null), against1) && (Minecraft.getMinecraft().player.isSneaking() || Baritone.settings().assumeSafeWalk.get())) {
                        if (RayTraceUtils.getSelectedBlock().get().offset(side).equals(positionToPlace)) {
                            return state.setInput(InputOverrideHandler.Input.CLICK_RIGHT, true);
                        }
                        // wrong side?
                    }
                    System.out.println("Trying to look at " + against1 + ", actually looking at" + RayTraceUtils.getSelectedBlock());
                    return state.setInput(InputOverrideHandler.Input.CLICK_LEFT, true);
                }
            }
            if (!Baritone.settings().assumeSafeWalk.get()) {
                state.setInput(InputOverrideHandler.Input.SNEAK, true);
            }
            if (whereAmI.equals(dest)) {
                // If we are in the block that we are trying to get to, we are sneaking over air and we need to place a block beneath us against the one we just walked off of
                // Out.log(from + " " + to + " " + faceX + "," + faceY + "," + faceZ + " " + whereAmI);
                if (!MovementHelper.throwaway(true)) {// get ready to place a throwaway block
                    logDebug("bb pls get me some blocks. dirt or cobble");
                    return state.setStatus(MovementStatus.UNREACHABLE);
                }
                double faceX = (dest.getX() + src.getX() + 1.0D) * 0.5D;
                double faceY = (dest.getY() + src.getY() - 1.0D) * 0.5D;
                double faceZ = (dest.getZ() + src.getZ() + 1.0D) * 0.5D;
                // faceX, faceY, faceZ is the middle of the face between from and to
                BlockPos goalLook = src.down(); // this is the block we were just standing on, and the one we want to place against

                Rotation backToFace = RotationUtils.calcRotationFromVec3d(playerHead(), new Vec3d(faceX, faceY, faceZ), playerRotations());
                float pitch = backToFace.getPitch();
                double dist = Math.max(Math.abs(player().posX - faceX), Math.abs(player().posZ - faceZ));
                if (dist < 0.29) {
                    float yaw = RotationUtils.calcRotationFromVec3d(VecUtils.getBlockPosCenter(dest), playerHead(), playerRotations()).getYaw();
                    state.setTarget(new MovementState.MovementTarget(new Rotation(yaw, pitch), true));
                    state.setInput(InputOverrideHandler.Input.MOVE_BACK, true);
                } else {
                    state.setTarget(new MovementState.MovementTarget(backToFace, true));
                }
                state.setInput(InputOverrideHandler.Input.SNEAK, true);
                if (Objects.equals(RayTraceUtils.getSelectedBlock().orElse(null), goalLook)) {
                    return state.setInput(InputOverrideHandler.Input.CLICK_RIGHT, true); // wait to right click until we are able to place
                }
                // Out.log("Trying to look at " + goalLook + ", actually looking at" + Baritone.whatAreYouLookingAt());
                return state.setInput(InputOverrideHandler.Input.CLICK_LEFT, true);
            } else {
                MovementHelper.moveTowards(state, positionsToBreak[0]);
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
        return state.getStatus() != MovementStatus.RUNNING || MovementHelper.canWalkOn(dest.down());
    }

    @Override
    protected boolean prepared(MovementState state) {
        if (playerFeet().equals(src) || playerFeet().equals(src.down())) {
            Block block = BlockStateInterface.getBlock(src.down());
            if (block == Blocks.LADDER || block == Blocks.VINE) {
                state.setInput(InputOverrideHandler.Input.SNEAK, true);
            }
        }
        return super.prepared(state);
    }
}
