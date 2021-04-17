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
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import java.util.Random;

public class Main {

    public static final boolean DEBUG = false;

    /**
     * If true, many different parts of the builder switch to a more efficient mode where blocks can only be placed adjacent or upwards, never downwards
     */
    public static final boolean STRICT_Y = false;

    public static final boolean fakePlacementForPerformanceTesting = true;

    public static final Random RAND = new Random(5021);

    public static void main() throws InterruptedException {
        for (Face face : Face.VALUES) {
            System.out.println(face);
            System.out.println(face.x);
            System.out.println(face.y);
            System.out.println(face.z);
            System.out.println(face.index);
            System.out.println(face.offset);
            System.out.println(face.oppositeIndex);
        }
        {
            System.out.println("Without");
            long start = BetterBlockPos.toLong(5021, 69, 420);
            System.out.println(BetterBlockPos.fromLong(start));
            start += Face.UP.offset;
            System.out.println(BetterBlockPos.fromLong(start));
            start += Face.DOWN.offset;
            System.out.println(BetterBlockPos.fromLong(start));
            start += Face.UP.offset;
            System.out.println(BetterBlockPos.fromLong(start));
            start += Face.DOWN.offset;
            System.out.println(BetterBlockPos.fromLong(start));
        }
        {
            System.out.println("With");
            long start = BetterBlockPos.toLong(5021, 69, 420);
            System.out.println(BetterBlockPos.fromLong(start));
            start += Face.UP.offset;
            start &= BetterBlockPos.POST_ADDITION_MASK;
            System.out.println(BetterBlockPos.fromLong(start));
            start += Face.DOWN.offset;
            start &= BetterBlockPos.POST_ADDITION_MASK;
            System.out.println(BetterBlockPos.fromLong(start));
            start += Face.UP.offset;
            start &= BetterBlockPos.POST_ADDITION_MASK;
            System.out.println(BetterBlockPos.fromLong(start));
            start += Face.DOWN.offset;
            start &= BetterBlockPos.POST_ADDITION_MASK;
            System.out.println(BetterBlockPos.fromLong(start));
        }
        {
            System.out.println(BetterBlockPos.fromLong(BetterBlockPos.toLong(150, 150, 150)));
        }
        for (int i = 0; i < 10; i++) {
            long aaa = System.currentTimeMillis();
            int[][][] test = new int[64][64][64];
            int based = Block.BLOCK_STATE_IDS.get(Blocks.DIRT.getDefaultState());
            int numRealBlocks = 0;
            for (int x = 0; x < test.length; x++) {
                for (int y = 0; y < test[0].length; y++) {
                    for (int z = 0; z < test[0][0].length; z++) {
                        //if ((x + y + z) % 2 == 0) {
                        if (RAND.nextBoolean()) {
                            test[x][y][z] = based;
                            numRealBlocks++;
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
            int goal = numRealBlocks + 10000;
            while (numRealBlocks < goal) {
                //System.out.println(numRealBlocks);
                int x = RAND.nextInt(scaffolding.bounds().sizeX);
                int y = RAND.nextInt(scaffolding.bounds().sizeY);
                int z = RAND.nextInt(scaffolding.bounds().sizeZ);
                long pos = BetterBlockPos.toLong(x, y, z);
                if (scaffolding.air(pos)) {
                    //System.out.println("Setting to scaffolding " + BetterBlockPos.fromLong(pos) + " " + pos);
                    scaffolding.enable(pos);
                    numRealBlocks++;
                }
            }
            System.out.println("Done " + (System.currentTimeMillis() - a) + "ms after " + (a - aa) + "ms after " + (aa - aaa) + "ms");
            Thread.sleep(500);
            System.gc();
            Thread.sleep(500);
            //scaffolding.enable(0);
        }
        System.exit(0);
    }
}
