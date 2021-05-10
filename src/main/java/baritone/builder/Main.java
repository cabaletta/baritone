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
import baritone.builder.mc.VanillaBlockStateDataProvider;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Main {

    public static final boolean DEBUG = false;

    /**
     * If true, many different parts of the builder switch to a more efficient mode where blocks can only be placed adjacent or upwards, never downwards
     */
    public static final boolean STRICT_Y = false;

    public static final boolean fakePlacementForPerformanceTesting = true;

    public static final IBlockStateDataProvider DATA_PROVIDER = new VanillaBlockStateDataProvider();

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
        for (int i = 0; i < 0; i++) {
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
        for (int i = 0; i < 0; i++) {
            Stream.of(new Object())
                    .flatMap(ignored -> IntStream.range(0, 100).boxed())
                    .parallel()
                    .forEach(x -> System.out.println(x + ""));
            IntStream.range(100, 200).boxed()
                    .parallel()
                    .forEach(x -> System.out.println(x + ""));
            Stream.of(new Object())
                    .flatMap(ignored -> IntStream.range(200, 300).boxed())
                    .collect(Collectors.toList()).parallelStream()
                    .forEach(x -> System.out.println(x + ""));
        }
        {
            BlockStateCachedData.get(0);
        }
        {
            BlockStatePlacementOption.sanityCheck();
        }
        {
            /*Raytracer.raytraceMode++;
            Raytracer.raytraceMode %= 3;*/
            Random rand = new Random(5021);
            DoubleArrayList A = new DoubleArrayList();
            DoubleArrayList B = new DoubleArrayList();
            DoubleArrayList C = new DoubleArrayList();
            DoubleArrayList D = new DoubleArrayList();
            DoubleArrayList E = new DoubleArrayList();
            DoubleArrayList F = new DoubleArrayList();
            LongArrayList G = new LongArrayList();
            long a = System.currentTimeMillis();
            for (int trial = 0; trial < 10_000_000; ) {
                Vec3d playerEye = new Vec3d(rand.nextDouble() * 5 - 2.5, rand.nextDouble() * 5, rand.nextDouble() * 5 - 2.5);
                long eyeBlock = playerEye.getRoundedToZeroPositionUnsafeDontUse();
                if (eyeBlock == 0) {
                    // origin, unlucky
                    continue;
                }
                Face placeToAgainst = Face.VALUES[rand.nextInt(Face.NUM_FACES)];
                Face againstToPlace = placeToAgainst.opposite();
                long placeAgainst = placeToAgainst.offset(0);
                if (eyeBlock == placeAgainst) {
                    continue;
                }
                double[] hitVec = new double[3];
                for (int i = 0; i < 3; i++) {
                    switch (placeToAgainst.vec[i]) {
                        case -1: {
                            hitVec[i] = 0;
                            break;
                        }
                        case 0: {
                            hitVec[i] = rand.nextDouble();
                            break;
                        }
                        case 1: {
                            hitVec[i] = 1;
                            break;
                        }
                    }
                }
                Vec3d hit = new Vec3d(hitVec);
                Raytracer.runTrace(playerEye, placeAgainst, againstToPlace, hit);
                A.add(playerEye.x);
                B.add(playerEye.y);
                C.add(playerEye.z);
                D.add(hit.x);
                E.add(hit.y);
                F.add(hit.z);
                G.add(placeAgainst);
                trial++;
            }
            long b = System.currentTimeMillis();
            System.out.println("Nominal first run with overhead: " + (b - a) + "ms");
            for (int it = 0; it < 20; it++) {
                {
                    Thread.sleep(1000);
                    System.gc();
                    Thread.sleep(1000);
                    long start = System.currentTimeMillis();
                    for (int i = 0; i < 10_000_000; i++) {
                        Raytracer.rayTraceZoomy(A.getDouble(i), B.getDouble(i), C.getDouble(i), D.getDouble(i), E.getDouble(i), F.getDouble(i), G.getLong(i));
                    }
                    long end = System.currentTimeMillis();
                    System.out.println("Branchless took " + (end - start) + "ms");
                }
                {
                    Thread.sleep(1000);
                    System.gc();
                    Thread.sleep(1000);
                    long start = System.currentTimeMillis();
                    for (int i = 0; i < 10_000_000; i++) {
                        Raytracer.rayTraceZoomyBranchy(A.getDouble(i), B.getDouble(i), C.getDouble(i), D.getDouble(i), E.getDouble(i), F.getDouble(i), G.getLong(i));
                    }
                    long end = System.currentTimeMillis();
                    System.out.println("Branchy took " + (end - start) + "ms");
                }
            }
        }
        System.exit(0);
    }
}
