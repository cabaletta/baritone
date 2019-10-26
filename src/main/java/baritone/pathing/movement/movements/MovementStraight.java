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
import baritone.api.utils.Rotation;
import baritone.api.utils.RotationUtils;
import baritone.api.utils.input.Input;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.Movement;
import baritone.pathing.movement.MovementHelper;
import baritone.pathing.movement.MovementState;
import baritone.utils.BlockStateInterface;
import baritone.utils.math.HorizontalIntAABB;
import baritone.utils.math.MathUtils;
import baritone.utils.math.Vector2;
import baritone.utils.pathing.LineBlockIterator;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public final class MovementStraight extends Movement {

    private static final double RECENTER_DIST_THRESHOLD_SQR = 0.3 * 0.3;

    // When the distance to the target is smaller than this, then we must
    // stop sprinting to prevent overshooting the target.
    private static final double CAN_STILL_SPRINT_DIST_SQR = 2.5 * 2.5;

    // Same as above but for walking because we need to stop a little bit
    // before the fall because of momentum.
    private static final double CAN_STILL_WALK_DIST_SQR = 0.1 * 0.1;

    private static final double CANCEL_MOMENTUM_THRESHOLD_SQR = 0.05 * 0.05;

    // TODO: only go toward the fall box when we're close enough

    // If not null, then the player must stay within this box
    // for the next fall until they touch the ground.
    private HorizontalIntAABB nextFallBox = null;

    private boolean movedBackToCancelMomentum = false;

    public MovementStraight(IBaritone baritone, BetterBlockPos src, BetterBlockPos dest) {
        super(baritone, src, dest, new BetterBlockPos[]{});
    }

    private static boolean canWalkOn(BlockStateInterface bsi, int x, int y, int z) {
        IBlockState state = bsi.get0(x, y, z);
        Block block = state.getBlock();

        // be conservative for now
        return block != Blocks.AIR &&
                state.isFullCube() &&
                state.getBlock().slipperiness == 0.6F;
    }

    private LineBlockIterator getHorizontalLineIterator() {
        return new LineBlockIterator(src.x, src.z, dest.x, dest.z);
    }

    private boolean checkIfPossible(CalculationContext context) {
        // can only fall and there must be a block under the player
        if (dest.y > src.y || src.y < 1) {
            return false;
        }

        // must end up on a block (let's be conservative for now)
        if (!canWalkOn(context.bsi, dest.x, dest.y - 1, dest.z)) {
            return false;
        }

        int y = src.y;

        // TODO: better
        LineBlockIterator iterator = getHorizontalLineIterator();
        while (iterator.next()) {
            if (!MovementHelper.fullyPassable(context, iterator.currX, y, iterator.currY) ||
                    !MovementHelper.fullyPassable(context, iterator.currX, y + 1, iterator.currY)) {
                return false;
            }

            // Make sure that we can walk on the path, and if we can't, then
            // try again but after falling down:
            // ______
            // [][][]\
            // [][][]|____
            // [][][][][][]
            while (!canWalkOn(context.bsi, iterator.currX, y - 1, iterator.currY)) {
                y--;

                if (y < 1) {
                    return false;
                }

                // make sure that we can fall
                if (!MovementHelper.fullyPassable(context, iterator.currX, y, iterator.currY)) {
                    return false;
                }
            }
        }

        // can't go up yet
        if (y < dest.y) {
            return false;
        }

        // last part is falling to the destination
        while (y > dest.y) {
            y--;

            if (y < 1) {
                return false;
            }

            // make sure that we can fall
            if (!MovementHelper.fullyPassable(context, dest.x, y, dest.z)) {
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

        // TODO: better
        double xDiff = dest.x - src.x;
        double zDiff = dest.z - src.z;
        double dist = Math.sqrt(xDiff * xDiff + zDiff * zDiff);
        double cost = dist * WALK_ONE_BLOCK_COST;

        if (context.canSprint) {
            cost *= SPRINT_MULTIPLIER;
        }

        // return cost;
        return 0.0;
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

        if (ctx.player().posY < dest.y) {
            state.setStatus(MovementStatus.UNREACHABLE);
            return false;
        }

        // TODO: check if the path is still possible
        return true;
    }

    @Override
    public MovementState updateState(MovementState state) {
        super.updateState(state);

        if (state.getStatus() == MovementStatus.RUNNING) {
            EntityPlayerSP player = ctx.player();
            Vec3d playerFeet = ctx.playerFeetAsVec();

            if (ctx.playerFeet().equals(dest)) {
                return state.setStatus(MovementStatus.SUCCESS);
            }

            // check player's position compared to the line from src to dest
            Vector2 srcXZ = new Vector2((double) src.x + 0.5, (double) src.z + 0.5);
            Vector2 destXZ = new Vector2((double) dest.x + 0.5, (double) dest.z + 0.5);
            Vector2 playerPosXZ = new Vector2(player.posX, player.posZ);
            Vector2 closestPointXZ = MathUtils.getClosestPointOnSegment(srcXZ, destXZ, playerPosXZ);

            // recenter?
            if (playerPosXZ.distanceToSqr(closestPointXZ) >= RECENTER_DIST_THRESHOLD_SQR) {
                return state
                        .setTarget(new MovementState.MovementTarget(
                                new Rotation(RotationUtils.calcRotationFromVec3d(playerFeet,
                                        new Vec3d(closestPointXZ.x, player.posY, closestPointXZ.y),
                                        ctx.playerRotations()).getYaw(), ctx.player().rotationPitch),
                                false
                        ))
                        .setInput(Input.MOVE_FORWARD, true)
                        .setInput(Input.SPRINT, true);
            }

            // find the next fall
            if (player.onGround) {
                nextFallBox = null;

                // is there one?
                if (player.posY > dest.y) {
                    BlockStateInterface bsi = new BlockStateInterface(ctx);

                    AxisAlignedBB playerRelativeAABB = player
                            .getEntityBoundingBox()
                            .offset(playerFeet.scale(-1.0));

                    double distRemainingXZSqr = playerPosXZ.distanceToSqr(destXZ);

                    Vector2 dirXZ = destXZ.minus(playerPosXZ).normalize();
                    Vector2 predictionIncrementXZ = dirXZ.times(0.4);

                    // predict the next few positions
                    for (int i = 0; i < 20; i++) {
                        Vector2 delta = predictionIncrementXZ.times(i);

                        // don't go further than required!
                        if (delta.magnitudeSqr() > distRemainingXZSqr) {
                            break;
                        }

                        Vector2 predictedPosXZ = playerPosXZ.plus(delta);
                        AxisAlignedBB predictedAABB = playerRelativeAABB
                                .offset(predictedPosXZ.x, playerFeet.y, predictedPosXZ.y);
                        int floorBlockY = (int) Math.floor(predictedAABB.minY - 1.0);
                        HorizontalIntAABB simplerAABB = new HorizontalIntAABB(
                                (int) Math.floor(predictedAABB.minX),
                                (int) Math.floor(predictedAABB.minZ),
                                (int) Math.ceil(predictedAABB.maxX),
                                (int) Math.ceil(predictedAABB.maxZ)
                        );

                        WillFallResult willFallResult = willFall(simplerAABB, floorBlockY, bsi);
                        if (willFallResult == WillFallResult.NO) {
                            // continue looping until we find a fall
                            continue;
                        } else if (willFallResult == WillFallResult.UNEXPECTED_TERRAIN) {
                            return state.setStatus(MovementStatus.UNREACHABLE);
                        }

                        // Loop until we find something that the player can land on.
                        while (willFallResult == WillFallResult.YES) {
                            floorBlockY--;

                            if (floorBlockY < 0) {
                                // fall into void?
                                return state.setStatus(MovementStatus.UNREACHABLE);
                            }

                            willFallResult = willFall(simplerAABB, floorBlockY, bsi);
                            if (willFallResult == WillFallResult.UNEXPECTED_TERRAIN) {
                                return state.setStatus(MovementStatus.UNREACHABLE);
                            }
                        }

                        // We know that there are all blocks on this AABB are
                        // fully passable because we just did the simulation,
                        // so if the player stays there, then they will be able
                        // to fall without hitting any block during the path.
                        // TODO: try to extend this box as much as possible to
                        //  limit constraints by checking for blocks
                        //  around too
                        nextFallBox = simplerAABB;

                        break;
                    }
                }
            }

            moveTowardDestinationButStayInFallBox(state);
        }

        return state;
    }

    /**
     * Move toward the destination. If a fall box is present, then make sure to
     * go stay inside of it.
     *
     * @param state movement state
     */
    private void moveTowardDestinationButStayInFallBox(MovementState state) {
        if (nextFallBox == null) {
            state.setInput(Input.SPRINT, true);
            MovementHelper.moveTowards(ctx, state, dest);
            movedBackToCancelMomentum = false; // reset flag
            return;
        }

        EntityPlayerSP player = ctx.player();

        Optional<Vector2> maybeMovement = nextFallBox.getShortestMovementToEnter(player.getEntityBoundingBox());
        if (!maybeMovement.isPresent()) {
            // We're in the box so don't move and try to cancel the momentum
            // in order to stay in it.
            state.setInput(Input.SPRINT, false);
            if (!movedBackToCancelMomentum && new Vector2(player.motionX, player.motionZ).magnitudeSqr() >= CANCEL_MOMENTUM_THRESHOLD_SQR) {
                state.setInput(Input.MOVE_BACK, true);
                movedBackToCancelMomentum = true;
            } else {
                state.setInput(Input.MOVE_BACK, false);
            }
            return;
        } else {
            movedBackToCancelMomentum = false; // reset flag
        }

        Vector2 movement = maybeMovement.get();

        double horizontalDistSqr = movement.magnitudeSqr();

        if (horizontalDistSqr < CAN_STILL_WALK_DIST_SQR) {
            // wait for the momentum to bring us in the hole
            state.setInput(Input.SPRINT, false)
                    .setInput(Input.MOVE_FORWARD, false);
            return;
        }

        boolean canSprint = horizontalDistSqr >= CAN_STILL_SPRINT_DIST_SQR;

        state
                // go inside the box
                .setTarget(new MovementState.MovementTarget(
                        new Rotation(RotationUtils.calcRotationFromVec3d(
                                new Vec3d(0.0, 0.0, 0.0),
                                new Vec3d(movement.x, 0.0, movement.y),
                                ctx.playerRotations()
                        ).getYaw(), ctx.player().rotationPitch), false
                ))
                .setInput(Input.MOVE_FORWARD, true)
                .setInput(Input.SPRINT, canSprint);
    }

    private enum WillFallResult {
        YES,
        NO,
        UNEXPECTED_TERRAIN,
    }

    private static WillFallResult willFall(HorizontalIntAABB playerAABB, int floorBlockY, BlockStateInterface bsi) {
        for (int x = playerAABB.minX; x <= playerAABB.maxX; x++) {
            for (int z = playerAABB.minY; z <= playerAABB.maxY; z++) {
                if (canWalkOn(bsi, x, floorBlockY, z)) {
                    // player is supported by at least one block
                    return WillFallResult.NO;
                }

                // This should not happen because the cost calculation will
                // return an infinite cost. We don't handle this case yet.
                if (!MovementHelper.fullyPassable(bsi.get0(x, floorBlockY, z))) {
                    return WillFallResult.UNEXPECTED_TERRAIN;
                }
            }
        }

        return WillFallResult.YES;
    }

    @Override
    public void reset() {
        super.reset();
        nextFallBox = null;
        movedBackToCancelMomentum = false;
    }

}
