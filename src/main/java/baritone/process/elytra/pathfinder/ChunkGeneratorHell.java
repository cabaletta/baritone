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

import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

/** The nether's terrain shape for a seed, as Minecraft generated it and the native library ported it. */
final class ChunkGeneratorHell {

    private final NoiseGeneratorOctaves lperlinNoise1;
    private final NoiseGeneratorOctaves lperlinNoise2;
    private final NoiseGeneratorOctaves perlinNoise1;

    private ChunkGeneratorHell(NoiseGeneratorOctaves lperlinNoise1, NoiseGeneratorOctaves lperlinNoise2, NoiseGeneratorOctaves perlinNoise1) {
        this.lperlinNoise1 = lperlinNoise1;
        this.lperlinNoise2 = lperlinNoise2;
        this.perlinNoise1 = perlinNoise1;
    }

    static ChunkGeneratorHell fromSeed(long seed) {
        final Random rand = new Random(seed);
        // constructed in this order: they share the one random
        final NoiseGeneratorOctaves a = new NoiseGeneratorOctaves(rand, 16);
        final NoiseGeneratorOctaves b = new NoiseGeneratorOctaves(rand, 16);
        final NoiseGeneratorOctaves c = new NoiseGeneratorOctaves(rand, 8);
        return new ChunkGeneratorHell(a, b, c);
    }

    /** Fills {@code chunk} with the terrain of chunk (x, z). Only sets blocks, never clears them. */
    void generateChunk(int x, int z, Chunk chunk) {
        prepareHeights(x, z, chunk);
    }

    Chunk generateChunk(int x, int z) {
        final Chunk chunk = new Chunk();
        prepareHeights(x, z, chunk);
        return chunk;
    }

    private static final int X_SIZE = 5;
    private static final int Y_SIZE = 17;
    private static final int Z_SIZE = 5;

    private double[] getHeights(int xOffset, int yOffset, int zOffset) {
        final double[] buffer = new double[X_SIZE * Y_SIZE * Z_SIZE];
        // The three noise fields take most of a chunk's time and do not depend on each other, so
        // two go to the common pool while this thread does the third, as the native library did
        // with its own worker threads. A pool thread that is itself generating a chunk helps out
        // while it waits, so nesting is fine.
        final ForkJoinTask<double[]> arTask = ForkJoinPool.commonPool().submit((Callable<double[]>) () ->
                this.lperlinNoise1.generateNoiseOctaves(xOffset, yOffset, zOffset, X_SIZE, Y_SIZE, Z_SIZE, 684.412, 2053.236, 684.412));
        final ForkJoinTask<double[]> brTask = ForkJoinPool.commonPool().submit((Callable<double[]>) () ->
                this.lperlinNoise2.generateNoiseOctaves(xOffset, yOffset, zOffset, X_SIZE, Y_SIZE, Z_SIZE, 684.412, 2053.236, 684.412));
        final double[] pnr = this.perlinNoise1.generateNoiseOctaves(xOffset, yOffset, zOffset, X_SIZE, Y_SIZE, Z_SIZE, 8.555150000000001, 34.2206, 8.555150000000001);
        final double[] ar = arTask.join();
        final double[] br = brTask.join();

        int i = 0;
        final double[] adouble = new double[Y_SIZE];
        for (int j = 0; j < Y_SIZE; ++j) {
            adouble[j] = Math.cos((double) j * Math.PI * 6.0 / (double) Y_SIZE) * 2.0;
            double d2 = (double) j;
            if (j > Y_SIZE / 2) {
                d2 = (double) (Y_SIZE - 1 - j);
            }
            if (d2 < 4.0) {
                d2 = 4.0 - d2;
                adouble[j] -= d2 * d2 * d2 * 10.0;
            }
        }

        for (int l = 0; l < X_SIZE; ++l) {
            for (int i1 = 0; i1 < Z_SIZE; ++i1) {
                for (int k = 0; k < Y_SIZE; ++k) {
                    final double d4 = adouble[k];
                    final double d5 = ar[i] / 512.0;
                    final double d6 = br[i] / 512.0;
                    final double d7 = (pnr[i] / 10.0 + 1.0) / 2.0;
                    double d8;
                    if (d7 < 0.0) {
                        d8 = d5;
                    } else if (d7 > 1.0) {
                        d8 = d6;
                    } else {
                        d8 = d5 + (d6 - d5) * d7;
                    }
                    d8 = d8 - d4;
                    if (k > Y_SIZE - 4) {
                        final double d9 = (double) ((float) (k - (Y_SIZE - 4)) / 3.0F);
                        d8 = d8 * (1.0 - d9) + -10.0 * d9;
                    }
                    buffer[i] = d8;
                    ++i;
                }
            }
        }
        return buffer;
    }

    private void prepareHeights(int x, int z, Chunk primer) {
        final double[] buffer = this.getHeights(x * 4, 0, z * 4);
        final int j = 64 / 2 + 1; // 64 = sea level

        for (int j1 = 0; j1 < 4; ++j1) {
            for (int k1 = 0; k1 < 4; ++k1) {
                for (int l1 = 0; l1 < 16; ++l1) {
                    double d1 = buffer[((j1 + 0) * 5 + k1 + 0) * 17 + l1 + 0];
                    double d2 = buffer[((j1 + 0) * 5 + k1 + 1) * 17 + l1 + 0];
                    double d3 = buffer[((j1 + 1) * 5 + k1 + 0) * 17 + l1 + 0];
                    double d4 = buffer[((j1 + 1) * 5 + k1 + 1) * 17 + l1 + 0];
                    final double d5 = (buffer[((j1 + 0) * 5 + k1 + 0) * 17 + l1 + 1] - d1) * 0.125;
                    final double d6 = (buffer[((j1 + 0) * 5 + k1 + 1) * 17 + l1 + 1] - d2) * 0.125;
                    final double d7 = (buffer[((j1 + 1) * 5 + k1 + 0) * 17 + l1 + 1] - d3) * 0.125;
                    final double d8 = (buffer[((j1 + 1) * 5 + k1 + 1) * 17 + l1 + 1] - d4) * 0.125;

                    for (int i2 = 0; i2 < 8; ++i2) {
                        double d10 = d1;
                        double d11 = d2;
                        final double d12 = (d3 - d1) * 0.25;
                        final double d13 = (d4 - d2) * 0.25;

                        for (int j2 = 0; j2 < 4; ++j2) {
                            double d15 = d10;
                            final double d16 = (d11 - d10) * 0.25;

                            for (int k2 = 0; k2 < 4; ++k2) {
                                boolean solid = false;
                                if (l1 * 8 + i2 < j) {
                                    solid = true; // lava
                                }
                                if (d15 > 0.0) {
                                    solid = true; // netherrack
                                }
                                if (solid) {
                                    primer.setBlock(j2 + j1 * 4, i2 + l1 * 8, k2 + k1 * 4, true);
                                }
                                d15 += d16;
                            }
                            d10 += d12;
                            d11 += d13;
                        }
                        d1 += d5;
                        d2 += d6;
                        d3 += d7;
                        d4 += d8;
                    }
                }
            }
        }
    }
}
