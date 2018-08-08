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

package baritone.bot.pathing.calc.openset;

import baritone.bot.pathing.calc.PathNode;
import baritone.bot.pathing.util.FibonacciHeap;

/**
 * Wrapper adapter between FibonacciHeap and OpenSet
 *
 * @author leijurv
 */
public class FibonacciHeapOpenSet extends FibonacciHeap implements IOpenSet {

    @Override
    public void insert(PathNode node) {
        super.insert(node, node.combinedCost);
    }

    @Override
    public PathNode removeLowest() {
        PathNode pn = super.removeMin();
        pn.parent = null;
        return pn;
    }

    public void update(PathNode node) {
        super.decreaseKey(node.parent, node.combinedCost);
    }
}
