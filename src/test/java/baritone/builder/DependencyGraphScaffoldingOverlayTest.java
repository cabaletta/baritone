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

import baritone.api.utils.BetterBlockPos;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

public class DependencyGraphScaffoldingOverlayTest {
    @Test
    public void testLarge() throws InterruptedException {
        Random RAND = new Random(5021);
        long aaa = System.currentTimeMillis();
        BlockStateCachedData[][][] test = new BlockStateCachedData[64][64][64];
        int numRealBlocks = 0;
        for (int x = 0; x < test.length; x++) {
            for (int y = 0; y < test[0].length; y++) {
                for (int z = 0; z < test[0][0].length; z++) {
                    if (RAND.nextInt(10) < 2) {
                        test[x][y][z] = FakeStates.probablyCanBePlaced(RAND);
                        numRealBlocks++;
                    } else {
                        test[x][y][z] = FakeStates.AIR;
                    }
                }

            }
        }
        PackedBlockStateCuboid states = new PackedBlockStateCuboid(test);
        PlaceOrderDependencyGraph graph = new PlaceOrderDependencyGraph(states);
        long aa = System.currentTimeMillis();
        DependencyGraphScaffoldingOverlay scaffolding = new DependencyGraphScaffoldingOverlay(graph);
        long a = System.currentTimeMillis();
        StringBuilder report = new StringBuilder(scaffolding.getCollapsedGraph().getComponents().size() + "\n");
        int goal = numRealBlocks + 200000;
        while (numRealBlocks < goal) {
            int x = RAND.nextInt(((CuboidBounds) scaffolding.bounds()).sizeX);
            int y = RAND.nextInt(((CuboidBounds) scaffolding.bounds()).sizeY);
            int z = RAND.nextInt(((CuboidBounds) scaffolding.bounds()).sizeZ);
            long pos = BetterBlockPos.toLong(x, y, z);
            if (scaffolding.air(pos)) {
                scaffolding.enable(pos);
                numRealBlocks++;
                if (numRealBlocks % 10000 == 0) {
                    report.append(numRealBlocks).append(' ').append(scaffolding.getCollapsedGraph().getComponents().size()).append('\n');
                    scaffolding.recheckEntireCollapsedGraph();
                }
            }
        }
        assertEquals("38832\n" +
                "60000 41471\n" +
                "70000 43558\n" +
                "80000 44176\n" +
                "90000 43575\n" +
                "100000 41808\n" +
                "110000 39108\n" +
                "120000 35351\n" +
                "130000 30807\n" +
                "140000 25772\n" +
                "150000 20387\n" +
                "160000 15149\n" +
                "170000 10482\n" +
                "180000 7067\n" +
                "190000 4767\n" +
                "200000 3155\n" +
                "210000 2011\n" +
                "220000 1281\n" +
                "230000 755\n" +
                "240000 439\n" +
                "250000 270\n", report.toString());
        System.out.println("Done " + (System.currentTimeMillis() - a) + "ms after " + (a - aa) + "ms after " + (aa - aaa) + "ms");
        assertEquals(238, scaffolding.getCollapsedGraph().getComponents().size());
    }
}
