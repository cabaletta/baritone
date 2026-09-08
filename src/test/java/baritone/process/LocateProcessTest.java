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

import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.IPlayerContext;
import baritone.utils.locate.BiomeGenerator;
import baritone.utils.locate.BiomeDestinationSearch;
import baritone.utils.locate.StructureTarget;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.client.player.LocalPlayer;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.MockMakers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class LocateProcessTest {
    private static Holder<Biome> plains;
    private static Holder<Biome> desert;
    private static BiomeGenerator generator;
    private IPlayerContext ctx;
    private Level world;
    private LocateProcess process;
    private List<Runnable> work;
    private List<String> messages;

    @BeforeClass
    public static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        BiomeGenerator vanilla = BiomeGenerator.vanilla(Level.OVERWORLD, 0, "default");
        plains = vanilla.source().possibleBiomes().stream().filter(b -> b.is(Biomes.PLAINS)).findFirst().orElseThrow();
        desert = vanilla.source().possibleBiomes().stream().filter(b -> b.is(Biomes.DESERT)).findFirst().orElseThrow();
        generator = new BiomeGenerator(new FixedBiomeSource(desert), vanilla.sampler());
    }

    private static <T> T stub(Class<T> type) {
        // No JVM agent or final-class instrumentation is needed for these collaborators.
        return mock(type, withSettings().mockMaker(MockMakers.SUBCLASS));
    }

    @Before
    public void setup() {
        ctx = stub(IPlayerContext.class);
        world = stub(Level.class);
        when(ctx.world()).thenReturn(world);
        when(ctx.player()).thenReturn(stub(LocalPlayer.class));
        when(ctx.playerFeet()).thenReturn(new BetterBlockPos(0, 1, 0));
        when(world.getMinY()).thenReturn(0);
        when(world.getMaxY()).thenReturn(4);
        when(world.getWorldBorder()).thenReturn(new WorldBorder());
        when(world.getBiome(any(BlockPos.class))).thenReturn(plains);
        when(world.hasChunk(anyInt(), anyInt())).thenReturn(true);
        when(world.getBlockState(any(BlockPos.class))).thenAnswer(call ->
                ((BlockPos) call.getArgument(0)).getY() == 0 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
        work = new ArrayList<>();
        messages = new ArrayList<>();
        process = new LocateProcess(ctx, work::add, messages::add);
    }

    @Test
    public void cancellingStructureSearchDoesNotCreatePredictionContext() {
        AtomicBoolean opened = new AtomicBoolean();
        process.locateStructure(StructureTarget.parse("end city"), () -> {
            opened.set(true);
            throw new AssertionError("Cancelled job created a context");
        });
        process.onLostControl();
        work.getFirst().run();
        assertFalse(opened.get());
        assertFalse(process.isActive());
    }

    @Test
    public void structureSearchReplacesPendingBiomeSearch() {
        AtomicBoolean biomeRan = new AtomicBoolean();
        process.locate(Biomes.DESERT, 32, () -> { biomeRan.set(true); return generator; });
        process.locateStructure(StructureTarget.parse("end city"), () -> { throw new IllegalArgumentException("structure context failure"); });
        work.forEach(Runnable::run);
        assertFalse(biomeRan.get());
        assertNull(process.onTick(false, true).goal);
        assertFalse(process.isActive());
        assertTrue(messages.getLast().contains("structure context failure"));
    }

    @Test
    public void pendingSearchPausesAndCancelDiscardsQueuedWork() {
        AtomicBoolean sampled = new AtomicBoolean();
        process.locate(Biomes.DESERT, 32, () -> { sampled.set(true); return generator; });
        assertEquals(PathingCommandType.REQUEST_PAUSE, process.onTick(false, true).commandType);
        process.onLostControl();
        work.getFirst().run();
        assertFalse(sampled.get());
        assertFalse(process.isActive());
        assertTrue(messages.isEmpty());
    }

    @Test
    public void replacingSearchCannotPublishTheOldResult() {
        AtomicBoolean oldRan = new AtomicBoolean();
        process.locate(Biomes.DESERT, 32, () -> { oldRan.set(true); return generator; });
        process.locate(Biomes.DESERT, 32, () -> { throw new IllegalArgumentException("replacement"); });
        work.forEach(Runnable::run);
        assertFalse(oldRan.get());
        assertEquals(PathingCommandType.CANCEL_AND_SET_GOAL, process.onTick(false, true).commandType);
        assertFalse(process.isActive());
        assertTrue(messages.getLast().contains("replacement"));
    }

    @Test
    public void changedWorldDiscardsCompletedSearch() {
        process.locate(Biomes.DESERT, 32, () -> generator);
        work.getFirst().run();
        when(ctx.world()).thenReturn(stub(Level.class));
        assertNull(process.onTick(false, true).goal);
        assertFalse(process.isActive());
        assertTrue(messages.isEmpty());
    }

    @Test
    public void arrivalRequiresActualBiomeAndSafeCancellation() {
        process.locate(Biomes.DESERT, 32, () -> generator);
        when(world.getBiome(any(BlockPos.class))).thenReturn(desert);
        assertEquals(PathingCommandType.REQUEST_PAUSE, process.onTick(false, false).commandType);
        assertTrue(process.isActive());
        assertNull(process.onTick(false, true).goal);
        assertFalse(process.isActive());
        assertTrue(messages.getLast().startsWith("Arrived"));
    }

    @Test
    public void choosesActualStandingSpaceAndRevalidatesChangedTerrain() {
        BlockPos target = new BlockPos(10, 1, 0);
        when(world.getBiome(any(BlockPos.class))).thenAnswer(call -> target.equals(call.getArgument(0)) ? desert : plains);
        process.locate(Biomes.DESERT, 32, () -> generator);
        work.getFirst().run();
        PathingCommand result = finishRefinement();
        assertNotNull(result.goal);
        assertTrue(result.goal.isInGoal(target));
        assertFalse(result.goal.isInGoal(0, 1, 0));
        when(world.getBlockState(target)).thenReturn(Blocks.STONE.defaultBlockState());
        assertNull(process.onTick(false, true).goal);
        assertFalse(process.isActive());
        assertTrue(messages.getLast().contains("terrain changed"));
    }

    @Test
    public void wrongPredictionDoesNotSendPlayerIntoSolidTerrain() {
        process.locate(Biomes.DESERT, 32, () -> generator);
        work.getFirst().run();
        assertNull(finishRefinement().goal);
        assertFalse(process.isActive());
        assertTrue(messages.getLast().contains("No standing or swimming position"));
    }

    @Test
    public void cancellationInterruptsRunningSearch() throws InterruptedException {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch interrupted = new CountDownLatch(1);
        try (var executor = Executors.newSingleThreadExecutor()) {
            process = new LocateProcess(ctx, executor, messages::add);
            process.locate(Biomes.DESERT, 32, () -> {
                started.countDown();
                try {
                    new CountDownLatch(1).await();
                } catch (InterruptedException e) {
                    interrupted.countDown();
                    Thread.currentThread().interrupt();
                }
                return generator;
            });
            try {
                assertTrue(started.await(5, TimeUnit.SECONDS));
            } finally {
                process.onLostControl();
            }
            assertTrue(interrupted.await(5, TimeUnit.SECONDS));
            assertFalse(process.isActive());
            assertTrue(messages.isEmpty());
        }
    }

    @Test
    public void waitsForDestinationChunksWithoutReadingUnloadedTerrain() {
        when(world.hasChunk(anyInt(), anyInt())).thenReturn(false);
        process.locate(Biomes.DESERT, 32, () -> generator);
        work.getFirst().run();
        assertEquals(PathingCommandType.REQUEST_PAUSE, process.onTick(false, true).commandType);
        assertTrue(process.isActive());
        verify(world, never()).getBlockState(any());
        for (int tick = 0; tick < 100; tick++) {
            process.onTick(false, true);
        }
        assertFalse(process.isActive());
        assertTrue(messages.getLast().contains("did not load"));
    }

    @Test
    public void standingCheckRejectsUnloadedChunksWithoutReadingThem() {
        when(world.hasChunk(anyInt(), anyInt())).thenReturn(false);
        assertFalse(BiomeDestinationSearch.isSuitable(world, Biomes.DESERT, new BlockPos(0, 1, 0)));
        verify(world, never()).getBlockState(any());
        verify(world, never()).getBiome(any());
    }

    @Test
    public void standingCheckRejectsHazardsAndAcceptsBreathingWaterSurface() {
        BlockPos pos = new BlockPos(0, 1, 0);
        when(world.getBiome(pos)).thenReturn(desert);
        assertTrue(BiomeDestinationSearch.isSuitable(world, Biomes.DESERT, pos));
        when(world.getBlockState(pos.below())).thenReturn(Blocks.MAGMA_BLOCK.defaultBlockState());
        assertFalse(BiomeDestinationSearch.isSuitable(world, Biomes.DESERT, pos));
        when(world.getBlockState(pos)).thenReturn(Blocks.WATER.defaultBlockState());
        assertTrue(BiomeDestinationSearch.isSuitable(world, Biomes.DESERT, pos));
        when(world.getBlockState(pos.above())).thenReturn(Blocks.WATER.defaultBlockState());
        assertFalse(BiomeDestinationSearch.isSuitable(world, Biomes.DESERT, pos));
        when(world.getBlockState(pos.above())).thenReturn(Blocks.AIR.defaultBlockState());
        when(world.getBlockState(pos)).thenReturn(Blocks.LAVA.defaultBlockState());
        assertFalse(BiomeDestinationSearch.isSuitable(world, Biomes.DESERT, pos));
    }

    @Test
    public void rejectedWorkerDoesNotLeaveActiveProcess() {
        process = new LocateProcess(ctx, task -> { throw new RejectedExecutionException(); }, messages::add);
        assertThrows(RejectedExecutionException.class, () -> process.locate(Biomes.DESERT, 32, () -> generator));
        assertFalse(process.isActive());
    }

    private PathingCommand finishRefinement() {
        for (int i = 0; i < 10; i++) {
            PathingCommand result = process.onTick(false, true);
            if (result.commandType != PathingCommandType.REQUEST_PAUSE) {
                return result;
            }
        }
        throw new AssertionError("Refinement did not finish");
    }
}
