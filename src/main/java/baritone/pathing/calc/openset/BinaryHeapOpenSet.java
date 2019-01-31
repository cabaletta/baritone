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

import java.util.Arrays;

/**
 * A binary heap implementation of an open set. This is the one used in the AStarPathFinder.
 *
 * @author leijurv
 */
public final class BinaryHeapOpenSet implements IOpenSet {

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

    public int size() {
        return size;
    }

    @Override
    public final void insert(PathNode value) {
        if (size >= array.length - 1) {
            array = Arrays.copyOf(array, array.length << 1);
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
        PathNode[] array = this.array;
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
        PathNode[] array = this.array;
        PathNode result = array[1];
        PathNode val = array[size];
        array[1] = val;
        val.heapPosition = 1;
        array[size] = null;
        int size = --this.size;
        result.heapPosition = -1;
        if (size < 2) {
            return result;
        }
        int index = 1;
        int smallerChild = 1;
        double cost = val.combinedCost;
        while ((smallerChild <<= 1) < size) {
            PathNode smallerChildNode = array[smallerChild];
            PathNode otherChildNode = array[smallerChild + 1];
            double smallerChildCost = smallerChildNode.combinedCost;
            double rightChildCost = otherChildNode.combinedCost;
            if (smallerChildCost > rightChildCost) {
                smallerChild++;
                smallerChildCost = rightChildCost;
                smallerChildNode = otherChildNode;
            }
            if (cost <= smallerChildCost) {
                return result;
            }
            array[index] = smallerChildNode;
            array[smallerChild] = val;
            val.heapPosition = smallerChild;
            smallerChildNode.heapPosition = index;
            index = smallerChild;
        }
        // if we get here, then smallerChild >= size
        // one last swap to check
        if (smallerChild == size) {
            PathNode onlyChildNode = array[smallerChild];
            if (cost > onlyChildNode.combinedCost) {
                array[index] = onlyChildNode;
                array[smallerChild] = val;
                val.heapPosition = smallerChild;
                onlyChildNode.heapPosition = index;
            }
        }
        return result;
    }
}
