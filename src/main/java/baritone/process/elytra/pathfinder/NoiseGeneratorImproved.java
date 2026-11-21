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

/** Minecraft's improved Perlin noise, as the native library ported it. */
final class NoiseGeneratorImproved {

    private static final double[] GRAD_X = {1.0, -1.0, 1.0, -1.0, 1.0, -1.0, 1.0, -1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, -1.0, 0.0};
    private static final double[] GRAD_Y = {1.0, 1.0, -1.0, -1.0, 0.0, 0.0, 0.0, 0.0, 1.0, -1.0, 1.0, -1.0, 1.0, -1.0, 1.0, -1.0};
    private static final double[] GRAD_Z = {0.0, 0.0, 0.0, 0.0, 1.0, 1.0, -1.0, -1.0, 1.0, 1.0, -1.0, -1.0, 0.0, 1.0, 0.0, -1.0};

    private final short[] permutations = new short[512];
    private final double xCoord;
    private final double yCoord;
    private final double zCoord;

    NoiseGeneratorImproved(Random random) {
        this.xCoord = random.nextDouble() * 256.0;
        this.yCoord = random.nextDouble() * 256.0;
        this.zCoord = random.nextDouble() * 256.0;
        for (int i = 0; i < 256; i++) {
            this.permutations[i] = (short) i;
        }
        for (int l = 0; l < 256; ++l) {
            final int j = random.nextInt(256 - l) + l;
            final short k = this.permutations[l];
            this.permutations[l] = this.permutations[j];
            this.permutations[j] = k;
            this.permutations[l + 256] = this.permutations[l];
        }
    }

    private static double lerp(double a, double b, double c) {
        return b + a * (c - b);
    }

    private static double grad(int hash, double x, double y, double z) {
        final int i = hash & 15;
        return GRAD_X[i] * x + GRAD_Y[i] * y + GRAD_Z[i] * z;
    }

    void populateNoiseArray(double[] noiseArray, double xOffset, double yOffset, double zOffset, int xSize, int ySize, int zSize, double xScale, double yScale, double zScale, double noiseScale) {
        if (ySize == 1) {
            throw new IllegalArgumentException("ySize == 1 uses a code path the native library did not keep");
        }
        final short[] p = this.permutations;
        int i = 0;
        final double d0 = 1.0 / noiseScale;
        int k = -1;
        double d1 = 0.0;
        double d2 = 0.0;
        double d3 = 0.0;
        double d4 = 0.0;

        for (int l2 = 0; l2 < xSize; ++l2) {
            double d5 = xOffset + (double) l2 * xScale + this.xCoord;
            int i3 = (int) d5;
            if (d5 < (double) i3) {
                --i3;
            }
            final int j3 = i3 & 255;
            d5 = d5 - (double) i3;
            final double d6 = d5 * d5 * d5 * (d5 * (d5 * 6.0 - 15.0) + 10.0);

            for (int k3 = 0; k3 < zSize; ++k3) {
                double d7 = zOffset + (double) k3 * zScale + this.zCoord;
                int l3 = (int) d7;
                if (d7 < (double) l3) {
                    --l3;
                }
                final int i4 = l3 & 255;
                d7 = d7 - (double) l3;
                final double d8 = d7 * d7 * d7 * (d7 * (d7 * 6.0 - 15.0) + 10.0);

                for (int j4 = 0; j4 < ySize; ++j4) {
                    double d9 = yOffset + (double) j4 * yScale + this.yCoord;
                    int k4 = (int) d9;
                    if (d9 < (double) k4) {
                        --k4;
                    }
                    final int l4 = k4 & 255;
                    d9 = d9 - (double) k4;
                    final double d10 = d9 * d9 * d9 * (d9 * (d9 * 6.0 - 15.0) + 10.0);

                    if (j4 == 0 || l4 != k) {
                        k = l4;
                        final int l = p[j3] + l4;
                        final int i1 = p[l] + i4;
                        final int j1 = p[l + 1] + i4;
                        final int k1 = p[j3 + 1] + l4;
                        final int l1 = p[k1] + i4;
                        final int i2 = p[k1 + 1] + i4;
                        d1 = lerp(d6, grad(p[i1], d5, d9, d7), grad(p[l1], d5 - 1.0, d9, d7));
                        d2 = lerp(d6, grad(p[j1], d5, d9 - 1.0, d7), grad(p[i2], d5 - 1.0, d9 - 1.0, d7));
                        d3 = lerp(d6, grad(p[i1 + 1], d5, d9, d7 - 1.0), grad(p[l1 + 1], d5 - 1.0, d9, d7 - 1.0));
                        d4 = lerp(d6, grad(p[j1 + 1], d5, d9 - 1.0, d7 - 1.0), grad(p[i2 + 1], d5 - 1.0, d9 - 1.0, d7 - 1.0));
                    }

                    final double d11 = lerp(d10, d1, d2);
                    final double d12 = lerp(d10, d3, d4);
                    final double d13 = lerp(d8, d11, d12);
                    noiseArray[i++] += d13 * d0;
                }
            }
        }
    }
}
