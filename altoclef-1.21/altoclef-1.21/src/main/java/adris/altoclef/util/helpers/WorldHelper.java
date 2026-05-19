package adris.altoclef.util.helpers;

import adris.altoclef.AltoClef;
import adris.altoclef.mixins.ClientConnectionAccessor;
import adris.altoclef.util.Dimension;
import baritone.api.BaritoneAPI;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.MovementHelper;
import baritone.process.MineProcess;
import baritone.utils.BlockStateInterface;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.network.ClientConnection;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.dimension.PortalManager;

import java.util.*;

/**
 * Super useful helper functions for getting information about the world.
 */
public interface WorldHelper {

    // God bless 1.18
    int WORLD_CEILING_Y = 255;
    int WORLD_FLOOR_Y = -64;

    /**
     * Get the number of in-game ticks the game/world has been active for.
     */
    static int getTicks() {
        ClientConnection con = Objects.requireNonNull(MinecraftClient.getInstance().getNetworkHandler()).getConnection();
        return ((ClientConnectionAccessor) con).getTicks();
    }

    static Vec3d toVec3d(BlockPos pos) {
        if (pos == null) return null;
        return new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    static Vec3d toVec3d(Vec3i pos) {
        return new Vec3d(pos.getX(), pos.getY(), pos.getZ());
    }

    static Vec3i toVec3i(Vec3d pos) {
        return new Vec3i((int) pos.getX(), (int) pos.getY(), (int) pos.getZ());
    }

    static BlockPos toBlockPos(Vec3d pos) {
        return new BlockPos((int) pos.getX(), (int) pos.getY(), (int) pos.getZ());
    }

    static boolean isSourceBlock(AltoClef mod, BlockPos pos, boolean onlyAcceptStill) {
        BlockState s = mod.getWorld().getBlockState(pos);
        if (s.getBlock() instanceof FluidBlock) {
            // Only accept still fluids.
            if (!s.getFluidState().isStill() && onlyAcceptStill) return false;
            int level = s.getFluidState().getLevel();
            // Ignore if there's liquid above, we can't tell if it's a source block or not.
            BlockState above = mod.getWorld().getBlockState(pos.up());
            if (above.getBlock() instanceof FluidBlock) return false;
            return level == 8;
        }
        return false;
    }

    static double distanceXZSquared(Vec3d from, Vec3d to) {
        Vec3d delta = to.subtract(from);
        return (delta.x * delta.x) + (delta.z * delta.z);
    }

    static double distanceXZ(Vec3d from, Vec3d to) {
        return Math.sqrt(distanceXZSquared(from, to));
    }

    static boolean inRangeXZ(Vec3d from, Vec3d to, double range) {
        return distanceXZSquared(from, to) < range * range;
    }

    static boolean inRangeXZ(BlockPos from, BlockPos to, double range) {
        return inRangeXZ(toVec3d(from), toVec3d(to), range);
    }

    static boolean inRangeXZ(Entity entity, Vec3d to, double range) {
        return inRangeXZ(entity.getPos(), to, range);
    }

    static boolean inRangeXZ(Entity entity, BlockPos to, double range) {
        return inRangeXZ(entity, toVec3d(to), range);
    }

    static boolean inRangeXZ(Entity entity, Entity to, double range) {
        return inRangeXZ(entity, to.getPos(), range);
    }

    static Dimension getCurrentDimension() {
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world == null) return Dimension.OVERWORLD;
        if (world.getDimension().ultrawarm()) return Dimension.NETHER;
        if (world.getDimension().natural()) return Dimension.OVERWORLD;
        return Dimension.END;
    }


    static boolean isSolid(AltoClef mod, BlockPos pos) {
        return mod.getWorld().getBlockState(pos).isSolidBlock(mod.getWorld(), pos);
    }

    /**
     * Get the "head" of a block with a bed, if the block is a bed.
     */
    static BlockPos getBedHead(AltoClef mod, BlockPos posWithBed) {
        BlockState state = mod.getWorld().getBlockState(posWithBed);
        if (state.getBlock() instanceof BedBlock) {
            Direction facing = state.get(BedBlock.FACING);
            if (mod.getWorld().getBlockState(posWithBed).get(BedBlock.PART).equals(BedPart.HEAD)) {
                return posWithBed;
            }
            return posWithBed.offset(facing);
        }
        return null;
    }

