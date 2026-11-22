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

import net.minecraft.core.BlockPos;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PathFindTest {

    private static final NetherPathfinder.Dimension NETHER = NetherPathfinder.Dimension.NETHER;

    @Test
    public void straightThroughAirWhenNothingIsKnown() {
        final NetherPathfinder ctx = new NetherPathfinder(1, null, NETHER, 128);
        final PathSegment segment = ctx.pathFind(0, 60, 0, 500, 60, 0, true, false, 10000, true, 8.0);
        assertNotNull(segment);
        assertTrue(segment.finished);
        assertTrue(segment.blocks.size() >= 2);
        int lastX = Integer.MIN_VALUE;
        for (BlockPos packed : segment.blocks) {
            final int x = packed.getX();
            assertTrue("x goes forward", x > lastX);
            lastX = x;
            assertEquals(0, packed.getZ(), 16);
            assertEquals(60, packed.getY(), 16);
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
        assertTrue("refined to " + segment.blocks.size() + " points", segment.blocks.size() <= 3);
    }

    @Test
    public void givesUpAfterAHundredUnknownChunks() {
        final NetherPathfinder ctx = new NetherPathfinder(1, null, NETHER, 128);
        final PathSegment segment = ctx.pathFind(0, 60, 0, 20000, 60, 0, true, false, 10000, true, 8.0);
        assertNotNull(segment);
        assertFalse(segment.finished);
        assertTrue(segment.blocks.get(segment.blocks.size() - 1).getX() > 100);
    }

    @Test
    public void reachesTheGoalOverRealTerrain() {
        final NetherPathfinder ctx = Oracle.generatedWorld(24);
        final PathSegment segment = ctx.pathFind(20, 60, 20, 340, 60, 200, true, false, 10000, true, 8.0);
        assertNotNull(segment);
        assertTrue(segment.finished);
        // every point of the path is in air
        for (BlockPos packed : segment.blocks) {
            final int x = packed.getX(), y = packed.getY(), z = packed.getZ();
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
        assertFalse(ctx.hasChunkFromCaller(1, 1));
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
        // The search that was stopped clears the flag as it returns, so this cancel is the first
        // one again. It used to be cleared at the start of the next search instead, which dropped a
        // cancel that arrived while that search was still queued -- see CancelTest.
        assertFalse("the cancelled search cleared the flag on its way out", ctx.cancel());
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
        assertTrue(segment.blocks.get(segment.blocks.size() - 1).getX() > 50);
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
            new NetherPathfinder(1, null, null, 128);
            throw new AssertionError("expected NullPointerException");
        } catch (NullPointerException expected) {
            // good: the dimension is an enum, so the only bad one is a missing one
        }
    }
}
