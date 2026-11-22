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

package baritone.process.elytra.pathfinder;

import java.util.Arrays;

/** Baritone's binary heap, as the native library also has it. */
final class BinaryHeapOpenSet {

    private static final int INITIAL_CAPACITY = 1024;

    private PathNode[] array = new PathNode[INITIAL_CAPACITY];
    private int size = 0;

    int getSize() {
        return this.size;
    }

    boolean isEmpty() {
        return this.size == 0;
    }

    void insert(PathNode value) {
        if (this.size >= this.array.length - 1) {
            this.array = Arrays.copyOf(this.array, this.array.length << 1);
        }
        this.size++;
        value.heapPosition = this.size;
        this.array[this.size] = value;
        update(value);
    }

    void update(PathNode val) {
        int index = val.heapPosition;
        int parentIndex = index >>> 1;
        final double cost = val.combinedCost;
        PathNode parentNode = this.array[parentIndex];
        while (index > 1 && parentNode.combinedCost > cost) {
            this.array[index] = parentNode;
            this.array[parentIndex] = val;
            val.heapPosition = parentIndex;
            parentNode.heapPosition = index;
            index = parentIndex;
            parentIndex = index >>> 1;
            parentNode = this.array[parentIndex];
        }
    }

    PathNode removeLowest() {
        if (this.size == 0) {
            throw new IllegalStateException("empty");
        }
        final PathNode result = this.array[1];
        final PathNode val = this.array[this.size];
        this.array[1] = val;
        val.heapPosition = 1;
        this.array[this.size] = null;
        this.size--;
        result.heapPosition = -1;
        if (this.size < 2) {
            return result;
        }
        int index = 1;
        int smallerChild = 2;
        final double cost = val.combinedCost;
        do {
            PathNode smallerChildNode = this.array[smallerChild];
            double smallerChildCost = smallerChildNode.combinedCost;
            if (smallerChild < this.size) {
                final PathNode rightChildNode = this.array[smallerChild + 1];
                final double rightChildCost = rightChildNode.combinedCost;
                if (smallerChildCost > rightChildCost) {
                    smallerChild++;
                    smallerChildCost = rightChildCost;
                    smallerChildNode = rightChildNode;
                }
            }
            if (cost <= smallerChildCost) {
                break;
            }
            this.array[index] = smallerChildNode;
            this.array[smallerChild] = val;
            val.heapPosition = smallerChild;
            smallerChildNode.heapPosition = index;
            index = smallerChild;
        } while ((smallerChild <<= 1) <= this.size);
        return result;
    }
}
