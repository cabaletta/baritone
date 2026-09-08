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
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.chunk.StructureAccess;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.MockMakers;

import java.util.List;
import java.util.concurrent.CancellationException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

public class StructureSearchTest {
    @BeforeClass
    public static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    public void readableNamesAndTags() {
        assertEquals(new StructureTarget("end_city", 6400), StructureTarget.parse("end city"));
        assertEquals(new StructureTarget("end_city", 10000), StructureTarget.parse("End City 10000"));
        assertEquals("#minecraft:village", StructureTarget.parse("village").name());
        assertEquals("minecraft:ruined_portal", StructureTarget.parse("minecraft:ruined_portal").name());
        assertThrows(IllegalArgumentException.class, () -> StructureTarget.parse("end city 99999999999"));
        assertEquals(List.of("city"), StructureTarget.complete("end c").toList());
        assertEquals(List.of("end_city"), StructureTarget.complete("end_").toList());
    }

    @Test
    public void arrivalMarginAllowsWaterSurfaceButNotSurfaceAboveDeepStructures() {
        var monument = new StructureSearch.Result(BuiltinStructures.OCEAN_MONUMENT, BlockPos.ZERO,
                new BoundingBox(0, 39, 0, 60, 61, 60));
        assertTrue(monument.containsArrivalPosition(new BlockPos(30, 63, 30)));
        var city = new StructureSearch.Result(BuiltinStructures.ANCIENT_CITY, BlockPos.ZERO,
                new BoundingBox(0, -50, 0, 60, -20, 60));
        assertFalse(city.containsArrivalPosition(new BlockPos(30, 64, 30)));
    }

    @Test
    public void loadsTaggedStructuresAndPredictsAnEndCity() throws Exception {
        try (var context = StructurePredictionContext.vanilla(Level.END, 0, "default")) {
            assertTrue(context.registries.lookupOrThrow(Registries.STRUCTURE)
                    .getValueOrThrow(BuiltinStructures.END_CITY).biomes().size() > 0);
            assertTrue(context.templates.get(Identifier.parse("minecraft:end_city/base_floor")).isPresent());
            StructureSearch.Result result = StructureSearch.find(context, StructureTarget.parse("end city"),
                    new BlockPos(1000, 80, 1000), new BiomeSearch.Bounds(0, 255, -30000000, 30000000, -30000000, 30000000));
            assertNotNull(result);
            assertEquals(BuiltinStructures.END_CITY, result.structure());
            assertTrue(result.box().minY() >= 60);
            assertMatchesNativeGeneration(context, result);
            assertThrows(IllegalArgumentException.class, () -> StructureSearch.find(context, StructureTarget.parse("village"),
                    BlockPos.ZERO, new BiomeSearch.Bounds(0, 255, -10000, 10000, -10000, 10000)));
        }
    }

    @Test
    public void villagesAndStrongholdsMatchNativeGenerationAtNegativeCoordinates() throws Exception {
        try (var context = StructurePredictionContext.vanilla(Level.OVERWORLD, 12345, "default")) {
            var registry = context.registries.lookupOrThrow(Registries.STRUCTURE);
            StructureTarget.suggestions().forEach(name -> assertFalse(name, StructureTarget.parse(name).resolve(registry).isEmpty()));
            StructureTarget.complete("minecraft:").forEach(name -> assertFalse(StructureTarget.parse(name).resolve(registry).isEmpty()));
            for (String name : List.of("village", "stronghold")) {
                var result = StructureSearch.find(context, StructureTarget.parse(name), new BlockPos(-2000, 64, -2000),
                        new BiomeSearch.Bounds(-64, 319, -30000000, 30000000, -30000000, 30000000));
                assertNotNull(name, result);
                assertMatchesNativeGeneration(context, result);
            }
        }
    }

    @Test
    public void netherCompetitionDoesNotReportBastionAsFortress() throws Exception {
        try (var context = StructurePredictionContext.vanilla(Level.NETHER, 0, "default")) {
            for (String name : List.of("fortress", "bastion")) {
                var result = StructureSearch.find(context, StructureTarget.parse(name), new BlockPos(-100, 64, -100),
                        new BiomeSearch.Bounds(0, 127, -30000000, 30000000, -30000000, 30000000));
                assertNotNull(name, result);
                assertMatchesNativeGeneration(context, result);
            }
        }
    }

    @Test
    public void cancellationAndBordersAreRespected() throws Exception {
        try (var context = StructurePredictionContext.vanilla(Level.END, 0, "default")) {
            assertNull(StructureSearch.find(context, StructureTarget.parse("end city"), BlockPos.ZERO,
                    new BiomeSearch.Bounds(0, 255, 0, 1, 0, 1)));
            Thread.currentThread().interrupt();
            try {
                assertThrows(CancellationException.class, () -> StructureSearch.find(context, StructureTarget.parse("end city"),
                        BlockPos.ZERO, new BiomeSearch.Bounds(0, 255, -10000, 10000, -10000, 10000)));
            } finally {
                Thread.interrupted();
            }
        }
    }

    /** Independent end-to-end oracle: Minecraft's own chunk structure generation and selection. */
    private void assertMatchesNativeGeneration(StructurePredictionContext context, StructureSearch.Result result) {
        ChunkPos position = new ChunkPos(result.position().getX() >> 4, result.position().getZ() >> 4);
        ProtoChunk chunk = new ProtoChunk(position, UpgradeData.EMPTY, context.height,
                PalettedContainerFactory.create(context.registries), null);
        StructureManager manager = mock(StructureManager.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        doAnswer(call -> {
            Structure structure = call.getArgument(1);
            StructureStart start = call.getArgument(2);
            StructureAccess access = call.getArgument(3);
            access.setStartForStructure(structure, start);
            return null;
        }).when(manager).setStartForStructure(any(), any(), any(), any());
        context.generator.createStructures(context.registries, context.placements, manager, chunk, context.templates, context.dimension);
        StructureStart actual = chunk.getStartForStructure(context.registries.lookupOrThrow(Registries.STRUCTURE).getValueOrThrow(result.structure()));
        assertNotNull(result.structure().toString(), actual);
        assertTrue(actual.isValid());
        assertEquals(actual.getBoundingBox(), result.box());
    }
}
