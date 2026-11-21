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


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * When a cancel takes effect. A search is cancelled by a flag the searching thread reads, so the
 * question each of these asks is which search a given cancel belongs to.
 */
public class CancelTest {

    private static final NetherPathfinder.Dimension NETHER = NetherPathfinder.Dimension.NETHER;

    private static NetherPathfinder context() {
        return new NetherPathfinder(Oracle.SEED, null, NETHER, 128);
    }

    /**
     * The search PathFindTest uses to fill half a second: real terrain, far enough away that it
     * cannot arrive. Left alone it returns an unfinished segment once the primary timeout is up,
     * so "returned null quickly" means cancelled and nothing else.
     */
    private static PathSegment longSearch(NetherPathfinder ctx) {
        return ctx.pathFind(0, 50, 0, 100_000, 50, 0, true, false, 0, false, 1.0);
    }

    /** A few hundred blocks through known-empty chunks, which finishes in well under a second. */
    private static PathSegment shortSearch(NetherPathfinder ctx) {
        return ctx.pathFind(0, 60, 0, 500, 60, 0, true, false, 10_000, true, 8.0);
    }

    @Test(timeout = 60_000)
    public void aCancelThatArrivesBeforeTheSearchStartsStillStopsIt() {
        // This is the cancel destroy() sends. It can land while a search is still queued behind the
        // pathfinder's lock: clearing the flag as a search started used to drop it, and that search
        // then ran on with the context already torn down, holding the lock the game thread wants.
        final NetherPathfinder ctx = context();
        assertFalse("nothing has asked it to stop yet", ctx.cancel());

        final long t0 = System.nanoTime();
        final PathSegment segment = longSearch(ctx);
        final long ms = (System.nanoTime() - t0) / 1_000_000L;

        assertNull("a cancelled search has no path", segment);
        assertTrue("gave up at once rather than running the search out, took " + ms + " ms", ms < 400);
    }

    @Test(timeout = 60_000)
    public void aCancelAppliesToOneSearchAndNotTheNext() {
        // The search that was cancelled clears the flag on its way out, so the next one starts
        // clean. Without that, one cancel would kill every later search on the same context.
        final NetherPathfinder ctx = context();
        ctx.cancel();
        assertNull("the first search after the cancel is the one it stops", longSearch(ctx));

        final PathSegment segment = shortSearch(ctx);
        assertNotNull("the search after a cancelled one runs normally", segment);
        assertTrue(segment.finished);
    }

    @Test(timeout = 60_000)
    public void aSearchThatFinishesLeavesTheFlagClear() {
        final NetherPathfinder ctx = context();
        assertNotNull(shortSearch(ctx));
        // getAndSet reports what it replaced, so a false here says the flag was down
        assertFalse("a completed search did not leave the flag set", ctx.cancel());
    }
}
