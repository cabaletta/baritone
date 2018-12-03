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

package baritone.utils.pathing;

import baritone.Baritone;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.IPlayerContext;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.util.math.BlockPos;

public enum AvoidanceHelper {
    INSTANCE;

    public void apply(Long2DoubleOpenHashMap map, IPlayerContext ctx) {
        if (!Baritone.settings().avoidance.get()) {
            return;
        }
        long start = System.currentTimeMillis();
        double mobSpawnerCoeff = Baritone.settings().mobSpawnerAvoidanceCoefficient.get();
        double mobCoeff = Baritone.settings().mobAvoidanceCoefficient.get();
        if (mobSpawnerCoeff != 1.0D) {
            ctx.worldData().getCachedWorld().getLocationsOf("mob_spawner", 1, ctx.playerFeet().x, ctx.playerFeet().z, 2).forEach(mobspawner -> sphere(mobspawner, Baritone.settings().mobSpawnerAvoidanceRadius.get(), map, mobSpawnerCoeff));
        }
        if (mobCoeff != 1.0D) {
            ctx.world().loadedEntityList.stream().filter(entity -> entity instanceof EntityMob).forEach(entity -> sphere(new BlockPos(entity), Baritone.settings().mobAvoidanceRadius.get(), map, mobCoeff));
        }
        long end = System.currentTimeMillis();
        System.out.println("Took " + (end - start) + "ms to generate avoidance of " + map.size() + "  blocks");
    }

    private void sphere(BlockPos center, int radius, Long2DoubleOpenHashMap map, double coeff) {
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + y * y + z * z <= radius * radius) {
                        map.put(BetterBlockPos.longHash(center.getX() + x, center.getY() + y, center.getZ() + z), coeff);
                    }
                }
            }
        }
    }
}
