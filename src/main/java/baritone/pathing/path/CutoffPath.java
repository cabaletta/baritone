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

import baritone.pathing.goals.Goal;
import baritone.pathing.movement.Movement;
import baritone.utils.pathing.BetterBlockPos;

import java.util.Collections;
import java.util.List;

public class CutoffPath implements IPath {

    private final List<BetterBlockPos> path;

    private final List<Movement> movements;

    private final int numNodes;

    private final Goal goal;

    public CutoffPath(IPath prev, int lastPositionToInclude) {
        path = prev.positions().subList(0, lastPositionToInclude + 1);
        movements = prev.movements().subList(0, lastPositionToInclude + 1);
        numNodes = prev.getNumNodesConsidered();
        goal = prev.getGoal();
    }

    @Override
    public Goal getGoal() {
        return goal;
    }

    @Override
    public List<Movement> movements() {
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
}
