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

/**
 * A helper to get a list of coordinates to "blocks" that touch a line in 2D.
 */
public final class LineBlockIterator {

    private final int startX, startY;
    private final int dirX, dirY;
    private final int mode, maxIter;
    private final double otherAxisDelta;

    private int i = -1;

    public int currX, currY;

    // Sometimes, one iteration can yield two blocks because the point is
    // not integer so it corresponds to two blocks.
    private int nextX, nextY;
    private boolean hasNextBeforeIter = false;

    public LineBlockIterator(int x1, int y1, int x2, int y2) {
        startX = x1;
        startY = y1;

        dirX = x2 >= x1 ? 1 : -1;
        dirY = y2 >= y1 ? 1 : -1;

        int deltaX = x2 - x1;
        int deltaY = y2 - y1;

        int absDeltaX = Math.abs(deltaX);
        int absDeltaY = Math.abs(deltaY);

        // We use the axis with the largest difference as a basis to find
        // points to make sure that we hit all possible points.
        maxIter = Math.max(absDeltaX, absDeltaY);

        if (absDeltaX == 0) {
            mode = 0;
            otherAxisDelta = 0; // unused
        } else if (absDeltaY == 0) {
            mode = 1;
            otherAxisDelta = 0; // unused
        } else if (absDeltaX == absDeltaY) {
            mode = 2;
            otherAxisDelta = 0; // unused
        } else {
            if (Math.abs(absDeltaX) > Math.abs(absDeltaY)) {
                mode = 3;
                otherAxisDelta = deltaY;
            } else {
                mode = 4;
                otherAxisDelta = deltaX;
            }
        }
    }

    public boolean next() {
        if (hasNextBeforeIter) {
            currX = nextX;
            currY = nextY;
            hasNextBeforeIter = false;
            return true;
        }

        if (++i > maxIter) {
            return false;
        }

        if (mode == 0) {
            currX = startX;
            currY = startY + i * dirY;
        } else if (mode == 1) {
            currX = startX + i * dirX;
            currY = startY;
        } else if (mode == 2) {
            currX = startX + i;
            currY = startY + i;
        } else {
            double delta = (double) i / (double) maxIter * otherAxisDelta;

            // see comment in constructor
            if (mode == 3) {
                currX = startX + i * dirX;
                nextX = startX + i * dirX;
                currY = startY + (int) Math.floor(delta);
                nextY = startY + (int) Math.ceil(delta);

                if (currY != nextY) {
                    hasNextBeforeIter = true;
                }
            } else if (mode == 4) {
                currX = startX + (int) Math.floor(delta);
                nextX = startX + (int) Math.ceil(delta);
                currY = startY + i * dirY;
                nextY = startY + i * dirY;

                if (currX != nextX) {
                    hasNextBeforeIter = true;
                }
            }
        }

        return true;
    }

}
