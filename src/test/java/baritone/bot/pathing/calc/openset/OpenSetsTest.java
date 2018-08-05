package baritone.bot.pathing.calc.openset;

import baritone.bot.pathing.calc.PathNode;
import baritone.bot.pathing.goals.GoalBlock;
import net.minecraft.util.math.BlockPos;
import org.junit.Test;

import static org.junit.Assert.*;

public class OpenSetsTest {

    @Test
    public void testOpenSets() {
        for (int size = 1; size < 100; size++) {
            testSize(size);
        }
        for (int size = 100; size < 10000; size += 100) {
            testSize(size);
        }
    }

    public void testSize(int size) {
        System.out.println("Testing size " + size);
        // Include LinkedListOpenSet even though it's not performant because I absolutely trust that it behaves properly
        // I'm really testing the heap implementations against it as the ground truth
        IOpenSet[] test = new IOpenSet[]{new BinaryHeapOpenSet(), new LinkedListOpenSet(), new FibonacciHeapOpenSet()};
        for (IOpenSet set : test) {
            assertTrue(set.isEmpty());
        }
        PathNode[] toInsert = new PathNode[size];
        for (int i = 0; i < size; i++) {
            PathNode pn = new PathNode(new BlockPos(0, 0, 0), new GoalBlock(new BlockPos(0, 0, 0)));
            pn.combinedCost = Math.random();
            toInsert[i] = pn;

        }
        System.out.println("Insertion");
        for (IOpenSet set : test) {
            long before = System.currentTimeMillis();
            for (int i = 0; i < size; i++)
                set.insert(toInsert[i]);
            System.out.println(set.getClass() + " " + (System.currentTimeMillis() - before));
            //all three take either 0 or 1ms to insert up to 10,000 nodes
            //linkedlist takes 0ms most often (because there's no array resizing or allocation there, just pointer shuffling)
        }
        for (IOpenSet set : test) {
            assertFalse(set.isEmpty());
        }
        System.out.println("Removal");
        double[][] results = new double[test.length][size];
        for (int i = 0; i < test.length; i++) {
            long before = System.currentTimeMillis();
            for (int j = 0; j < size; j++) {
                results[i][j] = test[i].removeLowest().combinedCost;
            }
            System.out.println(test[i].getClass() + " " + (System.currentTimeMillis() - before));
        }
        for (int j = 0; j < size; j++) {
            for (int i = 1; i < test.length; i++) {
                assertEquals(results[i][j], results[0][j], 0);
            }
        }
        for (IOpenSet set : test) {
            assertTrue(set.isEmpty());
        }
    }
}