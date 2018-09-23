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

/**
 * @author Brady
 * @since 9/23/2018
 */
public final class ParkourResult extends Result {

    public static final ParkourResult IMPOSSIBLE = new ParkourResult(0, 0, COST_INF);

    public final int x, z;

    public ParkourResult(int x, int z, double cost) {
        super(cost);
        this.x = x;
        this.z = z;
    }
}
