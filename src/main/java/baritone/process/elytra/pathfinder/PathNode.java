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

package baritone.process.elytra.pathfinder;

final class PathNode {

    static final double COST_INF = 1000000.0;

    final NodePos pos;
    final double estimatedCostToGoal;
    double cost = COST_INF;
    double combinedCost = 0;
    PathNode previous = null;
    int heapPosition = -1;

    PathNode(NodePos pos, BlockPos goal) {
        this.pos = pos;
        this.estimatedCostToGoal = heuristic(pos, goal);
    }

    boolean isOpen() {
        return this.heapPosition != -1;
    }

    private static double heuristic(NodePos pos, BlockPos goal) {
        return pos.absolutePosCenter().distanceTo(goal) - (pos.size.width() * 4);
    }
}
