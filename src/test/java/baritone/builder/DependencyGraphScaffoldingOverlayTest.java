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
        for (int i = 0; i < 1; i++) {
            long aaa = System.currentTimeMillis();
            //int[][][] test = new int[64][64][64];
            BlockStateCachedData[][][] test = new BlockStateCachedData[64][64][64];
            int numRealBlocks = 0;
            for (int x = 0; x < test.length; x++) {
                for (int y = 0; y < test[0].length; y++) {
                    for (int z = 0; z < test[0][0].length; z++) {
                        //if ((x + y + z) % 2 == 0) {
                        if (RAND.nextInt(10) < 2) {
                            test[x][y][z] = FakeStates.probablyCanBePlaced(RAND);
                            numRealBlocks++;
                        } else {
                            test[x][y][z] = FakeStates.AIR;
                        }
                    }

                }
            }
            /*for (int[][] arr : test) {
                for (int[] arr2 : arr) {
                    Arrays.fill(arr2, based);
                }
            }*/
            PackedBlockStateCuboid states = new PackedBlockStateCuboid(test);
            PlaceOrderDependencyGraph graph = new PlaceOrderDependencyGraph(states);
            long aa = System.currentTimeMillis();
            /*DependencyGraphAnalyzer.prevalidate(graph);
            for (int i = 0; i < 1; i++) {
                long a = System.currentTimeMillis();
                DependencyGraphAnalyzer.prevalidateExternalToInteriorSearch(graph);
                long b = System.currentTimeMillis();
                System.out.println((b - a) + "ms");
                Thread.sleep(500);
                System.gc();
                Thread.sleep(500);
            }*/
            DependencyGraphScaffoldingOverlay scaffolding = new DependencyGraphScaffoldingOverlay(graph);
            long a = System.currentTimeMillis();
            System.out.println(scaffolding.getCollapsedGraph().getComponents().size());
            int goal = numRealBlocks + 200000;
            while (numRealBlocks < goal) {
                //System.out.println(numRealBlocks + " " + scaffolding.getCollapsedGraph().getComponents().size());
                int x = RAND.nextInt(((CuboidBounds) scaffolding.bounds()).sizeX);
                int y = RAND.nextInt(((CuboidBounds) scaffolding.bounds()).sizeY);
                int z = RAND.nextInt(((CuboidBounds) scaffolding.bounds()).sizeZ);
                long pos = BetterBlockPos.toLong(x, y, z);
                if (scaffolding.air(pos)) {
                    //System.out.println("Setting to scaffolding " + BetterBlockPos.fromLong(pos) + " " + pos);
                    scaffolding.enable(pos);
                    numRealBlocks++;
                    if (numRealBlocks % 10000 == 0) {
                        System.out.println(numRealBlocks + " " + scaffolding.getCollapsedGraph().getComponents().size());
                        scaffolding.recheckEntireCollapsedGraph();
                    }
                }
            }
            System.out.println(scaffolding.getCollapsedGraph().getComponents().size());
            System.out.println("Done " + (System.currentTimeMillis() - a) + "ms after " + (a - aa) + "ms after " + (aa - aaa) + "ms");
            assertEquals(238, scaffolding.getCollapsedGraph().getComponents().size());
            /*Thread.sleep(500);
            System.gc();
            Thread.sleep(500);*/
            //scaffolding.enable(0);
        }
    }
}
