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

package baritone.builder;


import java.util.Arrays;

public class NodeBinaryHeap {

    private static final int INITIAL_CAPACITY = 1024;

    private Node[] array;

    private int size;

    public NodeBinaryHeap() {
        this.size = INITIAL_CAPACITY;
        this.array = new Node[this.size];
    }

    public int size() {
        return size;
    }

    public void insert(Node value) {
        if (size >= array.length - 1) {
            array = Arrays.copyOf(array, array.length << 1);
        }
        size++;
        value.heapPosition = size;
        array[size] = value;
        update(value);
    }

    public void update(Node val) {
        int index = val.heapPosition;
        int parentInd = index >>> 1;
        int cost = val.combinedCost;
        Node parentNode = array[parentInd];
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

    public boolean isEmpty() {
        return size == 0;
    }

    public Node removeLowest() {
        if (size == 0) {
            throw new IllegalStateException();
        }
        Node result = array[1];
        Node val = array[size];
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
        int cost = val.combinedCost;
        do {
            Node smallerChildNode = array[smallerChild];
            int smallerChildCost = smallerChildNode.combinedCost;
            if (smallerChild < size) {
                Node rightChildNode = array[smallerChild + 1];
                int rightChildCost = rightChildNode.combinedCost;
                if (smallerChildCost > rightChildCost) {
                    smallerChild++;
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
        } while ((smallerChild <<= 1) <= size);
        return result;
    }
}
