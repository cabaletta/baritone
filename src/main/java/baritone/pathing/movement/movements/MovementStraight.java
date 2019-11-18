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

    // When the input heuristic is smaller than this, then we must
    // stop sprinting to prevent overshooting the target.
    private static final double CAN_STILL_SPRINT_THRESHOLD = 1.5 * 1.5;

    private static final double IGNORE_FALL_BOX_MIN_DIST_SQR = 4.0 * 4.0;

    // If not null, then the player must stay within this box
    // for the next fall until they touch the ground.
    private HorizontalIntAABB nextFallBox = null;

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

        Vector2 srcXZ = new Vector2((double) src.x + 0.5, (double) src.z + 0.5);
        Vector2 destXZ = new Vector2((double) dest.x + 0.5, (double) dest.z + 0.5);
        int feetY = src.y;

        AxisAlignedBB playerRelativeAABB = ctx.player()
                .getEntityBoundingBox()
                .offset(ctx.playerFeetAsVec().scale(-1.0));

        double distRemainingXZSqr = srcXZ.distanceToSqr(destXZ);

        Vector2 dirXZ = destXZ.minus(srcXZ).normalize();
        Vector2 predictionIncrementXZ = dirXZ.times(0.1);

        // predict the next positions
        for (int i = 0; i < 2000; i++) {
            Vector2 delta = predictionIncrementXZ.times(i);

            // don't go further than required!
            if (delta.magnitudeSqr() > distRemainingXZSqr) {
                break;
            }

            Vector2 predictedPosXZ = srcXZ.plus(delta);
            AxisAlignedBB predictedAABB = playerRelativeAABB
                    .offset(predictedPosXZ.x, feetY, predictedPosXZ.y);
            HorizontalIntAABB simplerAABB = new HorizontalIntAABB(
                    (int) Math.floor(predictedAABB.minX),
                    (int) Math.floor(predictedAABB.minZ),
                    (int) Math.ceil(predictedAABB.maxX),
                    (int) Math.ceil(predictedAABB.maxZ)
            );

            for (int x = simplerAABB.minX; x < simplerAABB.maxX; x++) {
                for (int z = simplerAABB.minY; z < simplerAABB.maxY; z++) {
                    if (!MovementHelper.fullyPassable(context, x, feetY, z) ||
                            !MovementHelper.fullyPassable(context, x, feetY + 1, z)) {
                        return false;
                    }
                }
            }

            boolean isDestXZ = dest.x == (int) Math.floor(predictedPosXZ.x) &&
                    dest.z == (int) Math.floor(predictedPosXZ.y);

            // Loop until we find something that the player can land on if they
            // are falling or just the feet block if they aren't.
            while (true) {
                // Whatever the block is, we don't care if we arrived at the
                // destination. It's the next movement's job to ensure that it
                // supports it.
                if (isDestXZ && feetY == dest.y) {
                    return true;
                }

                WillFallResult willFallResult = willFall(simplerAABB, feetY - 1, context.bsi);
                if (willFallResult == WillFallResult.NO) {
                    break;
                } else if (willFallResult == WillFallResult.UNSUPPORTED_TERRAIN) {
                    return false;
                }

                feetY--;

                if (feetY < dest.y) {
                    // stuck below destination
                    return false;
                }
            }
        }

        return false;
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

        // TODO: fall
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

            Vector2 srcXZ = new Vector2((double) src.x + 0.5, (double) src.z + 0.5);
            Vector2 destXZ = new Vector2((double) dest.x + 0.5, (double) dest.z + 0.5);
            Vector2 playerPosXZ = new Vector2(player.posX, player.posZ);

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
                    Vector2 predictionIncrementXZ = dirXZ.times(0.1);

                    // predict the next few positions
                    for (int i = 0; i < 80; i++) {
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
                        } else if (willFallResult == WillFallResult.UNSUPPORTED_TERRAIN) {
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
                            if (willFallResult == WillFallResult.UNSUPPORTED_TERRAIN) {
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

            if (nextFallBox == null) {
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
                } else {
                    state.setInput(Input.SPRINT, true);
                    MovementHelper.moveTowards(ctx, state, dest);
                }
            } else {
                moveTowardDestinationButStayInFallBox(state);
            }
        }

        return state;
    }

    /**
     * Move toward the destination but stays inside the fall box.
     *
     * @param state movement state
     */
    private void moveTowardDestinationButStayInFallBox(MovementState state) {
        EntityPlayerSP player = ctx.player();

        Optional<Vector2> maybeMovement = nextFallBox.getShortestMovementToEnter(player.getEntityBoundingBox());
        if (!maybeMovement.isPresent()) {
            // We're in the box so don't move in order to stay inside.
            state.setInput(Input.SPRINT, false)
                    .setInput(Input.MOVE_FORWARD, false);
            return;
        }

        Vector2 movement = maybeMovement.get();
        double movementMagnitudeSqr = movement.magnitudeSqr();

        // Only try to go precisely to it if it is close enough.
        // If it is far, then going to the destination will get us closer to
        // it anyways because it is in our path so we can just go in the direction
        // of the destination.
        if (movementMagnitudeSqr > IGNORE_FALL_BOX_MIN_DIST_SQR) {
            state.setInput(Input.SPRINT, true);
            MovementHelper.moveTowards(ctx, state, dest);
            return;
        }

        double inputHeuristic = Math.sqrt(movementMagnitudeSqr) -
                new Vector2(player.motionX, player.motionZ).magnitude();

        if (inputHeuristic < 0.0) {
            // wait for the momentum to bring us in the hole
            state.setInput(Input.SPRINT, false)
                    .setInput(Input.MOVE_FORWARD, false);
            return;
        }

        boolean canSprint = inputHeuristic >= CAN_STILL_SPRINT_THRESHOLD;

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
        UNSUPPORTED_TERRAIN,
    }

    private static WillFallResult willFall(HorizontalIntAABB playerAABB, int floorBlockY, BlockStateInterface bsi) {
        for (int x = playerAABB.minX; x < playerAABB.maxX; x++) {
            for (int z = playerAABB.minY; z < playerAABB.maxY; z++) {
                if (canWalkOn(bsi, x, floorBlockY, z)) {
                    // player is supported by at least one block
                    return WillFallResult.NO;
                }

                // We don't handle weird blocks yet.
                if (bsi.get0(x, floorBlockY, z).getBlock() != Blocks.AIR) {
                    return WillFallResult.UNSUPPORTED_TERRAIN;
                }
            }
        }

        return WillFallResult.YES;
    }

    @Override
    public void reset() {
        super.reset();
        nextFallBox = null;
    }

}
