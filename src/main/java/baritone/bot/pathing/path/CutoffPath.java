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

package baritone.bot.pathing.path;

import baritone.bot.pathing.movement.Movement;
import baritone.bot.utils.pathing.BetterBlockPos;

import java.util.Collections;
import java.util.List;

public class CutoffPath implements IPath {

    final List<BetterBlockPos> path;

    final List<Movement> movements;

    private final int numNodes;

    public CutoffPath(IPath prev, int lastPositionToInclude) {
        path = prev.positions().subList(0, lastPositionToInclude + 1);
        movements = prev.movements().subList(0, lastPositionToInclude + 1);
        numNodes = prev.getNumNodesConsidered();
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
