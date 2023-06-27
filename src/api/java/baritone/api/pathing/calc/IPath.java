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

package baritone.api.pathing.calc;

import baritone.api.Settings;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.movement.IMovement;
import baritone.api.utils.BetterBlockPos;

import java.util.HashSet;
import java.util.List;

/**
 * @author leijurv, Brady
 */
public interface IPath {

    /**
     * Ordered list of movements to carry out.
     * movements.get(i).getSrc() should equal positions.get(i)
     * movements.get(i).getDest() should equal positions.get(i+1)
     * movements.size() should equal positions.size()-1
     *
     * @return All of the movements to carry out
     */
    List<IMovement> movements();

    /**
     * All positions along the way.
     * Should begin with the same as getSrc and end with the same as getDest
     *
     * @return All of the positions along this path
     */
    List<BetterBlockPos> positions();

    /**
     * This path is actually going to be executed in the world. Do whatever additional processing is required.
     * (as opposed to Path objects that are just constructed every frame for rendering)
     *
     * @return The result of path post processing
     */
    default IPath postProcess() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the number of positions in this path. Equivalent to {@code positions().size()}.
     *
     * @return Number of positions in this path
     */
    default int length() {
        return positions().size();
    }

    /**
     * @return The goal that this path was calculated towards
     */
    Goal getGoal();

    /**
     * Returns the number of nodes that were considered during calculation before
     * this path was found.
     *
     * @return The number of nodes that were considered before finding this path
     */
    int getNumNodesConsidered();

    /**
     * Returns the start position of this path. This is the first element in the
     * {@link List} that is returned by {@link IPath#positions()}.
     *
     * @return The start position of this path
     */
    default BetterBlockPos getSrc() {
        return positions().get(0);
    }

    /**
     * Returns the end position of this path. This is the last element in the
     * {@link List} that is returned by {@link IPath#positions()}.
     *
     * @return The end position of this path.
     */
    default BetterBlockPos getDest() {
        List<BetterBlockPos> pos = positions();
        return pos.get(pos.size() - 1);
    }

    /**
     * Returns the estimated number of ticks to complete the path from the given node index.
     *
     * @param pathPosition The index of the node we're calculating from
     * @return The estimated number of ticks remaining frm the given position
     */
    default double ticksRemainingFrom(int pathPosition) {
        double sum = 0;
        //this is fast because we aren't requesting recalculation, it's just cached
        List<IMovement> movements = movements();
        for (int i = pathPosition; i < movements.size(); i++) {
            sum += movements.get(i).getCost();
        }
        return sum;
    }

    /**
     * Cuts off this path at the loaded chunk border, and returns the resulting path. Default
     * implementation just returns this path, without the intended functionality.
     * <p>
     * The argument is supposed to be a BlockStateInterface LOL LOL LOL LOL LOL
     *
     * @param bsi The block state lookup, highly cursed
     * @return The result of this cut-off operation
     */
    default IPath cutoffAtLoadedChunks(Object bsi) {
        throw new UnsupportedOperationException();
    }

    /**
     * Cuts off this path using the min length and cutoff factor settings, and returns the resulting path.
     * Default implementation just returns this path, without the intended functionality.
     *
     * @param destination The end goal of this path
     * @return The result of this cut-off operation
     * @see Settings#pathCutoffMinimumLength
     * @see Settings#pathCutoffFactor
     */
    default IPath staticCutoff(Goal destination) {
        throw new UnsupportedOperationException();
    }


    /**
     * Performs a series of checks to ensure that the assembly of the path went as expected.
     */
    default void sanityCheck() {
        List<BetterBlockPos> path = positions();
        List<IMovement> movements = movements();
        if (!getSrc().equals(path.get(0))) {
            throw new IllegalStateException("Start node does not equal first path element");
        }
        if (!getDest().equals(path.get(path.size() - 1))) {
            throw new IllegalStateException("End node does not equal last path element");
        }
        if (path.size() != movements.size() + 1) {
            throw new IllegalStateException("Size of path array is unexpected");
        }
        HashSet<BetterBlockPos> seenSoFar = new HashSet<>();
        for (int i = 0; i < path.size() - 1; i++) {
            BetterBlockPos src = path.get(i);
            BetterBlockPos dest = path.get(i + 1);
            IMovement movement = movements.get(i);
            if (!src.equals(movement.getSrc())) {
                throw new IllegalStateException("Path source is not equal to the movement source");
            }
            if (!dest.equals(movement.getDest())) {
                throw new IllegalStateException("Path destination is not equal to the movement destination");
            }
            if (seenSoFar.contains(src)) {
                throw new IllegalStateException("Path doubles back on itself, making a loop");
            }
            seenSoFar.add(src);
        }
    }
}
