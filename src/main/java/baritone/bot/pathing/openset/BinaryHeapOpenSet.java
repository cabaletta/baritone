package baritone.bot.pathing.openset;

import baritone.bot.pathing.calc.PathNode;

import java.util.Arrays;

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

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
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

    /**
     * Swaps the elements at the specified indices.
     *
     * @param index1 The first index
     * @param index2 The second index
     */
    protected void swap(int index1, int index2) {
        PathNode tmp = array[index1];
        array[index1] = array[index2];
        array[index2] = tmp;
    }
}
