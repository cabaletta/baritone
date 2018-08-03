package baritone.bot.pathing.calc;

import java.util.Arrays;

public class BinaryHeapOpenSet implements IOpenSet {
    private static final int INITIAL_CAPACITY = 1024;
    private PathNode[] array;
    private int size;

    public BinaryHeapOpenSet() {
        this(INITIAL_CAPACITY);
    }

    public BinaryHeapOpenSet(int size) {
        this.size = 0;
        this.array = new PathNode[size];
    }

    public void insert(PathNode value) {
        if (size >= array.length - 1) {
            array = Arrays.copyOf(array, array.length * 2);
        }
        size++;
        int index = size;
        array[index] = value;
        int parent = index >>> 1;
        while (index > 1 && array[parent].combinedCost > array[index].combinedCost) {
            swap(index, parent);
            index = parent;
            parent = index >>> 1;
        }
    }

    /**
     * Returns true if the heap has no elements; false otherwise.
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Removes and returns the minimum element in the heap.
     */
    public PathNode removeLowest() {
        if (size == 0) {
            throw new IllegalStateException();
        }
        PathNode result = array[1];
        array[1] = array[size];
        array[size] = null;
        size--;
        int index = 1;
        int smallerChild = 2;
        while (smallerChild <= size) {
            int right = smallerChild + 1;
            if (right <= size && array[smallerChild].combinedCost > array[right].combinedCost) {
                smallerChild = right;
            }
            if (array[index].combinedCost > array[smallerChild].combinedCost) {
                swap(index, smallerChild);
            } else {
                break;
            }
            index = smallerChild;
            smallerChild = index << 1;
        }
        return result;
    }

    protected void swap(int index1, int index2) {
        PathNode tmp = array[index1];
        array[index1] = array[index2];
        array[index2] = tmp;
    }
}
