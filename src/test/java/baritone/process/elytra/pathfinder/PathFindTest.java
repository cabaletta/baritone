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

import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PathFindTest {

    private static final int NETHER = NetherPathfinder.DIMENSION_NETHER;

    static int unpackX(long packed) {
        return (int) (packed >> 38);
    }

    static int unpackY(long packed) {
        return (int) ((packed << 26) >> 52);
    }

    static int unpackZ(long packed) {
        return (int) ((packed << 38) >> 38);
    }

    @Test
    public void packedPositionsRoundTrip() {
        for (int[] p : new int[][]{{0, 0, 0}, {1, 2, 3}, {-1, 5, -30000000}, {29999999, 383, 12345}, {-29999999, 0, 29999999}}) {
            final long packed = NetherPathfinder.packBlockPos(new BlockPos(p[0], p[1], p[2]));
            assertEquals(p[0], unpackX(packed));
            assertEquals(p[1], unpackY(packed));
            assertEquals(p[2], unpackZ(packed));
        }
    }

    @Test
    public void straightThroughAirWhenNothingIsKnown() {
        final NetherPathfinder ctx = new NetherPathfinder(1, null, NETHER, 128);
        final PathSegment segment = ctx.pathFind(0, 60, 0, 500, 60, 0, true, false, 10000, true, 8.0);
        assertNotNull(segment);
        assertTrue(segment.finished);
        assertTrue(segment.packed.length >= 2);
        int lastX = Integer.MIN_VALUE;
        for (long packed : segment.packed) {
            final int x = unpackX(packed);
            assertTrue("x goes forward", x > lastX);
            lastX = x;
            assertEquals(0, unpackZ(packed), 16);
            assertEquals(60, unpackY(packed), 16);
        }
        assertEquals(500, lastX, 16);
    }

    @Test
    public void refiningAStraightPathLeavesItsEnds() {
        // refining raytraces in generate mode, so the chunks have to be known and empty for the rays to pass
        final NetherPathfinder ctx = new NetherPathfinder(1, null, NETHER, 128);
        for (int x = -1; x <= 32; x++) for (int z = -1; z <= 1; z++) ctx.allocateAndInsertChunk(x, z);
        final PathSegment segment = ctx.pathFind(0, 60, 0, 500, 60, 0, true, true, 10000, true, 8.0);
        assertNotNull(segment);
        assertTrue(segment.finished);
        assertTrue("refined to " + segment.packed.length + " points", segment.packed.length <= 3);
    }

    @Test
    public void givesUpAfterAHundredUnknownChunks() {
        final NetherPathfinder ctx = new NetherPathfinder(1, null, NETHER, 128);
        final PathSegment segment = ctx.pathFind(0, 60, 0, 20000, 60, 0, true, false, 10000, true, 8.0);
        assertNotNull(segment);
        assertFalse(segment.finished);
        assertTrue(unpackX(segment.packed[segment.packed.length - 1]) > 100);
    }

    @Test
    public void reachesTheGoalOverRealTerrain() {
        final NetherPathfinder ctx = Oracle.generatedWorld(24);
        final PathSegment segment = ctx.pathFind(20, 60, 20, 340, 60, 200, true, false, 10000, true, 8.0);
        assertNotNull(segment);
        assertTrue(segment.finished);
        // every point of the path is in air
        for (long packed : segment.packed) {
            final int x = unpackX(packed), y = unpackY(packed), z = unpackZ(packed);
            assertFalse("path goes through a block at " + x + "," + y + "," + z, ctx.getChunkOrDefault(x >> 4, z >> 4, true).isSolid(x & 15, y, z & 15));
        }
    }

    @Test
    public void generatesTerrainWhenAskedTo() {
        final NetherPathfinder ctx = new NetherPathfinder(Oracle.SEED, null, NETHER, 128);
        final PathSegment segment = ctx.pathFind(20, 60, 20, 300, 60, 20, true, false, 10000, false, 1.0);
        assertNotNull(segment);
        assertTrue(segment.finished);
        assertTrue("generated chunks along the way", ctx.chunkCount() > 20);
        assertFalse(ctx.hasChunkFromJava(1, 1));
    }

    @Test
    public void cancelStopsARunningSearch() throws Exception {
        final NetherPathfinder ctx = new NetherPathfinder(Oracle.SEED, null, NETHER, 128);
        final AtomicReference<PathSegment> result = new AtomicReference<>();
        final long[] elapsed = new long[1];
        final Thread searcher = new Thread(() -> {
            final long t0 = System.nanoTime();
            result.set(ctx.pathFind(0, 50, 0, 100000, 50, 0, true, false, 0, false, 1.0));
            elapsed[0] = (System.nanoTime() - t0) / 1_000_000;
        });
        searcher.start();
        Thread.sleep(150);
        assertFalse("not cancelled before", ctx.cancel());
        searcher.join(10000);
        assertFalse(searcher.isAlive());
        assertNull(result.get());
        assertTrue("returned " + elapsed[0] + " ms after the start", elapsed[0] < 1000);
        assertTrue(ctx.cancel());
    }

    @Test
    public void aSearchThatCannotFinishReturnsWhatItHasAfterHalfASecond() {
        final NetherPathfinder ctx = new NetherPathfinder(Oracle.SEED, null, NETHER, 128);
        final long t0 = System.nanoTime();
        final PathSegment segment = ctx.pathFind(0, 50, 0, 100000, 50, 0, true, false, 0, false, 1.0);
        final long ms = (System.nanoTime() - t0) / 1_000_000;
        assertNotNull(segment);
        assertFalse(segment.finished);
        assertTrue("took " + ms + " ms", ms >= 500 && ms < 5000);
        assertTrue(unpackX(segment.packed[segment.packed.length - 1]) > 50);
    }

    @Test
    public void rejectsBadArguments() {
        final NetherPathfinder ctx = new NetherPathfinder(1, null, NETHER, 128);
        try {
            ctx.pathFind(0, -1, 0, 10, 60, 0, true, false, 0, true, 1.0);
            throw new AssertionError("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // good
        }
        try {
            new NetherPathfinder(1, null, NETHER, 500);
            throw new AssertionError("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // good
        }
        try {
            new NetherPathfinder(1, null, 7, 128);
            throw new AssertionError("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // good
        }
    }
}
