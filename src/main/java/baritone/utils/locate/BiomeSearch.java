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
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import java.util.concurrent.CancellationException;

/** Searches generation data without loading or generating chunks. */
public final class BiomeSearch {
    public static final int STEP = 32;
    public static final int REFINE_STEP = 4;
    public static final int DEFAULT_RADIUS = 6400;
    public static final int MAX_RADIUS = 12800;

    private BiomeSearch() {}

    public record Bounds(int minY, int maxY, double minX, double maxX, double minZ, double maxZ) {
        public boolean contains(int x, int z) {
            return x >= minX && x + 1 <= maxX && z >= minZ && z + 1 <= maxZ;
        }
    }

    @FunctionalInterface
    public interface Matcher {
        boolean matches(int x, int y, int z);
    }

    public static BlockPos find(BiomeSource source, Climate.Sampler sampler, ResourceKey<Biome> biome,
                                BlockPos origin, int radius, Bounds bounds) {
        if (source.possibleBiomes().stream().noneMatch(holder -> holder.is(biome))) {
            throw new IllegalArgumentException("That biome does not generate in this dimension/preset.");
        }
        return find((x, y, z) -> source.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y),
                QuartPos.fromBlock(z), sampler).is(biome), origin, radius, bounds);
    }

    /** Expanding square rings, with heights visited nearest the player's height first. */
    public static BlockPos find(Matcher matcher, BlockPos origin, int radius, Bounds bounds) {
        if (radius < STEP || radius > MAX_RADIUS) {
            throw new IllegalArgumentException("Radius must be between " + STEP + " and " + MAX_RADIUS + ".");
        }
        int startY = Math.max(bounds.minY, Math.min(bounds.maxY, origin.getY()));
        for (int ring = 0; ring <= radius / STEP; ring++) {
            BlockPos closest = null;
            for (int dx = -ring; dx <= ring; dx++) {
                int zStep = Math.abs(dx) == ring ? 1 : 2 * ring;
                for (int dz = -ring; dz <= ring; dz += zStep) {
                    checkCancelled();
                    int x = origin.getX() + dx * STEP;
                    int z = origin.getZ() + dz * STEP;
                    if (!bounds.contains(x, z)) {
                        continue;
                    }
                    for (int offset = 0; offset <= bounds.maxY - bounds.minY + STEP; offset += STEP) {
                        int below = startY - offset;
                        int above = startY + offset;
                        checkCancelled();
                        if (below >= bounds.minY && matcher.matches(x, below, z)) {
                            closest = closer(origin, closest, new BlockPos(x, below, z));
                            break;
                        }
                        if (offset != 0 && above <= bounds.maxY && matcher.matches(x, above, z)) {
                            closest = closer(origin, closest, new BlockPos(x, above, z));
                            break;
                        }
                    }
                }
            }
            if (closest != null) {
                return refine(matcher, origin, closest, radius, bounds);
            }
        }
        return null;
    }

    /** Refine a coarse match at biome-cell resolution, without claiming a global nearest boundary. */
    private static BlockPos refine(Matcher matcher, BlockPos origin, BlockPos sample, int radius, Bounds bounds) {
        BlockPos closest = sample;
        for (int x = sample.getX() - STEP; x <= sample.getX() + STEP; x += REFINE_STEP) {
            for (int z = sample.getZ() - STEP; z <= sample.getZ() + STEP; z += REFINE_STEP) {
                checkCancelled();
                if (Math.abs((long) x - origin.getX()) > radius || Math.abs((long) z - origin.getZ()) > radius
                        || !bounds.contains(x, z)) {
                    continue;
                }
                for (int y = sample.getY() - STEP; y <= sample.getY() + STEP; y += REFINE_STEP) {
                    checkCancelled();
                    if (y < bounds.minY || y > bounds.maxY) {
                        continue;
                    }
                    BlockPos candidate = new BlockPos(x, y, z);
                    if (closer(origin, closest, candidate) == candidate && matcher.matches(x, y, z)) {
                        closest = candidate;
                    }
                }
            }
        }
        return closest;
    }

    private static BlockPos closer(BlockPos origin, BlockPos first, BlockPos second) {
        // Prefer shorter horizontal travel, then the height closest to the player.
        if (first == null || horizontalDistanceSquared(origin, second) < horizontalDistanceSquared(origin, first)
                || (horizontalDistanceSquared(origin, second) == horizontalDistanceSquared(origin, first)
                && Math.abs(second.getY() - origin.getY()) < Math.abs(first.getY() - origin.getY()))) {
            return second;
        }
        return first;
    }

    public static long horizontalDistanceSquared(BlockPos first, BlockPos second) {
        long x = (long) first.getX() - second.getX();
        long z = (long) first.getZ() - second.getZ();
        return x * x + z * z;
    }

    public static void checkCancelled() {
        if (Thread.currentThread().isInterrupted()) {
            throw new CancellationException();
        }
    }
}
