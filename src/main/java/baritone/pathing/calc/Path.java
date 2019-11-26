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

package baritone.pathing.calc;

import baritone.Baritone;
import baritone.api.pathing.calc.IPath;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.movement.IMovement;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.Helper;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.Movement;
import baritone.pathing.movement.Moves;
import baritone.pathing.movement.movements.straight.MovementStraight;
import baritone.pathing.path.CutoffPath;
import baritone.utils.pathing.PathBase;

import java.util.*;

/**
 * A node based implementation of IPath
 *
 * @author leijurv
 */
class Path extends PathBase {

    private static final double STRAIGHT_BETTER_THRESHOLD = 0.95;

    /**
     * The start position of this path
     */
    private final BetterBlockPos start;

    /**
     * The end position of this path
     */
    private final BetterBlockPos end;

    /**
     * The blocks on the path. Guaranteed that path.get(0) equals start and
     * path.get(path.size()-1) equals end
     */
    private final List<BetterBlockPos> path;

    private final List<Movement> movements;

    private final List<PathNode> nodes;

    private final Goal goal;

    private final int totalNodeCount;

    private final CalculationContext context;

    private volatile boolean verified;

    Path(PathNode start, PathNode end, int totalNodeCount, Goal goal, CalculationContext context) {
        this.start = new BetterBlockPos(start.x, start.y, start.z);
        this.end = new BetterBlockPos(end.x, end.y, end.z);
        this.totalNodeCount = totalNodeCount;
        this.movements = new ArrayList<>();
        this.goal = goal;
        this.context = context;
        int pathNodeCount = countNodesBackwards(end);
        this.path = new ArrayList<>(pathNodeCount);
        this.nodes = new ArrayList<>(pathNodeCount);
        this.insertNodesAndPath(end, pathNodeCount);
    }

    private static int countNodesBackwards(PathNode end) {
        int count = 0;
        PathNode current = end;
        while (current != null) {
            current = current.previous;
            count++;
        }
        return count;
    }

    private void insertNodesAndPath(PathNode end, int count) {
        for (int i = 0; i < count; i++) {
            this.path.add(null);
            this.nodes.add(null);
        }

        // fill backwards
        PathNode current = end;
        while (--count >= 0) {
            this.path.set(count, current.getPosition());
            this.nodes.set(count, current);
            current = current.previous;
        }
    }

    @Override
    public Goal getGoal() {
        return goal;
    }

    private boolean assembleMovements() {
        if (path.isEmpty() || !movements.isEmpty()) {
            throw new IllegalStateException();
        }
        for (int i = 0; i < path.size() - 1; i++) {
            double cost = nodes.get(i + 1).cost - nodes.get(i).cost;
            Movement move = runBackwards(path.get(i), path.get(i + 1), cost);
            if (move == null) {
                return true;
            } else {
                movements.add(move);
            }
        }
        return false;
    }

    private double getStraightMovementCost(BetterBlockPos src, BetterBlockPos dest) {
        MovementStraight movement = new MovementStraight(context.baritone, src, dest);
        return movement.calculateCost(context);
    }

    private boolean simplificationHelper(ArrayList<Movement> list, BetterBlockPos src, BetterBlockPos dest) {
        if (dest == null) {
            return true;
        }
        MovementStraight simplified = new MovementStraight(context.baritone, src, dest);
        double cost = simplified.calculateCost(context);
        if (cost >= Movement.COST_INF) {
            Helper.HELPER.logDebug("straight movement became impossible during simplification?!");
            return false;
        }
        simplified.override(cost);
        list.add(simplified);
        return true;
    }

    private void simplifyMovements() {
        ArrayList<Movement> tmp = new ArrayList<>();
        BetterBlockPos straightSrc = null;
        BetterBlockPos straightDest = null;
        double nonStraightCost = 0.0;
        for (Movement move : movements) {
            BetterBlockPos moveDest = move.getDest();
            nonStraightCost += move.getCost();
            if (straightSrc != null && getStraightMovementCost(straightSrc, moveDest) * STRAIGHT_BETTER_THRESHOLD <= nonStraightCost) {
                straightDest = moveDest;
            } else {
                if (!simplificationHelper(tmp, straightSrc, straightDest)) {
                    return;
                }
                nonStraightCost = 0.0;
                straightSrc = moveDest;
                straightDest = null;
                tmp.add(move);
            }
        }
        if (!simplificationHelper(tmp, straightSrc, straightDest)) {
            return;
        }
        movements.clear();
        movements.addAll(tmp);
    }

    private void recreateNodesAndPathFromMovements() {
        if (this.movements.isEmpty()) {
            throw new IllegalStateException();
        }
        this.nodes.clear();
        this.path.clear();
        PathNode previous = null;
        for (int i = -1; i < this.movements.size(); i++) {
            BetterBlockPos pos;
            double cost;
            if (i == -1) {
                pos = this.movements.get(0).getSrc();
                cost = 0.0;
            } else {
                Movement movement = this.movements.get(i);
                pos = movement.getDest();
                cost = movement.getCost();
            }
            PathNode node = new PathNode(pos.x, pos.y, pos.z, goal);
            node.cost = cost;
            node.previous = previous;
            previous = node;
            this.nodes.add(node);
            this.path.add(pos);
        }
    }

    private Movement runBackwards(BetterBlockPos src, BetterBlockPos dest, double originalCost) {
        for (Moves moves : Moves.values()) {
            Movement move = moves.apply0(context, src);
            if (move.getDest().equals(dest)) {
                // have to calculate the cost at calculation time so we can accurately judge whether a cost increase happened between cached calculation and real execution
                // however, taking into account possible favoring that could skew the node cost, we really want the stricter limit of the two
                // so we take the minimum of the path node cost difference, and the calculated cost
                move.override(Math.min(move.calculateCost(context), originalCost));
                return move;
            }
        }
        // this is no longer called from bestPathSoFar, now it's in postprocessing
        Helper.HELPER.logDebug("Movement became impossible during calculation " + src + " " + dest + " " + dest.subtract(src));
        return null;
    }

    @Override
    public IPath postProcess() {
        if (verified) {
            throw new IllegalStateException();
        }
        verified = true;
        boolean failed = assembleMovements();
        if (Baritone.settings().experimentalSimplifyPath.value && !Baritone.settings().avoidance.value) {
            simplifyMovements();
            recreateNodesAndPathFromMovements();
        }
        movements.forEach(m -> m.checkLoadedChunk(context));

        if (failed) { // at least one movement became impossible during calculation
            CutoffPath res = new CutoffPath(this, movements().size());
            if (res.movements().size() != movements.size()) {
                throw new IllegalStateException();
            }
            return res;
        }
        // more post processing here
        sanityCheck();
        return this;
    }

    @Override
    public List<IMovement> movements() {
        if (!verified) {
            throw new IllegalStateException();
        }
        return Collections.unmodifiableList(movements);
    }

    @Override
    public List<BetterBlockPos> positions() {
        return Collections.unmodifiableList(path);
    }

    @Override
    public int getNumNodesConsidered() {
        return totalNodeCount;
    }

    @Override
    public BetterBlockPos getSrc() {
        return start;
    }

    @Override
    public BetterBlockPos getDest() {
        return end;
    }
}
