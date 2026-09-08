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

import baritone.pathing.movement.MovementHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;

import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Predicate;

/** Bounded main-thread refinement against loaded terrain; never requests a chunk load. */
public final class BiomeDestinationSearch {
    public static final int RADIUS = 16;
    public static final int CHECKS_PER_TICK = 2048;
    private static final int MAX_CANDIDATES = 16;
    private final BlockPos center;
    private final int minY;
    private final int height;
    private final Predicate<BlockPos> suitable;
    private final TreeSet<BlockPos> candidates;
    private int cursor;

    public BiomeDestinationSearch(BlockPos center, BlockPos origin, int minY, int maxY, Predicate<BlockPos> suitable) {
        this.center = center;
        this.minY = minY;
        this.height = Math.max(0, maxY - minY + 1);
        this.suitable = suitable;
        this.candidates = new TreeSet<>(Comparator.<BlockPos>comparingLong(pos -> {
            long dy = (long) pos.getY() - origin.getY();
            return BiomeSearch.horizontalDistanceSquared(pos, origin) + 4 * dy * dy;
        }).thenComparing(BlockPos::compareTo));
    }

    /** Returns true once the bounded region has been scanned. */
    public boolean tick() {
        int width = RADIUS * 2 + 1;
        int total = width * width * height;
        for (int checks = 0; checks < CHECKS_PER_TICK && cursor < total; checks++, cursor++) {
            int column = cursor / height;
            BlockPos pos = new BlockPos(center.getX() + column / width - RADIUS,
                    minY + cursor % height, center.getZ() + column % width - RADIUS);
            if (suitable.test(pos)) {
                candidates.add(pos);
                if (candidates.size() > MAX_CANDIDATES) {
                    candidates.pollLast();
                }
            }
        }
        return cursor >= total;
    }

    public List<BlockPos> candidates() {
        return List.copyOf(candidates);
    }

    public static boolean isSuitable(Level world, ResourceKey<Biome> biome, BlockPos pos) {
        return isStandingPosition(world, pos) && world.getBiome(pos).is(biome);
    }

    public static boolean isStandingPosition(Level world, BlockPos pos) {
        if (!world.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)
                || !world.getWorldBorder().isWithinBounds(pos)
                || pos.getY() <= world.getMinY() || pos.getY() >= world.getMaxY()) {
            return false;
        }
        var feet = world.getBlockState(pos);
        // Require existing space rather than choosing a point buried in solid terrain.
        if (!feet.getCollisionShape(world, pos).isEmpty()) {
            return false;
        }
        var headPos = pos.above();
        var head = world.getBlockState(headPos);
        if (!head.getCollisionShape(world, headPos).isEmpty() || MovementHelper.avoidWalkingInto(head)) {
            return false;
        }
        if (feet.getFluidState().is(Fluids.WATER) || feet.getFluidState().is(Fluids.FLOWING_WATER)) {
            // A breathing position at the water surface is a valid ocean destination.
            if (feet.is(Blocks.BUBBLE_COLUMN)) {
                return false;
            }
        } else {
            var floorPos = pos.below();
            var floor = world.getBlockState(floorPos);
            if (MovementHelper.avoidWalkingInto(feet) || floor.is(Blocks.MAGMA_BLOCK)
                    || MovementHelper.avoidWalkingInto(floor) || floor.is(Blocks.CAMPFIRE)
                    || floor.is(Blocks.SOUL_CAMPFIRE)
                    || !floor.isFaceSturdy(world, floorPos, Direction.UP)) {
                return false;
            }
        }
        return true;
    }
}
