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

package baritone.api.utils;

import baritone.api.utils.accessor.IItemStack;
import com.google.common.collect.ImmutableSet;
import io.netty.util.concurrent.ThreadPerTaskExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.server.packs.*;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Unit;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootDataManager;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import sun.misc.Unsafe;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BlockOptionalMeta {

    private final Block block;
    private final Set<BlockState> blockstates;
    private final ImmutableSet<Integer> stateHashes;
    private final ImmutableSet<Integer> stackHashes;
    private static final Pattern pattern = Pattern.compile("^(.+?)(?::(\\d+))?$");
    private static LootDataManager lootTables;
    private static Map<Block, List<Item>> drops = new HashMap<>();

    public BlockOptionalMeta(@Nonnull Block block) {
        this.block = block;
        this.blockstates = getStates(block);
        this.stateHashes = getStateHashes(blockstates);
        this.stackHashes = getStackHashes(blockstates);
    }

    public BlockOptionalMeta(@Nonnull String selector) {
        Matcher matcher = pattern.matcher(selector);

        if (!matcher.find()) {
            throw new IllegalArgumentException("invalid block selector");
        }

        MatchResult matchResult = matcher.toMatchResult();

        block = BlockUtils.stringToBlockRequired(matchResult.group(1));
        blockstates = getStates(block);
        stateHashes = getStateHashes(blockstates);
        stackHashes = getStackHashes(blockstates);
    }

    private static Set<BlockState> getStates(@Nonnull Block block) {
        return new HashSet<>(block.getStateDefinition().getPossibleStates());
    }

    private static ImmutableSet<Integer> getStateHashes(Set<BlockState> blockstates) {
        return ImmutableSet.copyOf(
                blockstates.stream()
                        .map(BlockState::hashCode)
                        .toArray(Integer[]::new)
        );
    }

    private static ImmutableSet<Integer> getStackHashes(Set<BlockState> blockstates) {
        //noinspection ConstantConditions
        return ImmutableSet.copyOf(
                blockstates.stream()
                        .flatMap(state -> drops(state.getBlock())
                                .stream()
                                .map(item -> new ItemStack(item, 1))
                        )
                        .map(stack -> ((IItemStack) (Object) stack).getBaritoneHash())
                        .toArray(Integer[]::new)
        );
    }

    public Block getBlock() {
        return block;
    }

    public boolean matches(@Nonnull Block block) {
        return block == this.block;
    }

    public boolean matches(@Nonnull BlockState blockstate) {
        Block block = blockstate.getBlock();
        return block == this.block && stateHashes.contains(blockstate.hashCode());
    }

    public boolean matches(ItemStack stack) {
        //noinspection ConstantConditions
        int hash = ((IItemStack) (Object) stack).getBaritoneHash();

        hash -= stack.getDamageValue();

        return stackHashes.contains(hash);
    }

    @Override
    public String toString() {
        return String.format("BlockOptionalMeta{block=%s}", block);
    }

    public BlockState getAnyBlockState() {
        if (blockstates.size() > 0) {
            return blockstates.iterator().next();
        }

        return null;
    }

    private static Method getVanillaServerPack;

    private static VanillaPackResources getVanillaServerPack() {
        if (getVanillaServerPack == null) {
            getVanillaServerPack = Arrays.stream(ServerPacksSource.class.getDeclaredMethods()).filter(field -> field.getReturnType() == VanillaPackResources.class).findFirst().orElseThrow();
            getVanillaServerPack.setAccessible(true);
        }

        try {
            return (VanillaPackResources) getVanillaServerPack.invoke(null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static LootDataManager getManager() {
        if (lootTables == null) {
            MultiPackResourceManager resources = new MultiPackResourceManager(PackType.SERVER_DATA, List.of(getVanillaServerPack()));
            ReloadableResourceManager resourceManager = new ReloadableResourceManager(PackType.SERVER_DATA);
            lootTables = new LootDataManager();
            resourceManager.registerReloadListener(lootTables);
            try {
                resourceManager.createReload(new ThreadPerTaskExecutor(Thread::new), new ThreadPerTaskExecutor(Thread::new), CompletableFuture.completedFuture(Unit.INSTANCE), resources.listPacks().toList()).done().get();
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }

        }
        return lootTables;
    }

    private static synchronized List<Item> drops(Block b) {
        return drops.computeIfAbsent(b, block -> {
            ResourceLocation lootTableLocation = block.getLootTable();
            if (lootTableLocation == BuiltInLootTables.EMPTY) {
                return Collections.emptyList();
            } else {
                List<Item> items = new ArrayList<>();
                try {

                    getManager().getLootTable(lootTableLocation).getRandomItemsRaw(
                        new LootContext.Builder(
                                new LootParams.Builder(ServerLevelStub.fastCreate())
                                    .withParameter(LootContextParams.ORIGIN, Vec3.atLowerCornerOf(BlockPos.ZERO))
                                    .withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
                                    .withOptionalParameter(LootContextParams.BLOCK_ENTITY, null)
                                    .withParameter(LootContextParams.BLOCK_STATE, block.defaultBlockState())
                                    .create(LootContextParamSets.BLOCK)
                            ).withOptionalRandomSeed(1L)
                            .create(null),
                        stack -> items.add(stack.getItem())
                    );
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return items;
            }
        });
    }

    private static class ServerLevelStub extends ServerLevel {
        private static Minecraft client = Minecraft.getInstance();
        private static Unsafe unsafe = getUnsafe();
        public ServerLevelStub(MinecraftServer $$0, Executor $$1, LevelStorageSource.LevelStorageAccess $$2, ServerLevelData $$3, ResourceKey<Level> $$4, LevelStem $$5, ChunkProgressListener $$6, boolean $$7, long $$8, List<CustomSpawner> $$9, boolean $$10) {
            super($$0, $$1, $$2, $$3, $$4, $$5, $$6, $$7, $$8, $$9, $$10);
        }

        @Override
        public FeatureFlagSet enabledFeatures() {
            assert client.level != null;
            return client.level.enabledFeatures();
        }

        public static ServerLevelStub fastCreate() {
            try {
                return (ServerLevelStub) unsafe.allocateInstance(ServerLevelStub.class);
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            }
        }

        public static Unsafe getUnsafe() {
            try {
                Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
                theUnsafe.setAccessible(true);
                return (Unsafe) theUnsafe.get(null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }
}
