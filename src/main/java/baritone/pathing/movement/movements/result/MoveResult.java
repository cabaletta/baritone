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

package baritone.pathing.movement.movements.result;

import static baritone.pathing.movement.ActionCosts.COST_INF;

public final class MoveResult {
    public static final MoveResult IMPOSSIBLE = new MoveResult(0, 0, 0, COST_INF);
    public final int destX;
    public final int destY;
    public final int destZ;
    public final double cost;

    public MoveResult(int x, int y, int z, double cost) {
        this.destX = x;
        this.destY = y;
        this.destZ = z;
        this.cost = cost;
    }
}