    /**
     * Get the "foot" of a block with a bed, if the block is a bed.
     */
    static BlockPos getBedFoot(AltoClef mod, BlockPos posWithBed) {
        BlockState state = mod.getWorld().getBlockState(posWithBed);
        if (state.getBlock() instanceof BedBlock) {
            Direction facing = state.get(BedBlock.FACING);
            if (mod.getWorld().getBlockState(posWithBed).get(BedBlock.PART).equals(BedPart.FOOT)) {
                return posWithBed;
            }
            return posWithBed.offset(facing.getOpposite());
        }
        return null;
    }

    // Get the left side of a chest, given a block pos.
    // Used to consistently identify whether a double chest is part of the same chest.
    static BlockPos getChestLeft(AltoClef mod, BlockPos posWithChest) {
        BlockState state = mod.getWorld().getBlockState(posWithChest);
        if (state.getBlock() instanceof ChestBlock) {
            ChestType type = state.get(ChestBlock.CHEST_TYPE);
            if (type == ChestType.SINGLE || type == ChestType.LEFT) {
                return posWithChest;
            }
            Direction facing = state.get(ChestBlock.FACING);
            return posWithChest.offset(facing.rotateYCounterclockwise());
        }
        return null;
    }

    static boolean isChestBig(AltoClef mod, BlockPos posWithChest) {
        BlockState state = mod.getWorld().getBlockState(posWithChest);
        if (state.getBlock() instanceof ChestBlock) {
            ChestType type = state.get(ChestBlock.CHEST_TYPE);
            return (type == ChestType.RIGHT || type == ChestType.LEFT);
        }
        return false;
    }

    static int getGroundHeight(AltoClef mod, int x, int z) {
        for (int y = WORLD_CEILING_Y; y >= WORLD_FLOOR_Y; --y) {
            BlockPos check = new BlockPos(x, y, z);
            if (isSolid(mod, check)) return y;
        }
        return -1;
    }

    static BlockPos getADesertTemple(AltoClef mod) {
        if (mod.getBlockTracker().isTracking(Blocks.STONE_PRESSURE_PLATE)) {
            Optional<BlockPos> stonePressurePlates = mod.getBlockTracker().getNearestTracking(Blocks.STONE_PRESSURE_PLATE);
            if (stonePressurePlates.isPresent() && mod.getWorld().getBlockState(stonePressurePlates.get()).getBlock() == Blocks.STONE_PRESSURE_PLATE && // Duct tape
                    mod.getWorld().getBlockState(stonePressurePlates.get().down()).getBlock() == Blocks.CUT_SANDSTONE &&
                    mod.getWorld().getBlockState(stonePressurePlates.get().down(2)).getBlock() == Blocks.TNT) {
                return stonePressurePlates.get();
            }
        }
        return null;
    }

    static boolean isUnopenedChest(AltoClef mod, BlockPos pos) {
        return mod.getItemStorage().getContainerAtPosition(pos).isEmpty();
    }

    static int getGroundHeight(AltoClef mod, int x, int z, Block... groundBlocks) {
        Set<Block> possibleBlocks = new HashSet<>(Arrays.asList(groundBlocks));
        for (int y = WORLD_CEILING_Y; y >= WORLD_FLOOR_Y; --y) {
            BlockPos check = new BlockPos(x, y, z);
            if (possibleBlocks.contains(mod.getWorld().getBlockState(check).getBlock())) return y;

        }
        return -1;
    }

    static boolean canBreak(AltoClef mod, BlockPos pos) {
        // JANK: Temporarily check if we can break WITHOUT paused interactions.
        // Not doing this creates bugs where we loop back and forth through the nether portal and stuff.
        boolean prevInteractionPaused = mod.getExtraBaritoneSettings().isInteractionPaused();
        mod.getExtraBaritoneSettings().setInteractionPaused(false);
        boolean result = mod.getWorld().getBlockState(pos).getHardness(mod.getWorld(), pos) >= 0
                && !mod.getExtraBaritoneSettings().shouldAvoidBreaking(pos)
                && MineProcess.plausibleToBreak(new CalculationContext(mod.getClientBaritone()), pos)
                && canReach(mod, pos) && !mod.getBlockTracker().unreachable(pos);
        mod.getExtraBaritoneSettings().setInteractionPaused(prevInteractionPaused);
        return result;
    }

