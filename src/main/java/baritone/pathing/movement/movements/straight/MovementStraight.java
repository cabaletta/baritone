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
import baritone.utils.math.GeometryHelper;
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

    @Override
    public double calculateCost(CalculationContext context) {
        // can only fall, not jump yet
        if (dest.y > src.y) {
            return COST_INF;
        }

        // There must be a block under the player because we often check for
        // it.
        if (src.y < 1) {
            return COST_INF;
        }

        PathSimulator pathSimulator = new PathSimulator(new Vec3d(src.x + 0.5, src.y, src.z + 0.5), dest, context.bsi);

        double totalCost = 0;

        while (pathSimulator.hasNext()) {
            PathSimulator.PathPart part = pathSimulator.next();

            if (part.isImpossible()) {
                return COST_INF;
            }

            double moveCost = part.getMoveLength() * WALK_ONE_BLOCK_COST;
            if (context.canSprint) {
                moveCost *= SPRINT_MULTIPLIER;
            }
            totalCost += moveCost;

            if (part.getEndY() != part.getStartY()) {
                int fallDistance = part.getStartY() - part.getEndY();
                if (fallDistance < 0) {
                    throw new IllegalStateException("path went up");
                }
                int index = Math.min(fallDistance, FALL_N_BLOCKS_COST.length - 1);
                totalCost += FALL_N_BLOCKS_COST[index];
            }
        }

        return totalCost;
    }

    @Override
    protected Set<BetterBlockPos> calculateValidPositions() {
        HashSet<BetterBlockPos> positions = new HashSet<>();

        GridCollisionIterator iterator =
                new GridCollisionIterator(PathSimulator.PLAYER_AABB_SIZE,
                        this.getSrcXZ(),
                        this.getDestXZ());
        while (iterator.hasNext()) {
            IntAABB2 groundBlocks = iterator.next().getCollidingSquares();

            // TODO: Some of these positions will never be reached because the
            //  player will be standing on some terrain. Remove them.
            for (int y = dest.y; y <= src.y; y++) {
                for (int x = groundBlocks.minX; x <= groundBlocks.maxX; x++) {
                    for (int z = groundBlocks.minY; z <= groundBlocks.maxY; z++) {
                        positions.add(new BetterBlockPos(x, y, z));
                    }
                }
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

                    if (!result.isPathStillValid()) {
                        return state.setStatus(MovementStatus.UNREACHABLE);
                    }

                    Optional<IntAABB2> maybeFallBox = result.getFallBox();
                    if (maybeFallBox.isPresent()) {
                        IntAABB2 fallBox = maybeFallBox.get();
                        extendFallBox(fallBox, playerFeet.y, result.getFloorBlockY() + 1);
                        validFallBox = fallBox;
                    }
                }
            }

            if (validFallBox != null) {
                moveTowardsDestinationButStayInFallBox(state);
            } else {
                Vector2 closestPointXZ = GeometryHelper.getClosestPointOnSegment(srcXZ, destXZ, playerPosXZ);

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
        // TODO: make this better
        extendFallBoxHelper(fallBox, fallStartY, fallEndY, 0, -1);
        extendFallBoxHelper(fallBox, fallStartY, fallEndY, -1, 0);
        extendFallBoxHelper(fallBox, fallStartY, fallEndY, 0, 1);
        extendFallBoxHelper(fallBox, fallStartY, fallEndY, 1, 0);
    }

    private void extendFallBoxHelper(IntAABB2 fallBox, int fallStartY, int fallEndY, int incrementX, int incrementZ) {
        int x = (incrementX >= 0 ? fallBox.maxX : fallBox.minX) + incrementX;
        int z = (incrementZ >= 0 ? fallBox.maxY : fallBox.minY) + incrementZ;

        if (canFallAndContinueToDestination(x, z, fallStartY, fallEndY)) {
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

    private boolean canFallAndContinueToDestination(int x, int z, int fallStartY, int maxFallEndY) {
        BlockStateInterface bsi = new BlockStateInterface(ctx);

        // make sure that the whole column is fully passable
        for (int y = maxFallEndY; y <= fallStartY; y++) {
            FallHelper.WillFallResult result = FallHelper.willFall(new BetterBlockPos(x, y, z), bsi);
            if (result != FallHelper.WillFallResult.YES) {
                return false;
            }
        }

        if (!FallHelper.getLandingBlock(new BetterBlockPos(x, maxFallEndY, z), bsi).isPresent()) {
            // void or unsupported blocks
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

    private Vector2 getSrcXZ() {
        return new Vector2((double) src.x + 0.5, (double) src.z + 0.5);
    }

    private Vector2 getDestXZ() {
        return new Vector2((double) dest.x + 0.5, (double) dest.z + 0.5);
    }

}
