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

import baritone.api.pathing.movement.ActionCosts;

/**
 * The result of a calculated movement, with destination x, y, z, and the cost of performing the movement
 *
 * @author leijurv
 */
public final class MutableMoveResult {

    public int x;
    public int y;
    public int z;
    public double cost;
    private MutableMoveResult next;
    private boolean hasNext;

    public MutableMoveResult() {
        reset();
    }

    public final void reset() {
        x = 0;
        y = 0;
        z = 0;
        cost = ActionCosts.COST_INF;
        hasNext = false;
        if (next != null) {
            next.reset();
        }
    }

    public MutableMoveResult nextPotentialDestination() {
        if (next == null) {
            next = new MutableMoveResult(); // this is okay because it's one-time at the beginning (or near the beginning) of the calculation, instead of per-movement
        }
        hasNext = true;
        return next;
    }

    public MutableMoveResult getNext() {
        if (!hasNext) {
            return null;
        }
        return next;
    }
}