    static boolean isInNetherPortal(AltoClef mod) {
        if (mod.getPlayer() == null)
            return false;
        PortalManager portalManager = mod.getPlayer().portalManager;
        if (portalManager == null)
            return false;
        return portalManager.isInPortal();
    }

    static boolean dangerousToBreakIfRightAbove(AltoClef mod, BlockPos toBreak) {
        // There might be mumbo jumbo next to it, we fall and we get killed by lava or something.
        if (MovementHelper.avoidBreaking(mod.getClientBaritone().bsi, toBreak.getX(), toBreak.getY(), toBreak.getZ(), mod.getWorld().getBlockState(toBreak))) {
            return true;
        }
        // Fall down
        for (int dy = 1; dy <= toBreak.getY() - WORLD_FLOOR_Y; ++dy) {
            BlockPos check = toBreak.down(dy);
            BlockState s = mod.getWorld().getBlockState(check);
            boolean tooFarToFall = dy > mod.getClientBaritoneSettings().maxFallHeightNoWater.value;
            // Don't fall in lava
            if (MovementHelper.isLava(s))
                return true;
            // Always fall in water
            // TODO: If there's a 1 meter thick layer of water and then a massive drop below, the bot will think it is safe.
            if (MovementHelper.isWater(s))
                return true;
            // We hit ground, depends
            if (WorldHelper.isSolid(mod, check)) {
                return tooFarToFall;
            }
        }
        // At this point we probably fall through the void, so not safe.
        return true;
    }

    static boolean canPlace(AltoClef mod, BlockPos pos) {
        return !mod.getExtraBaritoneSettings().shouldAvoidPlacingAt(pos)
                && canReach(mod, pos);
    }

    static boolean canReach(AltoClef mod, BlockPos pos) {
        if (mod.getModSettings().shouldAvoidOcean()) {
            // 45 is roughly the ocean floor. We add 2 just cause why not.
            // This > 47 can clearly cause a stuck bug.
            if (mod.getPlayer().getY() > 47 && mod.getChunkTracker().isChunkLoaded(pos) && isOcean(mod.getWorld().getBiome(pos))) { // But if we stuck, add more oceans
                // Block is in an ocean biome. If it's below sea level...
                if (pos.getY() < 64 && getGroundHeight(mod, pos.getX(), pos.getZ(), Blocks.WATER) > pos.getY()) {
                    return false;
                }
            }
        }
        return !mod.getBlockTracker().unreachable(pos);
    }

    static boolean isOcean(RegistryEntry<Biome> b) {
        return (b.matchesKey(BiomeKeys.OCEAN)
                || b.matchesKey(BiomeKeys.COLD_OCEAN)
                || b.matchesKey(BiomeKeys.DEEP_COLD_OCEAN)
                || b.matchesKey(BiomeKeys.DEEP_OCEAN)
                || b.matchesKey(BiomeKeys.DEEP_FROZEN_OCEAN)
                || b.matchesKey(BiomeKeys.DEEP_LUKEWARM_OCEAN)
                || b.matchesKey(BiomeKeys.LUKEWARM_OCEAN)
                || b.matchesKey(BiomeKeys.WARM_OCEAN)
                || b.matchesKey(BiomeKeys.FROZEN_OCEAN));
    }

    static boolean isAir(AltoClef mod, BlockPos pos) {
        return mod.getBlockTracker().blockIsValid(pos, Blocks.AIR, Blocks.CAVE_AIR, Blocks.VOID_AIR);
        //return state.isAir() || isAir(state.getBlock());
    }

    static boolean isAir(Block block) {
        return block == Blocks.AIR || block == Blocks.CAVE_AIR || block == Blocks.VOID_AIR;
    }

