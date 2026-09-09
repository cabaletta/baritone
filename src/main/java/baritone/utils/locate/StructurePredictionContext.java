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

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.tags.TagLoader;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.LevelStorageSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

/** Private generation state. No live chunks are loaded or changed by a structure prediction. */
public final class StructurePredictionContext implements AutoCloseable {
    public final RegistryAccess registries;
    public final ResourceKey<Level> dimension;
    public final ChunkGenerator generator;
    public final RandomState randomState;
    public final ChunkGeneratorStructureState placements;
    public final StructureTemplateManager templates;
    public final long seed;
    public final LevelHeightAccessor height;
    private final AutoCloseable cleanup;

    private StructurePredictionContext(RegistryAccess registries, ResourceKey<Level> dimension, ChunkGenerator generator,
                                       RandomState randomState, StructureTemplateManager templates, long seed,
                                       LevelHeightAccessor height, AutoCloseable cleanup) {
        this.registries = registries;
        this.dimension = dimension;
        this.generator = generator;
        this.randomState = randomState;
        this.templates = templates;
        this.seed = seed;
        this.height = height;
        this.cleanup = cleanup;
        this.placements = generator.createState(registries.lookupOrThrow(Registries.STRUCTURE_SET), randomState, seed);
    }

    /** Capture server-owned references on the client thread; build private noise state on the worker. */
    public static Supplier<StructurePredictionContext> singleplayer(ServerLevel level, StructureTemplateManager templates) {
        RegistryAccess registries = level.registryAccess();
        ResourceKey<Level> dimension = level.dimension();
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        RandomState originalRandom = level.getChunkSource().randomState();
        long seed = level.getSeed();
        LevelHeightAccessor height = LevelHeightAccessor.create(level.getMinY(), level.getHeight());
        return () -> new StructurePredictionContext(registries, dimension, generator,
                generator instanceof NoiseBasedChunkGenerator noise
                        ? RandomState.create(noise.generatorSettings().value(), registries.lookupOrThrow(Registries.NOISE), seed)
                        : originalRandom, templates, seed, height, () -> {});
    }

    // VanillaRegistries.createLookup alone has no loaded structure biome tags. Load real server data
    // in a private registry set, without applying tag changes to the connected client's registries.
    private static final class Vanilla {
        private static final MultiPackResourceManager RESOURCES = new MultiPackResourceManager(PackType.SERVER_DATA,
                List.of(ServerPacksSource.createVanillaPackSource()));
        private static final RegistryAccess.Frozen REGISTRIES = loadRegistries();

        private static RegistryAccess.Frozen loadRegistries() {
            RegistryAccess.Frozen builtins = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
            var lookups = TagLoader.buildUpdatedLookups(builtins, TagLoader.loadTagsForExistingRegistries(RESOURCES, builtins));
            RegistryAccess.Frozen generated = RegistryDataLoader.load(RESOURCES, lookups,
                    RegistryDataLoader.WORLDGEN_REGISTRIES, Runnable::run).join();
            return new RegistryAccess.ImmutableRegistryAccess(Stream.concat(builtins.registries(), generated.registries())).freeze();
        }
    }

    public static StructurePredictionContext vanilla(ResourceKey<Level> dimension, long seed, String preset) throws IOException {
        RegistryAccess registries = Vanilla.REGISTRIES;
        NoiseBasedChunkGenerator generator = BiomeGenerator.vanillaGenerator(registries, dimension, preset);
        RandomState random = RandomState.create(generator.generatorSettings().value(), registries.lookupOrThrow(Registries.NOISE), seed);
        // The vanilla template manager requires a save access for its optional disk-template source.
        // Give it a private empty directory rather than pointing it at a user's world.
        Path temporary = Files.createTempDirectory("baritone-structure-prediction-");
        LevelStorageSource.LevelStorageAccess access = null;
        try {
            access = LevelStorageSource.createDefault(temporary).createAccess("templates");
            StructureTemplateManager templates = new StructureTemplateManager(Vanilla.RESOURCES, access,
                    DataFixers.getDataFixer(), registries.lookupOrThrow(Registries.BLOCK));
            LevelStorageSource.LevelStorageAccess ownedAccess = access;
            return new StructurePredictionContext(registries, dimension, generator, random, templates, seed,
                    LevelHeightAccessor.create(generator.getMinY(), generator.getGenDepth()), () -> {
                        try { ownedAccess.close(); } finally { deleteTemporaryDirectory(temporary); }
                    });
        } catch (IOException | RuntimeException | Error e) {
            if (access != null) {
                try { access.close(); } catch (IOException closeError) { e.addSuppressed(closeError); }
            }
            try { deleteTemporaryDirectory(temporary); } catch (IOException closeError) { e.addSuppressed(closeError); }
            throw e;
        }
    }

    private static void deleteTemporaryDirectory(Path temporary) throws IOException {
        // Only the freshly created, privately owned directory is ever traversed; no symlinks are followed.
        try (Stream<Path> entries = Files.walk(temporary)) {
            for (Path entry : entries.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(entry);
            }
        }
    }

    @Override
    public void close() throws Exception {
        cleanup.close();
    }
}
