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

package baritone.pathing.movement.movements.straight;

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
import baritone.utils.math.IntAABB2;
import baritone.utils.math.MathUtils;
import baritone.utils.math.Vector2;
import baritone.utils.pathing.LineBlockIterator;
import net.minecraft.client.entity.EntityPlayerSP;
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
    private IntAABB2 validFallBox = null;

    public MovementStraight(IBaritone baritone, BetterBlockPos src, BetterBlockPos dest) {
        super(baritone, src, dest, new BetterBlockPos[]{});
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
            IntAABB2 simplerAABB = new IntAABB2(
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

                FallHelper.WillFallResult willFallResult = FallHelper.willFall(simplerAABB, feetY - 1, context.bsi);
                if (willFallResult == FallHelper.WillFallResult.NO) {
                    break;
                } else if (willFallResult == FallHelper.WillFallResult.UNSUPPORTED_TERRAIN) {
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
            BetterBlockPos playerFeet = ctx.playerFeet();

            if (playerFeet.equals(dest)) {
                return state.setStatus(MovementStatus.SUCCESS);
            }

            Vector2 srcXZ = new Vector2((double) src.x + 0.5, (double) src.z + 0.5);
            Vector2 destXZ = this.getDestXZ();
            Vector2 playerPosXZ = new Vector2(player.posX, player.posZ);

            // find the next fall
            if (player.onGround) {
                validFallBox = null;

                if (player.posY > dest.y) {
                    FallHelper.NextFallResult result = FallHelper.findNextFall(ctx, dest);

                    Optional<IntAABB2> maybeFallBox = result.getFallBox();
                    if (maybeFallBox.isPresent()) {
                        IntAABB2 fallBox = maybeFallBox.get();
                        extendFallBox(fallBox, playerFeet.y, result.getFloorBlockY() + 1);
                        validFallBox = fallBox;
                    } else if (!result.isPathStillValid()) {
                        return state.setStatus(MovementStatus.UNREACHABLE);
                    }
                }
            }

            if (validFallBox != null) {
                moveTowardsDestinationButStayInFallBox(state);
            } else {
                Vector2 closestPointXZ = MathUtils.getClosestPointOnSegment(srcXZ, destXZ, playerPosXZ);

                if (playerPosXZ.distanceToSqr(closestPointXZ) >= RECENTER_DIST_THRESHOLD_SQR) {
                    // recenter on line
                    moveTowards(state, closestPointXZ);
                } else {
                    moveTowards(state, destXZ);
                }
            }
        }

        return state;
    }

    @Override
    public void reset() {
        super.reset();
        validFallBox = null;
    }

    private void extendFallBox(IntAABB2 fallBox, int fallStartY, int fallEndY) {
        extendFallBoxHelper(fallBox, fallStartY, fallEndY, 0, -1);
        extendFallBoxHelper(fallBox, fallStartY, fallEndY, -1, 0);
        extendFallBoxHelper(fallBox, fallStartY, fallEndY, 0, 1);
        extendFallBoxHelper(fallBox, fallStartY, fallEndY, 1, 0);
    }

    private void extendFallBoxHelper(IntAABB2 fallBox, int fallStartY, int fallEndY, int incrementX, int incrementZ) {
        int x = (incrementX >= 0 ? fallBox.maxX : fallBox.minX) + incrementX;
        int z = (incrementZ >= 0 ? fallBox.maxY : fallBox.minY) + incrementZ;

        if (canContinueAfterFall(x, z, fallStartY, fallEndY)) {
            if (incrementX > 0) {
                fallBox.maxX += incrementX;
            } else if (incrementX < 0) {
                fallBox.minX += incrementX;
            }

            if (incrementZ > 0) {
                fallBox.maxY += incrementZ;
            } else if (incrementZ < 0) {
                fallBox.minY += incrementZ;
            }
        }
    }

    private boolean canContinueAfterFall(int x, int z, int fallStartY, int fallEndY) {
        BlockStateInterface bsi = new BlockStateInterface(ctx);

        // make sure that the whole column is fully passable
        for (int y = fallEndY; y <= fallStartY; y++) {
            FallHelper.WillFallResult result = FallHelper.willFall(new BetterBlockPos(x, y, z), bsi);
            if (result != FallHelper.WillFallResult.YES) {
                return false;
            }
        }

        if (!FallHelper.getLandingBlock(new BetterBlockPos(x, fallEndY, z), bsi).isPresent()) {
            return false;
        }

        // TODO: check if the path is possible from the landing block
        return true;
    }

    /**
     * Move towards the destination but stays inside the fall box.
     *
     * @param state movement state
     */
    private void moveTowardsDestinationButStayInFallBox(MovementState state) {
        EntityPlayerSP player = ctx.player();

        Vector2 target = this.getDestXZ();

        Optional<Vector2> maybeEnterMovement = validFallBox.getShortestMovementToEnter(player.getEntityBoundingBox());
        if (maybeEnterMovement.isPresent()) {
            Vector2 movement = maybeEnterMovement.get();
            double movementMagnitudeSqr = movement.magnitudeSqr();

            // Only try to go precisely to it if it is close enough because we
            // are not sure if the path works otherwise because we didn't
            // predict the path to go to the fall box but the path to go
            // towards the destination.
            // TODO: remove this hack
            if (movementMagnitudeSqr <= IGNORE_FALL_BOX_MIN_DIST_SQR) {
                Vector2 playerPosXZ = new Vector2(player.posX, player.posZ);
                target = playerPosXZ.plus(movement);
            }
        } else {
            // We're in the box so we can try to move towards the destination
            // but we have to stay inside of it.
            target = validFallBox.clampPointInsideWithMargin(target, player.width + 0.1);
        }

        moveTowards(state, target);
    }

    private void moveTowards(MovementState state, Vector2 target) {
        EntityPlayerSP player = ctx.player();
        Vector2 playerPosXZ = new Vector2(player.posX, player.posZ);
        Vector2 playerToTarget = target.minus(playerPosXZ);

        double inputHeuristic = playerToTarget.magnitude() -
                new Vector2(player.motionX, player.motionZ).magnitude();

        if (inputHeuristic < 0.0) {
            // wait for the momentum to bring us in the hole
            state.setInput(Input.SPRINT, false)
                    .setInput(Input.MOVE_FORWARD, false);
            return;
        }

        boolean canSprint = inputHeuristic >= CAN_STILL_SPRINT_THRESHOLD;

        state
                .setTarget(new MovementState.MovementTarget(
                        new Rotation(RotationUtils.calcRotationFromVec3d(
                                new Vec3d(0.0, 0.0, 0.0),
                                new Vec3d(playerToTarget.x, 0.0, playerToTarget.y),
                                ctx.playerRotations()
                        ).getYaw(), ctx.player().rotationPitch), false
                ))
                .setInput(Input.MOVE_FORWARD, true)
                .setInput(Input.SPRINT, canSprint);
    }

    private Vector2 getDestXZ() {
        return new Vector2((double) dest.x + 0.5, (double) dest.z + 0.5);
    }

}
