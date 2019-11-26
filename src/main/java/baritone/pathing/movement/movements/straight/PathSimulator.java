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

import baritone.api.utils.BetterBlockPos;
import baritone.pathing.movement.MovementHelper;
import baritone.utils.BlockStateInterface;
import baritone.utils.math.IntAABB2;
import baritone.utils.math.Vector2;
import net.minecraft.util.math.Vec3d;

import java.util.Iterator;
import java.util.Optional;

public final class PathSimulator implements Iterator<PathSimulator.PathPart> {

    public static final double PLAYER_AABB_SIZE = 0.6;

    private final BlockStateInterface bsi;

    private final GridCollisionIterator blockCollisionIterator;
    private Vector2 currentXZ = null;
    private int currentY;
    private final BetterBlockPos dest;

    private PathPart nextPart = null;

    public PathSimulator(Vec3d start, BetterBlockPos dest, BlockStateInterface bsi) {
        this.bsi = bsi;

        // remove 0.1 to be conservative when looking for falls
        // TODO: the problem is that we need to do the opposite (add 0.1) to be
        //  conservative when looking for walls
        blockCollisionIterator = new GridCollisionIterator(PLAYER_AABB_SIZE - 0.1,
                Vector2.fromXZ(start),
                new Vector2(dest.x + 0.5, dest.z + 0.5));
        currentY = (int) Math.floor(start.y);
        this.dest = dest;

        prepareNext();
    }

    @Override
    public boolean hasNext() {
        return nextPart != null;
    }

    @Override
    public PathPart next() {
        PathPart toReturn = nextPart;
        prepareNext();
        return toReturn;
    }

    private void prepareNext() {
        if (nextPart != null && nextPart.isImpossible) {
            // We found that the path was impossible last time so we cannot
            // continue.
            return;
        }

        if (!blockCollisionIterator.hasNext()) {
            // path complete
            nextPart = null;
            return;
        }

        double totalMoveLength = 0;
        Vector2 lastXZ;

        do {
            GridCollisionIterator.CollisionData next = blockCollisionIterator.next();

            lastXZ = currentXZ;
            currentXZ = next.getPosition();

            if (lastXZ != null) {
                totalMoveLength += lastXZ.distanceTo(currentXZ);
            }

            IntAABB2 collidingBlocks = next.getCollidingSquares();

            for (int x = collidingBlocks.minX; x < collidingBlocks.maxX; x++) {
                for (int z = collidingBlocks.minY; z < collidingBlocks.maxY; z++) {
                    if (!MovementHelper.fullyPassable(bsi, x, currentY, z) ||
                        !MovementHelper.fullyPassable(bsi, x, currentY + 1, z)) {
                        // a wall is blocking the path
                        nextPart = PathPart.makeImpossible();
                        return;
                    }
                }
            }

            FallHelper.WillFallResult willFallResult = FallHelper.willFall(collidingBlocks, currentY - 1, bsi);
            if (willFallResult == FallHelper.WillFallResult.NO) {
                // continue looping until we find a fall
                continue;
            } else if (willFallResult == FallHelper.WillFallResult.UNSUPPORTED_TERRAIN) {
                nextPart = PathPart.makeImpossible();
                return;
            }

            Optional<Integer> maybeLandingBlock = FallHelper.getLandingBlock(collidingBlocks, currentY, bsi);
            if (!maybeLandingBlock.isPresent()) {
                // void or unsupported blocks
                nextPart = PathPart.makeImpossible();
                return;
            }

            int landingBlockY = maybeLandingBlock.get();
            int feetBlockY = landingBlockY + 1;

            boolean isInDestXZ = Math.abs(currentXZ.x - dest.x) < PLAYER_AABB_SIZE &&
                    Math.abs(currentXZ.y - dest.z) < PLAYER_AABB_SIZE;

            if (feetBlockY < dest.y) {
                if (isInDestXZ) {
                    // cut the path to the destination
                    feetBlockY = dest.y;
                } else {
                    // we fell below the destination so now, we're stuck
                    nextPart = PathPart.makeImpossible();
                    return;
                }
            }

            nextPart = new PathPart(totalMoveLength, currentY, feetBlockY, collidingBlocks);
            currentY = feetBlockY;
            return;
        } while (blockCollisionIterator.hasNext());

        nextPart = new PathPart(totalMoveLength, currentY, currentY, null);
    }

    /**
     * A part of the total path.
     *
     * First, the player will have to move {@code moveLength} blocks towards
     * the destination. Then the player will fall from {@code startY} to
     * {@code endY}, always staying inside of {@code fallBox}. If the player
     * does not need to fall to complete this part, then {@code startY == endY}
     * and {@code fallBox == null}.
     */
    public static final class PathPart {
        private final double moveLength;
        private final int startY;
        private final int endY;
        private final IntAABB2 fallBox;
        private final boolean isImpossible;

        private PathPart(double moveLength, int startY, int endY, IntAABB2 fallBox) {
            this.moveLength = moveLength;
            this.startY = startY;
            this.endY = endY;
            this.fallBox = fallBox;
            isImpossible = false;
        }

        private PathPart() {
            moveLength = 0;
            startY = 0;
            endY = 0;
            fallBox = null;
            isImpossible = true;
        }

        private static PathPart makeImpossible() {
            return new PathPart();
        }

        public double getMoveLength() {
            assertPossible();
            return moveLength;
        }

        public int getStartY() {
            assertPossible();
            return startY;
        }

        public int getEndY() {
            assertPossible();
            return endY;
        }

        public IntAABB2 getFallBox() {
            assertPossible();
            return fallBox;
        }

        public boolean isImpossible() {
            return isImpossible;
        }

        private void assertPossible() {
            if (isImpossible) {
                throw new IllegalStateException("path is impossible");
            }
        }
    }

}
