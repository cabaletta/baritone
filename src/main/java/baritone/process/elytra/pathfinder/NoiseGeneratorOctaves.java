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

final class NoiseGeneratorOctaves {

    private final NoiseGeneratorImproved[] generators;

    NoiseGeneratorOctaves(Random random, int octaves) {
        this.generators = new NoiseGeneratorImproved[octaves];
        for (int i = 0; i < octaves; i++) {
            this.generators[i] = new NoiseGeneratorImproved(random);
        }
    }

    private static long lfloor(double value) {
        final long i = (long) value;
        return value < (double) i ? i - 1L : i;
    }

    double[] generateNoiseOctaves(int xOffset, int yOffset, int zOffset, int xSize, int ySize, int zSize, double xScale, double yScale, double zScale) {
        final double[] noiseArray = new double[xSize * ySize * zSize];
        double d3 = 1.0;
        for (int j = 0; j < this.generators.length; ++j) {
            double d0 = (double) xOffset * d3 * xScale;
            final double d1 = (double) yOffset * d3 * yScale;
            double d2 = (double) zOffset * d3 * zScale;
            long k = lfloor(d0);
            long l = lfloor(d2);
            d0 = d0 - (double) k;
            d2 = d2 - (double) l;
            k = k % 16777216L;
            l = l % 16777216L;
            d0 = d0 + (double) k;
            d2 = d2 + (double) l;
            this.generators[j].populateNoiseArray(noiseArray, d0, d1, d2, xSize, ySize, zSize, xScale * d3, yScale * d3, zScale * d3, d3);
            d3 /= 2.0;
        }
        return noiseArray;
    }
}