    static boolean isInteractableBlock(AltoClef mod, BlockPos pos) {
        Block block = mod.getWorld().getBlockState(pos).getBlock();
        return (block instanceof ChestBlock
                || block instanceof EnderChestBlock
                || block instanceof CraftingTableBlock
                || block instanceof AbstractFurnaceBlock
                || block instanceof LoomBlock
                || block instanceof CartographyTableBlock
                || block instanceof EnchantingTableBlock
                || block instanceof RedstoneOreBlock
                || block instanceof BarrelBlock
        );
    }

    static boolean isInsidePlayer(AltoClef mod, BlockPos pos) {
        return pos.isWithinDistance(mod.getPlayer().getPos(), 2);
    }

    static Iterable<BlockPos> getBlocksTouchingPlayer(AltoClef mod) {
        return getBlocksTouchingBox(mod, mod.getPlayer().getBoundingBox());
    }

    static Iterable<BlockPos> getBlocksTouchingBox(AltoClef mod, Box box) {
        BlockPos min = new BlockPos((int) box.minX, (int) box.minY, (int) box.minZ);
        BlockPos max = new BlockPos((int) box.maxX, (int) box.maxY, (int) box.maxZ);
        return scanRegion(mod, min, max);
    }

    static Iterable<BlockPos> scanRegion(AltoClef mod, BlockPos start, BlockPos end) {
        return () -> new Iterator<>() {
            int x = start.getX(), y = start.getY(), z = start.getZ();

            @Override
            public boolean hasNext() {
                return y <= end.getY() && z <= end.getZ() && x <= end.getX();
            }

            @Override
            public BlockPos next() {
                BlockPos result = new BlockPos(x, y, z);
                ++x;
                if (x > end.getX()) {
                    x = start.getX();
                    ++z;
                    if (z > end.getZ()) {
                        z = start.getZ();
                        ++y;
                    }
                }
                return result;
            }
        };
    }

    static boolean fallingBlockSafeToBreak(BlockPos pos) {
        BlockStateInterface bsi = new BlockStateInterface(BaritoneAPI.getProvider().getPrimaryBaritone().getPlayerContext());
        World w = MinecraftClient.getInstance().world;
        assert w != null;
        while (isFallingBlock(pos)) {
            if (MovementHelper.avoidBreaking(bsi, pos.getX(), pos.getY(), pos.getZ(), w.getBlockState(pos)))
                return false;
            pos = pos.up();
        }
        return true;
    }

    static boolean isFallingBlock(BlockPos pos) {
        World w = MinecraftClient.getInstance().world;
        assert w != null;
        return w.getBlockState(pos).getBlock() instanceof FallingBlock;
    }

    static Entity getSpawnerEntity(AltoClef mod, BlockPos pos) {
        BlockState state = mod.getWorld().getBlockState(pos);
        if (state.getBlock() instanceof SpawnerBlock) {
            BlockEntity be = mod.getWorld().getBlockEntity(pos);
            if (be instanceof MobSpawnerBlockEntity blockEntity) {
                return blockEntity.getLogic().getRenderedEntity(mod.getWorld(), pos);
            }
        }
        return null;
    }

    static Vec3d getOverworldPosition(Vec3d pos) {
        if (getCurrentDimension() == Dimension.NETHER) {
            pos = pos.multiply(8.0, 1, 8.0);
        }
        return pos;
    }

    static BlockPos getOverworldPosition(BlockPos pos) {
        if (getCurrentDimension() == Dimension.NETHER) {
            pos = new BlockPos(pos.getX() * 8, pos.getY(), pos.getZ() * 8);
        }
        return pos;
    }

    static boolean isChest(AltoClef mod, BlockPos block) {
        Block b = mod.getWorld().getBlockState(block).getBlock();
        return isChest(b);
    }

    static boolean isChest(Block b) {
        return b instanceof ChestBlock || b instanceof EnderChestBlock;
    }

    static boolean isBlock(AltoClef mod, BlockPos pos, Block block) {
        return mod.getWorld().getBlockState(pos).getBlock() == block;
    }

    static boolean canSleep() {
        int time = 0;
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world != null) {
            // You can sleep during thunderstorms
            if (world.isThundering() && world.isRaining())
                return true;
            time = (int) (world.getTimeOfDay() % 24000);
        }
        // https://minecraft.fandom.com/wiki/Daylight_cycle
        return 12542 <= time && time <= 23992;
    }
}
