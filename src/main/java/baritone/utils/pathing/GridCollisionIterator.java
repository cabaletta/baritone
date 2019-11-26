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

package baritone.utils.pathing;

import baritone.utils.math.IntAABB2;
import baritone.utils.math.Vector2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Let's say that you have a 2D grid of squares with size 1 and you have
 * another square, smaller that the squares in the grid, that will slide
 * from point A to point B.
 *
 * The square that is sliding is always colliding with at least one square in
 * the grid, but sometimes it touches multiple squares (think anti-aliasing).
 *
 * This class will help you get a list of all of the collisions at a time
 * during the travel. It yields integer AABBs which contains all of the blocks
 * that collide with the sliding block.
 *
 * See the images in the `doc/GridCollisionIterator` directory for a graphical
 * explanation.
 */
public final class GridCollisionIterator implements Iterator<IntAABB2> {

    private static final double INCREMENT_MAGNITUDE = 0.1;

    private final double slideSquareSize;
    private Vector2 currentPos;
    private Vector2 increment;
    private final Vector2 end;
    private int remaining;

    public GridCollisionIterator(double slideSquareSize, Vector2 start, Vector2 end) {
        if (slideSquareSize > 1.0) {
            throw new IllegalArgumentException("sliding square must be smaller than other squares");
        }

        this.slideSquareSize = slideSquareSize;

        Vector2 startToEnd = end.minus(start);

        // TODO: find a safer and faster way to do this than this ugly
        //  brute-force which will sometimes miss collisions
        currentPos = start;
        increment = startToEnd.normalize().times(INCREMENT_MAGNITUDE);
        this.end = end;
        remaining = (int) Math.floor(startToEnd.magnitude() / INCREMENT_MAGNITUDE) + 1;
    }

    @Override
    public boolean hasNext() {
        return remaining > 0;
    }

    @Override
    public IntAABB2 next() {
        if (remaining == 0) {
            throw new IllegalStateException("iterator has already ended");
        }

        if (remaining == 1) {
            return getCollisionsAtPosition(end);
        }

        IntAABB2 collisions = getCollisionsAtPosition(currentPos);
        currentPos = currentPos.plus(increment);
        remaining--;
        return collisions;
    }

    private IntAABB2 getCollisionsAtPosition(Vector2 pos) {
        return new IntAABB2(
                (int) Math.floor(pos.x - slideSquareSize / 2.0),
                (int) Math.floor(pos.y - slideSquareSize / 2.0),
                (int) Math.ceil(pos.x + slideSquareSize / 2.0),
                (int) Math.ceil(pos.y + slideSquareSize / 2.0)
        );
    }

}
