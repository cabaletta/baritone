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

package baritone.utils.math;

import net.minecraft.util.math.AxisAlignedBB;

import java.util.Optional;

/**
 * An axis aligned bounding box in 2 dimensions with integers.
 */
public class HorizontalIntAABB {

    public int minX;
    public int minY;
    public int maxX;
    public int maxY;

    public HorizontalIntAABB(int minX, int minY, int maxX, int maxY) {
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
    }

    public Vector2 clampPointInside(Vector2 vec) {
        return new Vector2(
                clamp(vec.x, minX, maxX),
                clamp(vec.y, minY, maxY)
        );
    }

    /**
     * Returns the vector of the shortest straight path to enter the box.
     * If the origin is already inside of the box, then return
     * {@code Optional.empty()}.
     *
     * {@code movingBox} must be smaller than {@code this}!
     *
     * @param movingBox - the box that wants to go inside of {@code this}
     * @return the direction to enter {@code this}
     */
    public Optional<Vector2> getShortestMovementToEnter(AxisAlignedBB movingBox) {
        if (movingBox.minX >= minX && movingBox.maxX <= maxX &&
                movingBox.minZ >= minY && movingBox.maxZ <= maxY) {
            return Optional.empty();
        }

        Vector2[] edges = new Vector2[] {
                new Vector2(movingBox.minX, movingBox.minZ),
                new Vector2(movingBox.minX, movingBox.maxZ),
                new Vector2(movingBox.maxX, movingBox.minZ),
                new Vector2(movingBox.maxX, movingBox.maxZ),
        };

        double largestDistSqr = -1.0;
        Vector2 moveVector = null;

        // The edge that is the furthest to this box will help us know the
        // direction because it is the one that will enter the box last and
        // we assume that if it enters the box, then all of the other edges
        // have already entered the box.
        for (Vector2 edge : edges) {
            Vector2 closestPointToEdge = clampPointInside(edge);
            double distSqr = edge.distanceToSqr(closestPointToEdge);
            if (distSqr > largestDistSqr) {
                moveVector = closestPointToEdge.minus(edge);
                largestDistSqr = distSqr;
            }
        }

        if (moveVector == null) {
            throw new IllegalStateException("what?!");
        }

        return Optional.of(moveVector);
    }

    private static double clamp(double val, double min, double max) {
        return Math.min(Math.max(val, min), max);
    }

}
