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

package baritone.process;

import baritone.Baritone;
import baritone.api.IBaritone;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.pathing.goals.GoalComposite;
import baritone.api.pathing.goals.GoalXZ;
import baritone.api.process.IBaritoneProcess;
import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;
import baritone.api.utils.Helper;
import baritone.api.utils.IPlayerContext;
import baritone.utils.locate.BiomeGenerator;
import baritone.utils.locate.BiomeSearch;
import baritone.utils.locate.BiomeDestinationSearch;
import baritone.utils.locate.StructurePredictionContext;
import baritone.utils.locate.StructureSearch;
import baritone.utils.locate.StructureTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.Executor;
import java.util.concurrent.Callable;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Owns both searching and travel so cancellation cannot start a stale path later. */
public final class LocateProcess implements IBaritoneProcess, Helper {
    private final IPlayerContext ctx;
    private final Executor executor;
    private final Consumer<String> logger;
    private Level world;
    private ResourceKey<Biome> biome;
    private FutureTask<SearchResult> search;
    private StructureSearch.Result structure;
    private String targetName;
    private BlockPos destination;
    private Goal goal;
    private BiomeDestinationSearch refinement;
    private List<BlockPos> candidates;
    private int radius;
    private int waitingForChunks;

    private record SearchResult(BlockPos position, StructureSearch.Result structure) {}

    public LocateProcess(IBaritone baritone) {
        this(baritone.getPlayerContext(), Baritone.getExecutor(), Helper.HELPER::logDirect);
    }

    LocateProcess(IPlayerContext ctx, Executor executor, Consumer<String> logger) {
        this.ctx = ctx;
        this.executor = executor;
        this.logger = logger;
    }

    public void locate(ResourceKey<Biome> biome, int radius, Supplier<BiomeGenerator> generator) {
        onLostControl();
        Level world = ctx.world();
        BlockPos origin = ctx.playerFeet();
        var border = world.getWorldBorder();
        BiomeSearch.Bounds bounds = new BiomeSearch.Bounds(world.getMinY(), world.getMaxY(),
                border.getMinX(), border.getMaxX(), border.getMinZ(), border.getMaxZ());
        FutureTask<SearchResult> task = new FutureTask<>(() -> {
            BiomeSearch.checkCancelled();
            BiomeGenerator data = generator.get();
            BlockPos pos = BiomeSearch.find(data.source(), data.sampler(), biome, origin, radius, bounds);
            return pos == null ? null : new SearchResult(pos, null);
        });
        this.world = world;
        this.biome = biome;
        this.targetName = "biome " + biome.identifier();
        this.radius = radius;
        this.search = task;
        try {
            executor.execute(task);
        } catch (RuntimeException e) {
            onLostControl();
            throw e;
        }
    }

    public void locateStructure(StructureTarget target, Callable<StructurePredictionContext> generator) {
        onLostControl();
        this.world = ctx.world();
        this.radius = target.radius();
        this.targetName = "structure " + target.name();
        BlockPos origin = ctx.playerFeet();
        var border = world.getWorldBorder();
        BiomeSearch.Bounds bounds = new BiomeSearch.Bounds(world.getMinY(), world.getMaxY(),
                border.getMinX(), border.getMaxX(), border.getMinZ(), border.getMaxZ());
        search = new FutureTask<>(() -> {
            BiomeSearch.checkCancelled();
            try (StructurePredictionContext context = generator.call()) {
                StructureSearch.Result result = StructureSearch.find(context, target, origin, bounds);
                return result == null ? null : new SearchResult(result.position(), result);
            }
        });
        try {
            executor.execute(search);
        } catch (RuntimeException e) {
            onLostControl();
            throw e;
        }
    }

    @Override
    public boolean isActive() {
        return world != null;
    }

