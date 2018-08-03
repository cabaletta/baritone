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
class PathNode {

    final BlockPos pos;

    final Goal goal;

    final double estimatedCostToGoal;

    // These three fields are mutable and are changed by PathFinder
    double cost;

    PathNode previous;

    Movement previousMovement;

    /**
     * Is this a member of the open set in A*? (only used during pathfinding)
     */
    boolean isOpen;

    /**
     * In the linked list of open nodes, which one is next? (only used during pathfinding)
     */
    PathNode nextOpen;

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
