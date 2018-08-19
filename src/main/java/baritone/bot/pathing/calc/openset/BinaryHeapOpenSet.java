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

import java.util.Arrays;

/**
 * A binary heap implementation of an open set. This is the one used in the AStarPathFinder.
 *
 * @author leijurv
 */
public class BinaryHeapOpenSet implements IOpenSet {

    /**
     * The initial capacity of the heap (2^10)
     */
    private static final int INITIAL_CAPACITY = 1024;

    /**
     * The array backing the heap
     */
    private PathNode[] array;

    /**
     * The size of the heap
     */
    private int size;

    public BinaryHeapOpenSet() {
        this(INITIAL_CAPACITY);
    }

    public BinaryHeapOpenSet(int size) {
        this.size = 0;
        this.array = new PathNode[size];
    }

    @Override
    public final void insert(PathNode value) {
        if (size >= array.length - 1) {
            array = Arrays.copyOf(array, array.length * 2);
        }
        size++;
        value.heapPosition = size;
        array[size] = value;
        update(value);
    }

    @Override
    public final void update(PathNode val) {
        int index = val.heapPosition;
        int parentInd = index >>> 1;
        double cost = val.combinedCost;
        PathNode parentNode = array[parentInd];
        while (index > 1 && parentNode.combinedCost > cost) {
            array[index] = parentNode;
            array[parentInd] = val;
            val.heapPosition = parentInd;
            parentNode.heapPosition = index;
            index = parentInd;
            parentInd = index >>> 1;
            parentNode = array[parentInd];
        }
    }

    @Override
    public final boolean isEmpty() {
        return size == 0;
    }

    @Override
    public final PathNode removeLowest() {
        if (size == 0) {
            throw new IllegalStateException();
        }
        PathNode result = array[1];
        PathNode val = array[size];
        array[1] = val;
        val.heapPosition = 1;
        array[size] = null;
        size--;
        result.heapPosition = -1;
        if (size < 2) {
            return result;
        }
        int index = 1;
        int smallerChild = 2;
        double cost = val.combinedCost;
        do {
            int right = smallerChild + 1;
            PathNode smallerChildNode = array[smallerChild];
            double smallerChildCost = smallerChildNode.combinedCost;
            if (right <= size) {
                PathNode rightChildNode = array[right];
                double rightChildCost = rightChildNode.combinedCost;
                if (smallerChildCost > rightChildCost) {
                    smallerChild = right;
                    smallerChildCost = rightChildCost;
                    smallerChildNode = rightChildNode;
                }
            }
            if (cost <= smallerChildCost) {
                break;
            }
            array[index] = smallerChildNode;
            array[smallerChild] = val;
            val.heapPosition = smallerChild;
            smallerChildNode.heapPosition = index;
            index = smallerChild;
            smallerChild = index << 1;
        } while (smallerChild <= size);
        return result;
    }
}
