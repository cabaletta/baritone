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

package baritone.pathing.calc;

import baritone.api.utils.BetterBlockPos;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.MovementHelper;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

/**
 * Line-of-sight checks for any-angle pathfinding (Theta*).
 * <p>
 * Uses a 3D Bresenham-like line rasterization to check whether the
 * 2-block-tall player column can travel in a straight line from
 * {@code start} to {@code end} without hitting an obstacle.
 *
 * @author Ria
 */
public final class LineOfSight {

    /**
     * Maximum number of cached LOS pairs. A small LRU-style cache avoids
     * recomputing the same check hundreds of times when many nodes share
     * the same grandparent.
     */
    private static final int MAX_CACHE_SIZE = 512;
    private static final Long2ObjectOpenHashMap<Boolean> CACHE = new Long2ObjectOpenHashMap<>();

    private LineOfSight() {}

    /**
     * Check whether there is an unobstructed straight line from start to end.
     * <p>
     * The check treats the player as a 2-block-tall column (feet at y, head at y+1).
     * At every intermediate block position along the line:
     * <ul>
     *   <li>The feet block must be walk-through-able.</li>
     *   <li>The head block must be walk-through-able.</li>
     *   <li>The block under the feet must be walk-on-able (solid ground).</li>
     * </ul>
     *
     * @param ctx   the calculation context (world access)
     * @param start the starting position
     * @param end   the ending position
     * @return true if the straight-line path is clear, false otherwise
     */
    public static boolean hasLineOfSight(CalculationContext ctx, BetterBlockPos start, BetterBlockPos end) {
        // Adjacent or same block — trivially clear (movement handles the step)
        int dx = end.x - start.x;
        int dy = end.y - start.y;
        int dz = end.z - start.z;
        int maxDiff = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
        if (maxDiff <= 1) {
            return true;
        }

        // Check cache
        long key = packKey(start.hashCode(), end.hashCode());
        Boolean cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }

        // Sample the line using sub-block precision (center of each block).
        // Number of steps = Chebyshev distance, so we check every block
        // that the line passes through.
        double stepX = (double) dx / maxDiff;
        double stepY = (double) dy / maxDiff;
        double stepZ = (double) dz / maxDiff;

        // Start from the center of the start block
        double curX = start.x + 0.5;
        double curY = start.y;
        double curZ = start.z + 0.5;

        int lastBX = start.x;
        int lastBY = start.y;
        int lastBZ = start.z;

        for (int i = 1; i < maxDiff; i++) {
            curX += stepX;
            curY += stepY;
            curZ += stepZ;

            int bx = (int) Math.floor(curX);
            int by = (int) Math.floor(curY);
            int bz = (int) Math.floor(curZ);

            // Skip duplicate checks when the line stays in the same block
            if (bx == lastBX && by == lastBY && bz == lastBZ) {
                continue;
            }
            lastBX = bx;
            lastBY = by;
            lastBZ = bz;

            // Player body column: feet must be walk-through-able,
            // head must be walk-through-able, and there must be solid ground.
            if (!MovementHelper.canWalkThrough(ctx, bx, by, bz)
                    || !MovementHelper.canWalkThrough(ctx, bx, by + 1, bz)
                    || !MovementHelper.canWalkOn(ctx, bx, by - 1, bz)) {
                putCache(key, false);
                return false;
            }
        }

        putCache(key, true);
        return true;
    }

    /**
     * Pack two int hash codes into a single long for caching.
     */
    private static long packKey(int a, int b) {
        return ((long) a << 32) | (b & 0xFFFFFFFFL);
    }

    private static void putCache(long key, boolean value) {
        if (CACHE.size() >= MAX_CACHE_SIZE) {
            // Simple eviction: remove a random entry (first key in the set)
            CACHE.long2ObjectEntrySet().iterator().next();
            CACHE.long2ObjectEntrySet().fastIterator().remove();
        }
        CACHE.put(Long.valueOf(key), Boolean.valueOf(value));
    }

    /**
     * Clear the LOS cache. Call when the world context changes
     * (dimension switch, chunk reload, etc.).
     */
    public static void clearCache() {
        CACHE.clear();
    }
}
