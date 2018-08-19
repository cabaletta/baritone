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
import baritone.bot.pathing.goals.Goal;
import baritone.bot.utils.pathing.BetterBlockPos;
import net.minecraft.util.math.BlockPos;
import org.junit.Test;

import java.util.*;

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

    public void removeAndTest(int amount, IOpenSet[] test, Optional<Collection<PathNode>> mustContain) {
        double[][] results = new double[test.length][amount];
        for (int i = 0; i < test.length; i++) {
            long before = System.currentTimeMillis();
            for (int j = 0; j < amount; j++) {
                PathNode pn = test[i].removeLowest();
                if (mustContain.isPresent() && !mustContain.get().contains(pn)) {
                    throw new IllegalStateException(mustContain.get() + " " + pn);
                }
                results[i][j] = pn.combinedCost;
            }
            System.out.println(test[i].getClass() + " " + (System.currentTimeMillis() - before));
        }
        for (int j = 0; j < amount; j++) {
            for (int i = 1; i < test.length; i++) {
                assertEquals(results[i][j], results[0][j], 0);
            }
        }
        for (int i = 0; i < amount - 1; i++) {
            assertTrue(results[0][i] < results[0][i + 1]);
        }
    }

    public void testSize(int size) {
        System.out.println("Testing size " + size);
        // Include LinkedListOpenSet even though it's not performant because I absolutely trust that it behaves properly
        // I'm really testing the heap implementations against it as the ground truth
        IOpenSet[] test = new IOpenSet[]{new BinaryHeapOpenSet(), new LinkedListOpenSet()};
        for (IOpenSet set : test) {
            assertTrue(set.isEmpty());
        }

        // generate the pathnodes that we'll be testing the sets on
        PathNode[] toInsert = new PathNode[size];
        for (int i = 0; i < size; i++) {
            // can't use an existing goal
            // because they use Baritone.settings()
            // and we can't do that because Minecraft itself isn't initted
            PathNode pn = new PathNode(new BetterBlockPos(0, 0, 0), new Goal() {
                @Override
                public boolean isInGoal(BlockPos pos) {
                    return false;
                }

                @Override
                public double heuristic(BlockPos pos) {
                    return 0;
                }
            });
            pn.combinedCost = Math.random();
            toInsert[i] = pn;
        }

        // create a list of what the first removals should be
        ArrayList<PathNode> copy = new ArrayList<>(Arrays.asList(toInsert));
        copy.sort(Comparator.comparingDouble(pn -> pn.combinedCost));
        Set<PathNode> lowestQuarter = new HashSet<>(copy.subList(0, size / 4));

        // all opensets should be empty; nothing has been inserted yet
        for (IOpenSet set : test) {
            assertTrue(set.isEmpty());
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

        // all opensets should now be full
        for (IOpenSet set : test) {
            assertFalse(set.isEmpty());
        }

        System.out.println("Removal round 1");
        // remove a quarter of the nodes and verify that they are indeed the size/4 lowest ones
        removeAndTest(size / 4, test, Optional.of(lowestQuarter));

        // none of them should be empty (sanity check)
        for (IOpenSet set : test) {
            assertFalse(set.isEmpty());
        }
        int cnt = 0;
        for (int i = 0; cnt < size / 2 && i < size; i++) {
            if (lowestQuarter.contains(toInsert[i])) { // these were already removed and can't be updated to test
                continue;
            }
            toInsert[i].combinedCost *= Math.random();
            // multiplying it by a random number between 0 and 1 is guaranteed to decrease it
            for (IOpenSet set : test) {
                // it's difficult to benchmark these individually because if you modify all at once then update then
                // it breaks the internal consistency of the heaps.
                // you have to call update every time you modify a node.
                set.update(toInsert[i]);
            }
            cnt++;
        }

        //still shouldn't be empty
        for (IOpenSet set : test) {
            assertFalse(set.isEmpty());
        }

        System.out.println("Removal round 2");
        // remove the remaining 3/4
        removeAndTest(size - size / 4, test, Optional.empty());

        // every set should now be empty
        for (IOpenSet set : test) {
            assertTrue(set.isEmpty());
        }
    }
}