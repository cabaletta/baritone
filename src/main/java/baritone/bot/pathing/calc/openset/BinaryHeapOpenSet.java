package baritone.bot.pathing.calc.openset;

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
        value.heapPosition = index;
        array[index] = value;
        upHeap(index);
    }

    public void update(PathNode node) {
        upHeap(node.heapPosition);
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
        array[1].heapPosition = 1;
        array[size] = null;
        size--;
        result.heapPosition = -1;
        if (size < 2) {
            return result;
        }
        int index = 1;
        int smallerChild = 2;
        double cost = array[index].combinedCost;
        do {
            int right = smallerChild + 1;
            double smallerChildCost = array[smallerChild].combinedCost;
            if (right <= size) {
                double rightChildCost = array[right].combinedCost;
                if (smallerChildCost > rightChildCost) {
                    smallerChild = right;
                    smallerChildCost = rightChildCost;
                }
            }
            if (cost <= smallerChildCost) {
                break;
            }
            swap(index, smallerChild);
            index = smallerChild;
            smallerChild = index << 1;
        } while (smallerChild <= size);
        return result;
    }

    private void upHeap(int index) {
        int parent = index >>> 1;
        double cost = array[index].combinedCost;
        while (index > 1 && array[parent].combinedCost > cost) {
            swap(index, parent);
            index = parent;
            parent = index >>> 1;
        }
    }


    /**
     * Swaps the elements at the specified indices.
     *
     * @param index1 The first index
     * @param index2 The second index
     */
    protected void swap(int index1, int index2) {
        //sanity checks, disabled because of performance hit
        //if (array[index1].heapPosition != index1) throw new IllegalStateException();
        //if (array[index2].heapPosition != index2) throw new IllegalStateException();
        PathNode tmp = array[index1];
        array[index1] = array[index2];
        array[index2] = tmp;
        tmp.heapPosition = index2;
        array[index1].heapPosition = index1;
    }

}
