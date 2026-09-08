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

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class BiomeGeneratorTest {
    @BeforeClass
    public static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    public void allDimensionsUseRealGenerationAndRejectForeignBiomes() {
        for (ResourceKey<Level> dimension : List.of(Level.OVERWORLD, Level.NETHER, Level.END)) {
            BiomeGenerator generator = BiomeGenerator.vanilla(dimension, 0, "default");
            ResourceKey<Biome> target = generator.source().getNoiseBiome(0, 16, 0, generator.sampler()).unwrapKey().orElseThrow();
            BlockPos found = BiomeSearch.find(generator.source(), generator.sampler(), target, new BlockPos(0, 64, 0),
                    32, new BiomeSearch.Bounds(0, 128, -1000, 1000, -1000, 1000));
            assertEquals(new BlockPos(0, 64, 0), found);
            ResourceKey<Biome> foreign = dimension.equals(Level.NETHER) ? Biomes.PLAINS : Biomes.NETHER_WASTES;
            assertThrows(IllegalArgumentException.class, () -> BiomeSearch.find(generator.source(), generator.sampler(),
                    foreign, BlockPos.ZERO, 32, new BiomeSearch.Bounds(0, 128, -1000, 1000, -1000, 1000)));
        }
    }

    @Test
    public void seedAndLargeBiomePresetAffectPredictionsDeterministically() {
        List<ResourceKey<Biome>> baseline = samples(12345, "default");
        assertEquals(baseline, samples(12345, "default"));
        assertNotEquals(baseline, samples(-12345, "default"));
        assertNotEquals(baseline, samples(12345, "large_biomes"));
        assertFalse(samples(12345, "amplified").isEmpty());
    }

    private List<ResourceKey<Biome>> samples(long seed, String preset) {
        BiomeGenerator generator = BiomeGenerator.vanilla(Level.OVERWORLD, seed, preset);
        List<ResourceKey<Biome>> result = new ArrayList<>();
        for (int x = -512; x <= 512; x += 128) {
            for (int z = -512; z <= 512; z += 128) {
                result.add(generator.source().getNoiseBiome(x, 16, z, generator.sampler()).unwrapKey().orElseThrow());
            }
        }
        return result;
    }
}
