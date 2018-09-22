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

package baritone.pathing.calc.openset;

import baritone.pathing.calc.PathNode;

/**
 * An open set for A* or similar graph search algorithm
 *
 * @author leijurv
 */
public interface IOpenSet {

    /**
     * Inserts the specified node into the heap
     *
     * @param node The node
     */
    void insert(PathNode node);

    /**
     * @return {@code true} if the heap has no elements; {@code false} otherwise.
     */
    boolean isEmpty();

    /**
     * Removes and returns the minimum element in the heap.
     *
     * @return The minimum element in the heap
     */
    PathNode removeLowest();

    /**
     * A faster path has been found to this node, decreasing its cost. Perform a decrease-key operation.
     *
     * @param node The node
     */
    void update(PathNode node);
}
