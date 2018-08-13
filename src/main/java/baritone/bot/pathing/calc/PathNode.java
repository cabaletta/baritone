/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.bot.pathing.calc;

import baritone.bot.pathing.goals.Goal;
import baritone.bot.pathing.movement.Movement;
import net.minecraft.util.math.BlockPos;

import java.util.Objects;

/**
 * A node in the path, containing the cost and steps to get to it.
 *
 * @author leijurv
 */
public class PathNode {

    /**
     * The position of this node
     */
    final BlockPos pos;

    /**
     * The goal it's going towards
     */
    final Goal goal;

    /**
     * Cached, should always be equal to goal.heuristic(pos)
     */
    final double estimatedCostToGoal;

    /**
     * Total cost of getting from start to here
     * Mutable and changed by PathFinder
     */
    double cost;

    /**
     * Should always be equal to estimatedCosttoGoal + cost
     * Mutable and changed by PathFinder
     */
    public double combinedCost;

    /**
     * In the graph search, what previous node contributed to the cost
     * Mutable and changed by PathFinder
     */
    PathNode previous;

    /**
     * In the graph search, what previous movement (edge) was taken to get to here
     * Mutable and changed by PathFinder
     */
    Movement previousMovement;

    /**
     * Is this a member of the open set in A*? (only used during pathfinding)
     * Instead of doing a costly member check in the open set, cache membership in each node individually too.
     */
    boolean isOpen;

    /**
     * Where is this node in the array flattenization of the binary heap? Needed for decrease-key operations.
     */
    public int heapPosition;

    public PathNode(BlockPos pos, Goal goal) {
        this.pos = pos;
        this.previous = null;
        this.cost = Short.MAX_VALUE;
        this.goal = goal;
        this.estimatedCostToGoal = goal.heuristic(pos);
        this.previousMovement = null;
        this.isOpen = false;
    }

    /**
     * TODO: Possibly reimplement hashCode and equals. They are necessary for this class to function but they could be done better
     *
     * @return The hash code value for this {@link PathNode}
     */
    @Override
    public int hashCode() {
        int hash = 3241;
        hash = 3457689 * hash + this.pos.getX();
        hash = 8734625 * hash + this.pos.getY();
        hash = 2873465 * hash + this.pos.getZ();
        // Don't call goal.hashCode(). this calls objects hashcode to verify that the actual goal objects are == identical, which is important for node caching
        hash = 3241543 * hash + Objects.hashCode(this.goal);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof PathNode))
            return false;

        final PathNode other = (PathNode) obj;
        return Objects.equals(this.pos, other.pos) && Objects.equals(this.goal, other.goal);
    }
}
