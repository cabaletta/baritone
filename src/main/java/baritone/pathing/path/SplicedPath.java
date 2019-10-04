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

package baritone.pathing.path;

import baritone.api.pathing.calc.IPath;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.movement.IMovement;
import baritone.api.utils.BetterBlockPos;
import baritone.utils.pathing.PathBase;

import java.util.*;

public class SplicedPath extends PathBase {

    private final List<BetterBlockPos> path;

    private final List<IMovement> movements;

    private final int numNodes;

    private final Goal goal;

    private SplicedPath(List<BetterBlockPos> path, List<IMovement> movements, int numNodesConsidered, Goal goal) {
        this.path = path;
        this.movements = movements;
        this.numNodes = numNodesConsidered;
        this.goal = goal;
        sanityCheck();
    }

    @Override
    public Goal getGoal() {
        return goal;
    }

    @Override
    public List<IMovement> movements() {
        return Collections.unmodifiableList(movements);
    }

    @Override
    public List<BetterBlockPos> positions() {
        return Collections.unmodifiableList(path);
    }

    @Override
    public int getNumNodesConsidered() {
        return numNodes;
    }

    @Override
    public int length() {
        return path.size();
    }

    public static Optional<SplicedPath> trySplice(IPath first, IPath second, boolean allowOverlapCutoff) {
        if (second == null || first == null) {
            return Optional.empty();
        }
        if (!first.getDest().equals(second.getSrc())) {
            return Optional.empty();
        }
        HashSet<BetterBlockPos> secondPos = new HashSet<>(second.positions());
        int firstPositionInSecond = -1;
        for (int i = 0; i < first.length() - 1; i++) { // overlap in the very last element is fine (and required) so only go up to first.length() - 1
            if (secondPos.contains(first.positions().get(i))) {
                firstPositionInSecond = i;
                break;
            }
        }
        if (firstPositionInSecond != -1) {
            if (!allowOverlapCutoff) {
                return Optional.empty();
            }
        } else {
            firstPositionInSecond = first.length() - 1;
        }
        int positionInSecond = second.positions().indexOf(first.positions().get(firstPositionInSecond));
        if (!allowOverlapCutoff && positionInSecond != 0) {
            throw new IllegalStateException();
        }
        List<BetterBlockPos> positions = new ArrayList<>();
        List<IMovement> movements = new ArrayList<>();
        positions.addAll(first.positions().subList(0, firstPositionInSecond + 1));
        movements.addAll(first.movements().subList(0, firstPositionInSecond));

        positions.addAll(second.positions().subList(positionInSecond + 1, second.length()));
        movements.addAll(second.movements().subList(positionInSecond, second.length() - 1));
        return Optional.of(new SplicedPath(positions, movements, first.getNumNodesConsidered() + second.getNumNodesConsidered(), first.getGoal()));
    }
}