    @Override
    public PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel) {
        if (ctx.world() != world || ctx.player() == null) {
            return finish(null);
        }
        if (biome != null && world.getBiome(ctx.playerFeet()).is(biome) && isSafeToCancel) {
            return finish("Arrived in " + biome.identifier() + ".");
        }
        if (structure != null && candidates != null && isSafeToCancel && structure.containsArrivalPosition(ctx.playerFeet())
                && BiomeDestinationSearch.isStandingPosition(world, ctx.playerFeet())) {
            return finish("Reached the predicted " + structure.structure().identifier()
                    + " area. Structure presence is not confirmed by the client; older or custom terrain may differ.");
        }
        if (search != null) {
            if (!search.isDone()) {
                return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
            }
            try {
                SearchResult result = search.get();
                if (result != null) {
                    destination = result.position();
                    structure = result.structure();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return finish("Locate search interrupted.");
            } catch (CancellationException e) {
                return finish(null);
            } catch (ExecutionException e) {
                return finish("Locate search failed: " + e.getCause().getMessage());
            }
            search = null;
            if (destination == null) {
                return finish("No matching " + targetName + " within " + radius + " blocks. Try a larger radius (up to "
                        + BiomeSearch.MAX_RADIUS + ")." + (biome == null ? "" : " Narrow biomes can fall between 32-block samples."));
            }
            if (!world.getWorldBorder().isWithinBounds(destination)) {
                return finish("The located destination is outside the current world border.");
            }
            logger.accept("Predicted " + targetName + " at " + destination.getX() + ", "
                    + destination.getY() + ", " + destination.getZ() + ". Travelling there.");
            // Approach horizontally until the target terrain can be inspected.
            goal = new GoalXZ(destination.getX(), destination.getZ());
        }
        if (!world.getWorldBorder().isWithinBounds(destination)) {
            return finish("The world border moved across the destination.");
        }
        if (refinement == null && candidates == null
                && BiomeSearch.horizontalDistanceSquared(ctx.playerFeet(), destination) <= 32L * 32
                && destinationAreaLoaded()) {
            int minY = structure == null ? world.getMinY() + 1 : Math.max(world.getMinY() + 1, structure.box().minY() - 2);
            int maxY = structure == null ? world.getMaxY() - 1 : Math.min(world.getMaxY() - 1, structure.box().maxY() + 4);
            refinement = new BiomeDestinationSearch(destination, ctx.playerFeet(), minY, maxY, this::suitable);
        }
        if (refinement != null) {
            if (!refinement.tick()) {
                return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
            }
            candidates = refinement.candidates();
            refinement = null;
            if (candidates.isEmpty()) {
                return finish("No standing or swimming position in the target area near the prediction. "
                        + "Check the seed/preset; older chunks or custom generation may differ.");
            }
            goal = new GoalComposite(candidates.stream().map(GoalBlock::new).toArray(Goal[]::new));
            return new PathingCommand(goal, PathingCommandType.FORCE_REVALIDATE_GOAL_AND_PATH);
        }
        if (candidates != null) {
            List<BlockPos> valid = candidates.stream()
                    .filter(this::suitable).toList();
            if (valid.size() != candidates.size()) {
                candidates = valid;
                if (valid.isEmpty()) {
                    return finish("The destination terrain changed or unloaded; run locate again.");
                }
                goal = new GoalComposite(valid.stream().map(GoalBlock::new).toArray(Goal[]::new));
                return new PathingCommand(goal, PathingCommandType.FORCE_REVALIDATE_GOAL_AND_PATH);
            }
        }
        if (calcFailed) {
            return finish("Unable to path to the located destination. Its coordinates are still in chat.");
        }
        if (goal.isInGoal(ctx.playerFeet()) && isSafeToCancel) {
            if (candidates == null && waitingForChunks++ < 100) {
                return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
            }
            if (candidates == null) {
                return finish("Destination chunks did not load. Try locate again once the terrain is visible.");
            }
            return finish("Reached the destination without entering the target area. Run locate again after checking the seed/preset.");
        }
        return new PathingCommand(goal, PathingCommandType.REVALIDATE_GOAL_AND_PATH);
    }

    private boolean suitable(BlockPos pos) {
        return structure == null ? BiomeDestinationSearch.isSuitable(world, biome, pos)
                : structure.containsArrivalPosition(pos) && BiomeDestinationSearch.isStandingPosition(world, pos);
    }

    private boolean destinationAreaLoaded() {
        int radius = BiomeDestinationSearch.RADIUS;
        for (int x = (destination.getX() - radius) >> 4; x <= (destination.getX() + radius) >> 4; x++) {
            for (int z = (destination.getZ() - radius) >> 4; z <= (destination.getZ() + radius) >> 4; z++) {
                if (!world.hasChunk(x, z)) {
                    return false;
                }
            }
        }
        return true;
    }

    private PathingCommand finish(String message) {
        if (message != null) {
            logger.accept(message);
        }
        onLostControl();
        return new PathingCommand(null, PathingCommandType.CANCEL_AND_SET_GOAL);
    }

    @Override
    public void onLostControl() {
        if (search != null) {
            search.cancel(true);
        }
        search = null;
        world = null;
        biome = null;
        structure = null;
        targetName = null;
        destination = null;
        goal = null;
        refinement = null;
        candidates = null;
        waitingForChunks = 0;
    }

    @Override
    public boolean isTemporary() {
        return false;
    }

    @Override
    public String displayName0() {
        return (search != null ? "Locating " : refinement != null ? "Checking terrain for " : "Travelling to ") + targetName;
    }
}
