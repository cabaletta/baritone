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
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/** Vanilla placement, weighted structure selection and full start generation, without live chunk access. */
public final class StructureSearch {
    private StructureSearch() {}

    public record Result(ResourceKey<Structure> structure, BlockPos position, BoundingBox box) {
        public boolean containsArrivalPosition(BlockPos pos) {
            // Allow an adjacent entrance or a breathing position just above an underwater structure.
            // Deep structures still require approaching their actual height rather than stopping on the surface.
            return pos.getX() >= box.minX() - 2 && pos.getX() <= box.maxX() + 2
                    && pos.getZ() >= box.minZ() - 2 && pos.getZ() <= box.maxZ() + 2
                    && pos.getY() >= box.minY() - 2 && pos.getY() <= box.maxY() + 4;
        }
    }
    private record Candidate(StructureSet set, ChunkPos chunk, BlockPos locatePos) {}

    public static Result find(StructurePredictionContext context, StructureTarget target,
                              BlockPos origin, BiomeSearch.Bounds bounds) {
        Set<ResourceKey<Structure>> requested = target.resolve(context.registries.lookupOrThrow(Registries.STRUCTURE));
        if (requested.stream().noneMatch(key -> context.registries.lookupOrThrow(Registries.STRUCTURE).getValueOrThrow(key)
                .biomes().stream().anyMatch(context.generator.getBiomeSource().possibleBiomes()::contains))) {
            throw new IllegalArgumentException("That structure does not generate in this dimension/preset.");
        }
        List<Candidate> candidates = new ArrayList<>();
        boolean possible = false;
        for (Holder<StructureSet> holder : context.placements.possibleStructureSets()) {
            BiomeSearch.checkCancelled();
            StructureSet set = holder.value();
            if (set.structures().stream().noneMatch(entry -> requested.contains(entry.structure().unwrapKey().orElseThrow()))) {
                continue;
            }
            possible = true;
            if (set.placement() instanceof RandomSpreadStructurePlacement spread) {
                int minX = Math.floorDiv(Math.floorDiv(origin.getX() - target.radius(), 16), spread.spacing());
                int maxX = Math.floorDiv(Math.floorDiv(origin.getX() + target.radius(), 16), spread.spacing());
                int minZ = Math.floorDiv(Math.floorDiv(origin.getZ() - target.radius(), 16), spread.spacing());
                int maxZ = Math.floorDiv(Math.floorDiv(origin.getZ() + target.radius(), 16), spread.spacing());
                for (int x = minX; x <= maxX; x++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        BiomeSearch.checkCancelled();
                        ChunkPos chunk = spread.getPotentialStructureChunk(context.seed, x * spread.spacing(), z * spread.spacing());
                        addCandidate(context, target.radius(), origin, bounds, set, chunk, candidates);
                    }
                }
            } else if (set.placement() instanceof ConcentricRingsStructurePlacement rings) {
                List<ChunkPos> positions = context.placements.getRingPositionsFor(rings);
                if (positions != null) {
                    for (ChunkPos chunk : positions) {
                        BiomeSearch.checkCancelled();
                        addCandidate(context, target.radius(), origin, bounds, set, chunk, candidates);
                    }
                }
            } else {
                throw new IllegalArgumentException("Unsupported structure placement: " + set.placement().getClass().getSimpleName());
            }
        }
        if (!possible) {
            throw new IllegalArgumentException("That structure does not generate in this dimension/preset.");
        }
        candidates.sort(Comparator.comparingLong(candidate -> BiomeSearch.horizontalDistanceSquared(origin, candidate.locatePos)));
        for (Candidate candidate : candidates) {
            BiomeSearch.checkCancelled();
            StructureStart start = generateSelected(context, candidate.set, candidate.chunk);
            if (start != null) {
                ResourceKey<Structure> actual = context.registries.lookupOrThrow(Registries.STRUCTURE)
                        .getResourceKey(start.getStructure()).orElseThrow();
                if (requested.contains(actual)) {
                    BoundingBox box = start.getBoundingBox();
                    return new Result(actual, candidate.locatePos.atY(Math.max(bounds.minY(), Math.min(bounds.maxY(), box.minY()))), box);
                }
            }
        }
        return null;
    }

    private static void addCandidate(StructurePredictionContext context, int radius, BlockPos origin, BiomeSearch.Bounds bounds,
                                     StructureSet set, ChunkPos chunk, List<Candidate> candidates) {
        BlockPos pos = set.placement().getLocatePos(chunk);
        if (Math.abs((long) pos.getX() - origin.getX()) <= radius && Math.abs((long) pos.getZ() - origin.getZ()) <= radius
                && bounds.contains(pos.getX(), pos.getZ()) && set.placement().isStructureChunk(context.placements, chunk.x(), chunk.z())) {
            candidates.add(new Candidate(set, chunk, pos));
        }
    }

    /** Same weighted retry order as ChunkGenerator.createStructures, including competing entries. */
    static StructureStart generateSelected(StructurePredictionContext context, StructureSet set, ChunkPos chunk) {
        List<StructureSet.StructureSelectionEntry> remaining = new ArrayList<>(set.structures());
        WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(0));
        random.setLargeFeatureSeed(context.seed, chunk.x(), chunk.z());
        int weight = remaining.stream().mapToInt(StructureSet.StructureSelectionEntry::weight).sum();
        while (!remaining.isEmpty()) {
            BiomeSearch.checkCancelled();
            int selection = remaining.size() == 1 ? 0 : random.nextInt(weight);
            int index = 0;
            while (selection >= remaining.get(index).weight()) {
                selection -= remaining.get(index++).weight();
            }
            var entry = remaining.remove(index);
            weight -= entry.weight();
            Structure structure = entry.structure().value();
            StructureStart start = structure.generate(entry.structure(), context.dimension, context.registries,
                    context.generator, context.generator.getBiomeSource(), context.randomState, context.templates,
                    context.seed, chunk, 0, context.height, structure.biomes()::contains);
            if (start.isValid()) {
                return start;
            }
        }
        return null;
    }
}
