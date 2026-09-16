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

    // keys[i] is always array[i].combinedCost
    // so sifting compares doubles that are right next to each other instead of chasing a pointer per level
    // into some PathNode that fell out of cache ages ago. this was worth 15% by itself, i was not expecting that
    private double[] keys;

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
        this.keys = new double[size];
    }

    public int size() {
        return size;
    }

    @Override
    public final void insert(PathNode value) {
        if (size >= array.length - 1) {
            array = Arrays.copyOf(array, array.length << 1);
            keys = Arrays.copyOf(keys, keys.length << 1);
        }
        size++;
        siftUp(size, value, value.combinedCost);
    }

    @Override
    public final void update(PathNode val) {
        // cost only ever goes down so it only ever goes up (the heap i mean)
        siftUp(val.heapPosition, val, val.combinedCost);
    }

    private void siftUp(int index, PathNode val, double cost) {
        PathNode[] array = this.array;
        double[] keys = this.keys;
        int parentInd = index >>> 1;
        while (index > 1 && keys[parentInd] > cost) {
            PathNode parentNode = array[parentInd];
            array[index] = parentNode;
            keys[index] = keys[parentInd];
            parentNode.heapPosition = index;
            index = parentInd;
            parentInd = index >>> 1;
        }
        array[index] = val;
        keys[index] = cost;
        val.heapPosition = index;
    }

    @Override
    public final boolean isEmpty() {
        return size == 0;
    }

    @Override
    public final PathNode removeLowest() {
        if (size == 0) {
            throw new IllegalStateException("Cannot remove from empty heap");
        }
        PathNode[] array = this.array;
        double[] keys = this.keys;
        PathNode result = array[1];
        result.heapPosition = -1;
        PathNode val = array[size];
        double cost = keys[size];
        array[size] = null;
        size--;
        if (size == 0) {
            return result;
        }
        int index = 1;
        int child = 2;
        while (child <= size) {
            double childCost = keys[child];
            if (child < size && keys[child + 1] < childCost) {
                child++;
                childCost = keys[child];
            }
            if (cost <= childCost) {
                break;
            }
            PathNode childNode = array[child];
            array[index] = childNode;
            keys[index] = childCost;
            childNode.heapPosition = index;
            index = child;
            child = index << 1;
        }
        array[index] = val;
        keys[index] = cost;
        val.heapPosition = index;
        return result;
    }
}
