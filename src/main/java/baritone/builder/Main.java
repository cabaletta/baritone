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

import baritone.api.pathing.movement.ActionCosts;
import baritone.api.utils.BetterBlockPos;
import baritone.builder.mc.DebugStates;
import baritone.builder.mc.VanillaBlockStateDataProvider;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import java.util.Random;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Main {

    public static final boolean DEBUG = true;
    public static final boolean SLOW_DEBUG = false;
    public static final boolean VERY_SLOW_DEBUG = false;

    /**
     * If true, many different parts of the builder switch to a more efficient mode where blocks can only be placed adjacent or upwards, never downwards
     */
    public static final boolean STRICT_Y = true;

    public static final BlockData DATA = new BlockData(new VanillaBlockStateDataProvider());

    public static final Random RAND = new Random(5021);

    public static void main() throws InterruptedException {
        System.out.println("Those costs are " + (ActionCosts.FALL_N_BLOCKS_COST[2] / 2) + " and " + ActionCosts.JUMP_ONE_BLOCK_COST + " and " + ActionCosts.FALL_N_BLOCKS_COST[1]);
        for (Face face : Face.VALUES) {
            System.out.println(face);
            System.out.println(face.x);
            System.out.println(face.y);
            System.out.println(face.z);
            System.out.println(face.index);
            System.out.println(face.offset);
            System.out.println(face.oppositeIndex);
            System.out.println("Horizontal " + face.horizontalIndex);
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
        for (int aaaa = 0; aaaa < 0; aaaa++) {
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
            boolean checkAgainst = true;
            for (int i = 0; i < 10_000_000; i++) {
                if (i % 1000 == 0 && checkAgainst) {
                    System.out.println(i);
                }
                LongArrayList normal = Raytracer.rayTraceZoomy(A.getDouble(i), B.getDouble(i), C.getDouble(i), D.getDouble(i), E.getDouble(i), F.getDouble(i), G.getLong(i));
                LongArrayList alternate = Raytracer.rayTraceZoomyAlternate(A.getDouble(i), B.getDouble(i), C.getDouble(i), D.getDouble(i), E.getDouble(i), F.getDouble(i), G.getLong(i));
                if (!normal.equals(alternate)) {
                    throw new IllegalStateException();
                }
                if (checkAgainst) {
                    LongArrayList superSlow = Raytracer.rayTraceFast(A.getDouble(i), B.getDouble(i), C.getDouble(i), D.getDouble(i), E.getDouble(i), F.getDouble(i));
                    if (!normal.equals(superSlow)) {
                        Raytracer.print(normal);
                        Raytracer.print(superSlow);
                        checkAgainst = false;
                    }
                }
            }
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
                    System.out.println("Normal took " + (end - start) + "ms");
                }
                {
                    Thread.sleep(1000);
                    System.gc();
                    Thread.sleep(1000);
                    long start = System.currentTimeMillis();
                    for (int i = 0; i < 10_000_000; i++) {
                        Raytracer.rayTraceZoomyAlternate(A.getDouble(i), B.getDouble(i), C.getDouble(i), D.getDouble(i), E.getDouble(i), F.getDouble(i), G.getLong(i));
                    }
                    long end = System.currentTimeMillis();
                    System.out.println("Alternate took " + (end - start) + "ms");
                }
            }
        }
        {
            DebugStates.debug();
        }
        for (int aaaa = 0; aaaa < 0; aaaa++) {
            Random rand = new Random(5021);
            int trials = 10_000_000;
            int[] X = new int[trials];
            int[] Y = new int[trials];
            int[] Z = new int[trials];
            int sz = 10;
            CuboidBounds bounds = new CuboidBounds(sz, sz, sz);
            for (int i = 0; i < trials; i++) {
                for (int[] toAdd : new int[][]{X, Y, Z}) {
                    toAdd[i] = rand.nextBoolean() ? rand.nextInt(sz) : rand.nextBoolean() ? -1 : sz;
                }
            }
            boolean[] a = new boolean[trials];
            boolean[] b = new boolean[trials];
            boolean[] c = new boolean[trials];
            boolean[] d = new boolean[trials];
            boolean[] e = new boolean[trials];
            for (int it = 0; it < 20; it++) {

                {
                    Thread.sleep(1000);
                    System.gc();
                    Thread.sleep(1000);
                    long start = System.currentTimeMillis();
                    for (int i = 0; i < trials; i++) {
                        a[i] = bounds.inRangeBranchy(X[i], Y[i], Z[i]);
                    }
                    long end = System.currentTimeMillis();
                    System.out.println("Branchy took " + (end - start) + "ms");
                }
                {
                    Thread.sleep(1000);
                    System.gc();
                    Thread.sleep(1000);
                    long start = System.currentTimeMillis();
                    for (int i = 0; i < trials; i++) {
                        b[i] = bounds.inRangeBranchless(X[i], Y[i], Z[i]);
                    }
                    long end = System.currentTimeMillis();
                    System.out.println("Branchless took " + (end - start) + "ms");
                }
                {
                    Thread.sleep(1000);
                    System.gc();
                    Thread.sleep(1000);
                    long start = System.currentTimeMillis();
                    for (int i = 0; i < trials; i++) {
                        c[i] = bounds.inRangeBranchless2(X[i], Y[i], Z[i]);
                    }
                    long end = System.currentTimeMillis();
                    System.out.println("Branchless2 took " + (end - start) + "ms");
                }
                {
                    Thread.sleep(1000);
                    System.gc();
                    Thread.sleep(1000);
                    long start = System.currentTimeMillis();
                    for (int i = 0; i < trials; i++) {
                        d[i] = bounds.inRangeBranchless3(X[i], Y[i], Z[i]);
                    }
                    long end = System.currentTimeMillis();
                    System.out.println("Branchless3 took " + (end - start) + "ms");
                }
                {
                    Thread.sleep(1000);
                    System.gc();
                    Thread.sleep(1000);
                    long start = System.currentTimeMillis();
                    for (int i = 0; i < trials; i++) {
                        e[i] = bounds.inRangeBranchless4(X[i], Y[i], Z[i]);
                    }
                    long end = System.currentTimeMillis();
                    System.out.println("Branchless4 took " + (end - start) + "ms");
                }
                /*
Branchless2 took 55ms
Branchless3 took 53ms
Branchless4 took 47ms
Branchy took 137ms
Branchless took 35ms
Branchless2 took 36ms
Branchless3 took 35ms
Branchless4 took 41ms
Branchy took 118ms
Branchless took 33ms
Branchless2 took 39ms
Branchless3 took 36ms
Branchless4 took 42ms
Branchy took 125ms
Branchless took 41ms
Branchless2 took 45ms
Branchless3 took 41ms
Branchless4 took 45ms
Branchy took 123ms
Branchless took 38ms
Branchless2 took 43ms
Branchless3 took 35ms
Branchless4 took 43ms
Branchy took 117ms
Branchless took 37ms
Branchless2 took 42ms
Branchless3 took 41ms
Branchless4 took 45ms
Branchy took 123ms
Branchless took 35ms
Branchless2 took 42ms
Branchless3 took 38ms
Branchless4 took 46ms
Branchy took 126ms
Branchless took 34ms
Branchless2 took 47ms
Branchless3 took 40ms
Branchless4 took 47ms
Branchy took 124ms
                 */

                // 3 is better than 2 and 4 because of data dependency
                // the L1 cache fetch for this.sizeX can happen at the same time as "x+1" (which is an increment of an argument)
                // in other words: in options 2 and 4, the "+1" or "-1" has a data dependency on the RAM fetch for this.sizeX, but in option 3 alone, the +1 happens upon the argument x, which is likely in a register, meaning it can be pipelined in parallel with the L1 cache fetch for this.sizeX

            }
        }
        /*{ // proguard test
            PlayerPhysics.determinePlayerRealSupport(BlockStateCachedData.get(69), BlockStateCachedData.get(420));
            PlayerPhysics.determinePlayerRealSupport(BlockStateCachedData.get(420), BlockStateCachedData.get(69));
        }*/
        {
            for (int sneak = 0; sneak < 4; sneak++) {
                System.out.println("meow");
                System.out.println(sneak);
                System.out.println(SneakPosition.encode(0, sneak));
                System.out.println(SneakPosition.encode(BetterBlockPos.POST_ADDITION_MASK, sneak));
                System.out.println(SneakPosition.decode(SneakPosition.encode(0, sneak)));
                System.out.println(SneakPosition.decode(SneakPosition.encode(BetterBlockPos.POST_ADDITION_MASK, sneak)));
            }
        }
        {
            int[][][] test = new int[8][8][8];
            int dirt = Block.BLOCK_STATE_IDS.get(Blocks.DIRT.getDefaultState());
            System.out.println("D " + dirt);
            for (int x = 0; x < test.length; x++) {
                for (int z = 0; z < test[0][0].length; z++) {
                    test[x][0][z] = dirt;
                    test[x][1][z] = dirt;
                }
            }
            test[5][5][5] = dirt;
            test[5][5][6] = dirt;
            test[0][5][5] = dirt;
            Consumer<DependencyGraphScaffoldingOverlay> debug = dgso -> {
                for (int y = 0; y < test[0].length; y++) {
                    System.out.println("Layer " + y);
                    for (int x = 0; x < test.length; x++) {
                        for (int z = 0; z < test[0][0].length; z++) {
                            long pos = BetterBlockPos.toLong(x, y, z);
                            if (dgso.real(pos)) {
                                System.out.print(dgso.getCollapsedGraph().getComponentLocations().get(pos).deletedIntoRecursive());
                            } else {
                                System.out.print(" ");
                            }
                        }
                        System.out.println();
                    }
                }
            };
            PackedBlockStateCuboid states = new PackedBlockStateCuboid(test, DATA);
            PlaceOrderDependencyGraph graph = new PlaceOrderDependencyGraph(states);
            System.out.println("N " + Face.NORTH.z);
            System.out.println("S " + Face.SOUTH.z);
            for (int z = 0; z < test[0][0].length; z++) {
                //System.out.println(states.get(states.bounds.toIndex(0, 0, z)));
                System.out.println(z + " " + graph.outgoingEdge(BetterBlockPos.toLong(0, 0, z), Face.NORTH) + " " + graph.outgoingEdge(BetterBlockPos.toLong(0, 0, z), Face.SOUTH));
            }
            DependencyGraphAnalyzer.prevalidate(graph);
            DependencyGraphAnalyzer.prevalidateExternalToInteriorSearch(graph);
            DependencyGraphScaffoldingOverlay scaffolding = new DependencyGraphScaffoldingOverlay(graph);
            System.out.println("Hewwo");
            scaffolding.getCollapsedGraph().getComponents().forEach((key, value) -> {
                System.out.println(key);
                System.out.println(value.getPositions().stream().map(BetterBlockPos::fromLong).collect(Collectors.toList()));
            });
            System.out.println();
            debug.accept(scaffolding);
            Scaffolder.Output out = Scaffolder.run(graph, DijkstraScaffolder.INSTANCE);
            debug.accept(out.secretInternalForTesting());
        }
        System.exit(0);
    }
}
