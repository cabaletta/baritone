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

package baritone.utils.locate;

import net.minecraft.core.BlockPos;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CancellationException;

import static org.junit.Assert.*;

public class BiomeSearchTest {
    private static final BiomeSearch.Bounds BOUNDS = new BiomeSearch.Bounds(-64, 319, -1000, 1000, -1000, 1000);

    @Test
    public void findsUndergroundBiomeAtNegativeCoordinates() {
        BlockPos target = new BlockPos(-96, -32, -64);
        assertEquals(target, BiomeSearch.find((x, y, z) -> target.equals(new BlockPos(x, y, z)),
                new BlockPos(-32, 64, -32), 128, BOUNDS));
    }

    @Test
    public void visitsNearestRingFirstAndIncludesRadiusBoundary() {
        assertEquals(new BlockPos(32, 64, 0), BiomeSearch.find((x, y, z) -> y == 64 && z == 0 && (x == 32 || x == -64),
                new BlockPos(0, 64, 0), 64, BOUNDS));
        assertEquals(new BlockPos(64, 64, 64), BiomeSearch.find((x, y, z) -> x == 64 && y == 64 && z == 64,
                new BlockPos(0, 64, 0), 64, BOUNDS));
    }

    @Test
    public void exhaustsGridOnceAndHonorsBorder() {
        Set<BlockPos> visited = new HashSet<>();
        assertNull(BiomeSearch.find((x, y, z) -> {
            assertTrue(visited.add(new BlockPos(x, y, z)));
            assertTrue(x >= 0 && x < 65 && z >= 0 && z < 65);
            return false;
        }, new BlockPos(0, 64, 0), 64, new BiomeSearch.Bounds(64, 64, 0, 65, 0, 65)));
        assertEquals(9, visited.size());
    }

    @Test
    public void cancellationDoesNotSample() {
        Thread.currentThread().interrupt();
        try {
            assertThrows(CancellationException.class, () -> BiomeSearch.find((x, y, z) -> {
                fail("Cancelled search must not sample generation");
                return false;
            }, BlockPos.ZERO, 64, BOUNDS));
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    public void refinesBoundaryBetweenCoarseSamples() {
        assertEquals(new BlockPos(28, 64, 0), BiomeSearch.find((x, y, z) -> x >= 28 && y == 64 && z == 0,
                new BlockPos(0, 64, 0), 64, BOUNDS));
    }

    @Test
    public void prefersCloserPointWithinSameSquareRing() {
        assertEquals(new BlockPos(0, 64, 32), BiomeSearch.find((x, y, z) -> y == 64
                        && ((x == -32 && z == -32) || (x == 0 && z == 32)),
                new BlockPos(0, 64, 0), 64, BOUNDS));
    }

    @Test
    public void refinementStaysInsideRadiusAndBorder() {
        BiomeSearch.Bounds bounds = new BiomeSearch.Bounds(60, 70, -40, 41, -40, 41);
        BlockPos result = BiomeSearch.find((x, y, z) -> {
            assertTrue(Math.abs(x) <= 32 && Math.abs(z) <= 32);
            assertTrue(bounds.contains(x, z));
            assertTrue(y >= 60 && y <= 70);
            return x >= 28;
        }, new BlockPos(0, 64, 0), 32, bounds);
        assertEquals(new BlockPos(28, 64, 0), result);
    }

    @Test
    public void rejectsUnboundedSearches() {
        assertThrows(IllegalArgumentException.class,
                () -> BiomeSearch.find((x, y, z) -> false, BlockPos.ZERO, Integer.MAX_VALUE, BOUNDS));
    }
}
