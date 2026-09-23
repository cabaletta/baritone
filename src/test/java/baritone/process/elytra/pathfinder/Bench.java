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

package baritone.process.elytra.pathfinder;

import java.util.Arrays;

/**
 * The numbers the port is held to, over the same kind of input as the native benchmarks: rays
 * through real terrain, path searches over it, and the 100k-block generating search of main.cpp.
 * Run it by hand: java -cp ... baritone.process.elytra.pathfinder.Bench
 */
public final class Bench {

    private static long splitmix(long[] s) {
        long z = (s[0] += 0x9E3779B97F4A7C15L);
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    private static double between(long[] s, double lo, double hi) {
        return lo + (splitmix(s) >>> 11) * 0x1.0p-53 * (hi - lo);
    }

    public static void main(String[] args) {
        final int side = args.length > 0 ? Integer.parseInt(args[0]) : 64;
        final int rays = args.length > 1 ? Integer.parseInt(args[1]) : 200000;

        long t0 = System.nanoTime();
        final NetherPathfinder ctx = Oracle.generatedWorld(side);
        System.out.printf("%d chunks generated in %d ms%n", side * side, (System.nanoTime() - t0) / 1_000_000);

        final long[] rng = {42};
        final double lim = side * 16.0;
        final double[] from = new double[rays * 3], to = new double[rays * 3];
        for (int i = 0; i < rays;) {
            final double fx = between(rng, 8.0, lim - 8.0), fy = between(rng, 32.0, 120.0), fz = between(rng, 8.0, lim - 8.0);
            if (ctx.getChunkOrDefault((int) fx >> 4, (int) fz >> 4, true).isSolid((int) fx & 15, (int) fy, (int) fz & 15)) continue;
            final double a = between(rng, 0, 6.283185307179586), l = between(rng, 30.0, 150.0);
            from[i * 3] = fx; from[i * 3 + 1] = fy; from[i * 3 + 2] = fz;
            to[i * 3] = Math.max(1.0, Math.min(lim - 1.0, fx + Math.cos(a) * l));
            to[i * 3 + 1] = Math.max(1.0, Math.min(126.0, fy + between(rng, -30.0, 30.0)));
            to[i * 3 + 2] = Math.max(1.0, Math.min(lim - 1.0, fz + Math.sin(a) * l));
            i++;
        }
        final boolean[] hits = new boolean[rays];
        for (int rep = 0; rep < 5; rep++) {
            t0 = System.nanoTime();
            for (int i = 0; i < rays; i++) {
                hits[i] = Raytracer.raytrace(ctx, from[i * 3], from[i * 3 + 1], from[i * 3 + 2], to[i * 3], to[i * 3 + 1], to[i * 3 + 2], NetherPathfinder.CacheMiss.SOLID) != null;
            }
            final double ms = (System.nanoTime() - t0) / 1e6;
            int hit = 0;
            for (boolean h : hits) if (h) hit++;
            System.out.printf("java: %d rays (30-150 blocks, air origins), SOLID mode: %.0f ms, %.3f us/ray, %.2f%% hit%n", rays, ms, ms * 1000 / rays, 100.0 * hit / rays);
        }

        // The same searches twice: treating a chunk the table lacks as air, and generating it. Every
        // chunk is in the table by the timed pass, so the second never generates and must cost no
        // more than the first; it is the search a flight that predicts terrain runs over the chunks
        // it has loaded. The untimed pass generates what the searches reach past the world's edge.
        final long searchSeed = rng[0];
        for (int pass = 0; pass < 2; pass++) {
            for (boolean airIfFake : new boolean[]{true, false}) {
                rng[0] = searchSeed;
                for (int dist : new int[]{300, 800}) {
                    long total = 0, blocks = 0;
                    int finished = 0;
                    final int runs = 20;
                    final long[] us = new long[runs]; // one search that runs into the 500 ms timeout dominates the average
                    for (int r = 0; r < runs; r++) {
                        final int sx = (int) between(rng, 16.0, lim - 16.0 - dist), sz = (int) between(rng, 16.0, lim - 16.0);
                        t0 = System.nanoTime();
                        final PathSegment p = ctx.pathFind(sx, 60, sz, sx + dist, 60, sz, true, false, 10000, airIfFake, 8.0);
                        us[r] = (System.nanoTime() - t0) / 1000;
                        total += us[r];
                        if (p != null) { blocks += p.blocks.size(); if (p.finished) finished++; }
                    }
                    if (pass == 0) continue;
                    Arrays.sort(us);
                    System.out.printf("java pathFind %s over %d blocks: avg %d us, median %d us, avg %d path nodes, %d/%d reached the goal%n",
                            airIfFake ? "airIfFake" : "generating, every chunk present,", dist, total / runs, us[runs / 2], blocks / runs, finished, runs);
                }
            }
        }

        final NetherPathfinder fresh = new NetherPathfinder(Oracle.SEED, null, NetherPathfinder.Dimension.NETHER, 128);
        t0 = System.nanoTime();
        final NodePos start = PathFinder.findAir(fresh, Size.X4, 0, 50, 0, false);
        final NodePos goal = PathFinder.findAir(fresh, Size.X4, 100000, 50, 0, false);
        final PathFinder.Path path = PathFinder.findPathFull(fresh, start, goal, 1);
        final double s = (System.nanoTime() - t0) / 1e9;
        System.out.printf("java: 100k-block generating search (main.cpp): %.2f s, path of %d blocks to %s, %d chunks kept%n", s, path.blocks.size(), path.getEndPos(), fresh.chunkCount());
    }
}
