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

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.biome.TheEndBiomeSource;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;

public record BiomeGenerator(BiomeSource source, Climate.Sampler sampler) {
    // Initialized on the search worker, not on the render thread. No seed-specific state is cached.
    private static final class Vanilla {
        private static final HolderLookup.Provider REGISTRIES = VanillaRegistries.createLookup();
    }

    public static BiomeGenerator vanilla(ResourceKey<Level> dimension, long seed, String preset) {
        NoiseBasedChunkGenerator generator = vanillaGenerator(Vanilla.REGISTRIES, dimension, preset);
        return new BiomeGenerator(generator.getBiomeSource(), RandomState.create(generator.generatorSettings().value(),
                Vanilla.REGISTRIES.lookupOrThrow(Registries.NOISE), seed).sampler());
    }

    public static NoiseBasedChunkGenerator vanillaGenerator(HolderLookup.Provider registries,
                                                           ResourceKey<Level> dimension, String preset) {
        if (!LocateSettings.validPreset(preset)) {
            throw new IllegalArgumentException("Unknown generation preset: " + preset);
        }
        ResourceKey<NoiseGeneratorSettings> settings;
        BiomeSource source;
        if (dimension.equals(Level.OVERWORLD)) {
            settings = switch (preset) {
                case "large_biomes" -> NoiseGeneratorSettings.LARGE_BIOMES;
                case "amplified" -> NoiseGeneratorSettings.AMPLIFIED;
                default -> NoiseGeneratorSettings.OVERWORLD;
            };
            source = MultiNoiseBiomeSource.createFromPreset(registries.lookupOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)
                    .getOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD));
        } else if (dimension.equals(Level.NETHER)) {
            settings = NoiseGeneratorSettings.NETHER;
            source = MultiNoiseBiomeSource.createFromPreset(registries.lookupOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)
                    .getOrThrow(MultiNoiseBiomeSourceParameterLists.NETHER));
        } else if (dimension.equals(Level.END)) {
            settings = NoiseGeneratorSettings.END;
            source = TheEndBiomeSource.create(registries.lookupOrThrow(Registries.BIOME));
        } else {
            throw new IllegalArgumentException("Seed prediction supports only vanilla Overworld, Nether and End dimensions.");
        }
        return new NoiseBasedChunkGenerator(source, registries.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(settings));
    }
}
