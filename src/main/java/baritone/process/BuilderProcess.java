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
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.pathing.goals.GoalComposite;
import baritone.api.pathing.goals.GoalGetToBlock;
import baritone.api.process.IBuilderProcess;
import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;
import baritone.api.schematic.FillSchematic;
import baritone.api.schematic.ISchematic;
import baritone.api.schematic.IStaticSchematic;
import baritone.api.schematic.MaskSchematic;
import baritone.api.schematic.SubstituteSchematic;
import baritone.api.schematic.RotatedSchematic;
import baritone.api.schematic.MirroredSchematic;
import baritone.api.schematic.format.ISchematicFormat;
import baritone.api.utils.*;
import baritone.api.utils.input.Input;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.Movement;
import baritone.pathing.movement.MovementHelper;
import baritone.utils.BaritoneProcessHelper;
import baritone.utils.BlockStateInterface;
import baritone.utils.PathingCommandContext;
import baritone.utils.schematic.MapArtSchematic;
import baritone.utils.schematic.SelectionSchematic;
import baritone.utils.schematic.SchematicSystem;
import baritone.utils.schematic.litematica.LitematicaHelper;
import baritone.utils.schematic.schematica.SchematicaHelper;
import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static baritone.api.pathing.movement.ActionCosts.COST_INF;

public final class BuilderProcess extends BaritoneProcessHelper implements IBuilderProcess {

    private static final Set<Property<?>> ORIENTATION_PROPS =
            ImmutableSet.of(
                    RotatedPillarBlock.AXIS, HorizontalDirectionalBlock.FACING,
                    StairBlock.FACING, StairBlock.HALF, StairBlock.SHAPE,
                    PipeBlock.NORTH, PipeBlock.EAST, PipeBlock.SOUTH, PipeBlock.WEST, PipeBlock.UP,
                    TrapDoorBlock.OPEN, TrapDoorBlock.HALF
            );

    /**
     * Properties whose value is decided by the surrounding world, not by how the player places the block:
     * stair shape, double-chest type, fence/wall/pipe connections, waterlogging and the like. A freshly
     * placed block gets whatever value the neighbours imply, and the value updates again when those
     * neighbours change. Comparing them for equality would make a correctly placed block look wrong
     * forever (so the builder would break and replace it in an endless loop), and would make the
     * placement search unable to find any acceptable face. They are therefore always ignored.
     * Controllable properties (facing, axis, slab top/bottom half) are deliberately NOT here.
     */
    private static final Set<Property<?>> WORLD_DETERMINED_PROPS =
            ImmutableSet.of(
                    BlockStateProperties.WATERLOGGED,
                    BlockStateProperties.STAIRS_SHAPE,
                    BlockStateProperties.CHEST_TYPE,
                    BlockStateProperties.NORTH, BlockStateProperties.EAST,
                    BlockStateProperties.SOUTH, BlockStateProperties.WEST,
                    BlockStateProperties.UP,
                    BlockStateProperties.NORTH_WALL, BlockStateProperties.EAST_WALL,
                    BlockStateProperties.SOUTH_WALL, BlockStateProperties.WEST_WALL,
                    BlockStateProperties.NORTH_REDSTONE, BlockStateProperties.EAST_REDSTONE,
                    BlockStateProperties.SOUTH_REDSTONE, BlockStateProperties.WEST_REDSTONE
            );

    private HashSet<BetterBlockPos> incorrectPositions;
    private LongOpenHashSet observedCompleted; // positions that are completed even if they're out of render distance and we can't make sure right now
    private String name;
    private ISchematic realSchematic;
    private ISchematic schematic;
    private Vec3i origin;
    private int ticks;
    private boolean paused;
    private int layer;
    private int numRepeats;
    private List<BlockState> approxPlaceable;
    public int stopAtHeight = 0;

    // creative mode auto /give bookkeeping
    private final Map<Block, Integer> creativeGiveAttempts = new HashMap<>();
    private final Set<Block> creativeUnGiveable = new HashSet<>();
    private int creativeGiveCooldown = 0;

    // counts ticks where we have work to do but cannot place anything and are not pathing (see unstickGoal)
    private int stuckTicks = 0;
    // stand positions we already tried this stuck episode, so we never ping-pong between two of them
    private final Set<BlockPos> triedNudges = new HashSet<>();

    // --- build tracing (diagnostics) ---
    // a monotonically increasing tick counter and bookkeeping to detect "standing still for too long"
    private long traceTick = 0;
    private long lastPlaceTraceTick = -1000;
    private String lastPlaceTraceKey = "";
    // how many ticks we have been trying (aiming at) the same placement without managing to click it
    private BlockPos placeAttemptPos;
    private int placeAttemptTicks;
    private BetterBlockPos traceLastFeet;
    private long traceLastFeetChange = 0;
    private long lastStallLogTick = -1000;

    public BuilderProcess(Baritone baritone) {
        super(baritone);
    }

    @Override
    public void build(String name, ISchematic schematic, Vec3i origin) {
        this.name = name;
        this.schematic = schematic;
        this.realSchematic = null;
        boolean buildingSelectionSchematic = schematic instanceof SelectionSchematic;
        if (!Baritone.settings().buildSubstitutes.value.isEmpty()) {
            this.schematic = new SubstituteSchematic(this.schematic, Baritone.settings().buildSubstitutes.value);
        }
        if (Baritone.settings().buildSchematicMirror.value != net.minecraft.world.level.block.Mirror.NONE) {
            this.schematic = new MirroredSchematic(this.schematic, Baritone.settings().buildSchematicMirror.value);
        }
        if (Baritone.settings().buildSchematicRotation.value != net.minecraft.world.level.block.Rotation.NONE) {
            this.schematic = new RotatedSchematic(this.schematic, Baritone.settings().buildSchematicRotation.value);
        }
        // TODO this preserves the old behavior, but maybe we should bake the setting value right here
        this.schematic = new MaskSchematic(this.schematic) {
            @Override
            public boolean partOfMask(int x, int y, int z, BlockState current) {
                // partOfMask is only called inside the schematic so desiredState is not null
                return !Baritone.settings().buildSkipBlocks.value.contains(this.desiredState(x, y, z, current, Collections.emptyList()).getBlock());
            }
        };
        int x = origin.getX();
        int y = origin.getY();
        int z = origin.getZ();
        if (Baritone.settings().schematicOrientationX.value) {
            x += schematic.widthX();
        }
        if (Baritone.settings().schematicOrientationY.value) {
            y += schematic.heightY();
        }
        if (Baritone.settings().schematicOrientationZ.value) {
            z += schematic.lengthZ();
        }
        this.origin = new Vec3i(x, y, z);
        this.paused = false;
        this.layer = Baritone.settings().startAtLayer.value;
        this.stopAtHeight = schematic.heightY();
        if (Baritone.settings().buildOnlySelection.value && buildingSelectionSchematic) {  // currently redundant but safer maybe
            if (baritone.getSelectionManager().getSelections().length == 0) {
                logDirect("Poor little kitten forgot to set a selection while BuildOnlySelection is true");
                this.stopAtHeight = 0;
            } else if (Baritone.settings().buildInLayers.value) {
                OptionalInt minim = Stream.of(baritone.getSelectionManager().getSelections()).mapToInt(sel -> sel.min().y).min();
                OptionalInt maxim = Stream.of(baritone.getSelectionManager().getSelections()).mapToInt(sel -> sel.max().y).max();
                if (minim.isPresent() && maxim.isPresent()) {
                    int startAtHeight = Baritone.settings().layerOrder.value ? y + schematic.heightY() - maxim.getAsInt() : minim.getAsInt() - y;
                    this.stopAtHeight = (Baritone.settings().layerOrder.value ? y + schematic.heightY() - minim.getAsInt() : maxim.getAsInt() - y) + 1;
                    this.layer = Math.max(this.layer, startAtHeight / Baritone.settings().layerHeight.value);  // startAtLayer or startAtHeight, whichever is highest
                    logDebug(String.format("Schematic starts at y=%s with height %s", y, schematic.heightY()));
                    logDebug(String.format("Selection starts at y=%s and ends at y=%s", minim.getAsInt(), maxim.getAsInt()));
                    logDebug(String.format("Considering relevant height %s - %s", startAtHeight, this.stopAtHeight));
                }
            }
        }

        this.numRepeats = 0;
        this.observedCompleted = new LongOpenHashSet();
        this.incorrectPositions = null;
        this.creativeGiveAttempts.clear();
        this.creativeUnGiveable.clear();
        this.creativeGiveCooldown = 0;
        this.stuckTicks = 0;
        this.triedNudges.clear();
    }

    public void resume() {
        paused = false;
    }

    public void pause() {
        paused = true;
    }

    @Override
    public boolean isPaused() {
        return paused;
    }

    @Override
    public boolean build(String name, File schematic, Vec3i origin) {
        Optional<ISchematicFormat> format = SchematicSystem.INSTANCE.getByFile(schematic);
        if (!format.isPresent()) {
            return false;
        }
        IStaticSchematic parsed;
        try {
            parsed = format.get().parse(new FileInputStream(schematic));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        ISchematic schem = applyMapArtAndSelection(origin, parsed);
        build(name, schem, origin);
        return true;
    }

    private ISchematic applyMapArtAndSelection(Vec3i origin, IStaticSchematic parsed) {
        ISchematic schematic = parsed;
        if (Baritone.settings().mapArtMode.value) {
            schematic = new MapArtSchematic(parsed);
        }
        if (Baritone.settings().buildOnlySelection.value) {
            schematic = new SelectionSchematic(schematic, origin, baritone.getSelectionManager().getSelections());
        }
        return schematic;
    }

    @Override
    public void buildOpenSchematic() {
        if (SchematicaHelper.isSchematicaPresent()) {
            Optional<Tuple<IStaticSchematic, BlockPos>> schematic = SchematicaHelper.getOpenSchematic();
            if (schematic.isPresent()) {
                IStaticSchematic raw = schematic.get().getA();
                BlockPos origin = schematic.get().getB();
                ISchematic schem = applyMapArtAndSelection(origin, raw);
                this.build(raw.toString(), schem, origin);
            } else {
                logDirect("No schematic currently open");
            }
        } else {
            logDirect("Schematica is not present");
        }
    }

    @Override
    public void buildOpenLitematic(int i) {
        if (LitematicaHelper.isLitematicaPresent()) {
            //if java.lang.NoSuchMethodError is thrown see comment in SchematicPlacementManager
            if (LitematicaHelper.hasLoadedSchematic(i)) {
                Tuple<IStaticSchematic, Vec3i> schematic = LitematicaHelper.getSchematic(i);
                Vec3i correctedOrigin = schematic.getB();
                ISchematic schematic2 = applyMapArtAndSelection(correctedOrigin, schematic.getA());
                build(schematic.getA().toString(), schematic2, correctedOrigin);
            } else {
                logDirect(String.format("List of placements has no entry %s", i + 1));
            }
        } else {
            logDirect("Litematica is not present");
        }
    }

    public void clearArea(BlockPos corner1, BlockPos corner2) {
        BlockPos origin = new BlockPos(Math.min(corner1.getX(), corner2.getX()), Math.min(corner1.getY(), corner2.getY()), Math.min(corner1.getZ(), corner2.getZ()));
        int widthX = Math.abs(corner1.getX() - corner2.getX()) + 1;
        int heightY = Math.abs(corner1.getY() - corner2.getY()) + 1;
        int lengthZ = Math.abs(corner1.getZ() - corner2.getZ()) + 1;
        build("clear area", new FillSchematic(widthX, heightY, lengthZ, Blocks.AIR.defaultBlockState()), origin);
    }

    @Override
    public List<BlockState> getApproxPlaceable() {
        return new ArrayList<>(approxPlaceable);
    }

    @Override
    public boolean isActive() {
        return schematic != null;
    }

    public BlockState placeAt(int x, int y, int z, BlockState current) {
        if (!isActive()) {
            return null;
        }
        if (!schematic.inSchematic(x - origin.getX(), y - origin.getY(), z - origin.getZ(), current)) {
            return null;
        }
        BlockState state = schematic.desiredState(x - origin.getX(), y - origin.getY(), z - origin.getZ(), current, this.approxPlaceable);
        if (state.getBlock() instanceof AirBlock) {
            return null;
        }
        return state;
    }

    /**
     * Creative mode with auto-give enabled. In this mode we do not require the desired block to already
     * be in the inventory; we obtain it with /give on demand instead.
     */
    private boolean isCreativeBuild() {
        return Baritone.settings().buildCreativeAutoGive.value && ctx.playerController().getGameType().isCreative();
    }

    /**
     * Whether a block has an item form and can therefore be obtained with /give and placed by hand.
     * Fluids, fire, portal blocks, piston heads and other technical blocks do not.
     */
    private static boolean giveable(BlockState state) {
        return state.getBlock().asItem() != Items.AIR;
    }

    /**
     * A desired block that is not air but has no item form can never be placed, so it must be ignored
     * rather than reported as missing material.
     */
    private boolean unobtainable(BlockState desired) {
        return desired != null
                && !(desired.getBlock() instanceof AirBlock)
                && (!giveable(desired) || creativeUnGiveable.contains(desired.getBlock()));
    }

    private boolean hasBlockInInventory(Block block) {
        for (ItemStack stack : ctx.player().getInventory().items) {
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem && ((BlockItem) stack.getItem()).getBlock() == block) {
                return true;
            }
        }
        return false;
    }

    private Optional<Tuple<BetterBlockPos, Rotation>> toBreakNearPlayer(BuilderCalculationContext bcc) {
        BetterBlockPos center = ctx.playerFeet();
        BetterBlockPos pathStart = baritone.getPathingBehavior().pathStart();
        for (int dx = -5; dx <= 5; dx++) {
            for (int dy = Baritone.settings().breakFromAbove.value ? -1 : 0; dy <= 5; dy++) {
                for (int dz = -5; dz <= 5; dz++) {
                    int x = center.x + dx;
                    int y = center.y + dy;
                    int z = center.z + dz;
                    if (dy == -1 && x == pathStart.x && z == pathStart.z) {
                        continue; // dont mine what we're supported by, but not directly standing on
                    }
                    BlockState desired = bcc.getSchematic(x, y, z, bcc.bsi.get0(x, y, z));
                    if (desired == null) {
                        continue; // irrelevant
                    }
                    BlockState curr = bcc.bsi.get0(x, y, z);
                    if (!(curr.getBlock() instanceof AirBlock) && !(curr.getBlock() == Blocks.WATER || curr.getBlock() == Blocks.LAVA) && !valid(curr, desired, false)) {
                        BetterBlockPos pos = new BetterBlockPos(x, y, z);
                        Optional<Rotation> rot = RotationUtils.reachable(ctx, pos, ctx.playerController().getBlockReachDistance());
                        if (rot.isPresent()) {
                            return Optional.of(new Tuple<>(pos, rot.get()));
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    public static class Placement {

        private final int hotbarSelection;
        private final BlockPos placeAgainst;
        private final Direction side;
        private final Rotation rot;

        public Placement(int hotbarSelection, BlockPos placeAgainst, Direction side, Rotation rot) {
            this.hotbarSelection = hotbarSelection;
            this.placeAgainst = placeAgainst;
            this.side = side;
            this.rot = rot;
        }
    }

    private Optional<Placement> searchForPlacables(BuilderCalculationContext bcc, List<BlockState> desirableOnHotbar) {
        BetterBlockPos center = ctx.playerFeet();
        for (int dx = -5; dx <= 5; dx++) {
            for (int dy = -5; dy <= 1; dy++) {
                for (int dz = -5; dz <= 5; dz++) {
                    int x = center.x + dx;
                    int y = center.y + dy;
                    int z = center.z + dz;
                    BlockState desired = bcc.getSchematic(x, y, z, bcc.bsi.get0(x, y, z));
                    if (desired == null) {
                        continue; // irrelevant
                    }
                    BlockState curr = bcc.bsi.get0(x, y, z);
                    if (MovementHelper.isReplaceable(x, y, z, curr, bcc.bsi) && !valid(curr, desired, false)) {
                        if (dy == 1 && bcc.bsi.get0(x, y + 1, z).getBlock() instanceof AirBlock) {
                            continue;
                        }
                        desirableOnHotbar.add(desired);
                        Optional<Placement> opt = possibleToPlace(desired, x, y, z, bcc.bsi);
                        if (opt.isPresent()) {
                            return opt;
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    public boolean placementPlausible(BlockPos pos, BlockState state) {
        VoxelShape voxelshape = state.getCollisionShape(ctx.world(), pos);
        return voxelshape.isEmpty() || ctx.world().isUnobstructed(null, voxelshape.move(pos.getX(), pos.getY(), pos.getZ()));
    }

    private Optional<Placement> possibleToPlace(BlockState toPlace, int x, int y, int z, BlockStateInterface bsi) {
        // these two don't care which face we're placing against, but they were asked for every face
        // placementPlausible is an entity collision query, so that's up to six of those per position per tick
        // still only asked once we've found something solid to place against, like before
        boolean checkedPlaceable = false;
        for (Direction against : Direction.values()) {
            BetterBlockPos placeAgainstPos = new BetterBlockPos(x, y, z).relative(against);
            BlockState placeAgainstState = bsi.get0(placeAgainstPos);
            if (MovementHelper.isReplaceable(placeAgainstPos.x, placeAgainstPos.y, placeAgainstPos.z, placeAgainstState, bsi)) {
                continue;
            }
            if (!checkedPlaceable) {
                BetterBlockPos pos = new BetterBlockPos(x, y, z);
                if (!toPlace.canSurvive(ctx.world(), pos) || !placementPlausible(pos, toPlace)) {
                    return Optional.empty();
                }
                checkedPlaceable = true;
            }
            VoxelShape shape = placeAgainstState.getShape(ctx.world(), placeAgainstPos);
            if (shape.isEmpty()) {
                continue;
            }
            AABB aabb = shape.bounds();
            for (Vec3 placementMultiplier : aabbSideMultipliers(against)) {
                double placeX = placeAgainstPos.x + aabb.minX * placementMultiplier.x + aabb.maxX * (1 - placementMultiplier.x);
                double placeY = placeAgainstPos.y + aabb.minY * placementMultiplier.y + aabb.maxY * (1 - placementMultiplier.y);
                double placeZ = placeAgainstPos.z + aabb.minZ * placementMultiplier.z + aabb.maxZ * (1 - placementMultiplier.z);
                Rotation rot = RotationUtils.calcRotationFromVec3d(RayTraceUtils.inferSneakingEyePosition(ctx.player()), new Vec3(placeX, placeY, placeZ), ctx.playerRotations());
                // Ray trace and simulate with the stable ideal rotation, NOT the randomized aim. If we used
                // the aim (which jitters by randomLooking every tick) the "first placeable position" would
                // change every tick, so the builder would keep switching targets and the camera would never
                // settle on one, which looks like it freezing. The click path below still requires the
                // client to actually look at the target, so a target that can never be clicked is dropped
                // by the placement-timeout logic in onTick.
                HitResult result = RayTraceUtils.rayTraceTowards(ctx.player(), rot, ctx.playerController().getBlockReachDistance(), true);
                if (result != null && result.getType() == HitResult.Type.BLOCK && ((BlockHitResult) result).getBlockPos().equals(placeAgainstPos) && ((BlockHitResult) result).getDirection() == against.getOpposite()) {
                    OptionalInt hotbar = hasAnyItemThatWouldPlace(toPlace, result, rot);
                    if (hotbar.isPresent()) {
                        return Optional.of(new Placement(hotbar.getAsInt(), placeAgainstPos, against.getOpposite(), rot));
                    }
                }
            }
        }
        return Optional.empty();
    }

    private OptionalInt hasAnyItemThatWouldPlace(BlockState desired, HitResult result, Rotation rot) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = ctx.player().getInventory().items.get(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) {
                continue;
            }
            float originalYaw = ctx.player().getYRot();
            float originalPitch = ctx.player().getXRot();
            // the state depends on the facing of the player sometimes
            ctx.player().setYRot(rot.getYaw());
            ctx.player().setXRot(rot.getPitch());
            BlockPlaceContext meme = new BlockPlaceContext(new UseOnContext(
                    ctx.world(),
                    ctx.player(),
                    InteractionHand.MAIN_HAND,
                    stack,
                    (BlockHitResult) result
            ) {}); // that {} gives us access to a protected constructor lmfao
            BlockState wouldBePlaced = ((BlockItem) stack.getItem()).getBlock().getStateForPlacement(meme);
            ctx.player().setYRot(originalYaw);
            ctx.player().setXRot(originalPitch);
            if (wouldBePlaced == null) {
                continue;
            }
            if (!meme.canPlace()) {
                continue;
            }
            if (valid(wouldBePlaced, desired, true)) {
                return OptionalInt.of(i);
            }
        }
        return OptionalInt.empty();
    }

    private static Vec3[] aabbSideMultipliers(Direction side) {
        switch (side) {
            case UP:
                return new Vec3[]{new Vec3(0.5, 1, 0.5), new Vec3(0.1, 1, 0.5), new Vec3(0.9, 1, 0.5), new Vec3(0.5, 1, 0.1), new Vec3(0.5, 1, 0.9)};
            case DOWN:
                return new Vec3[]{new Vec3(0.5, 0, 0.5), new Vec3(0.1, 0, 0.5), new Vec3(0.9, 0, 0.5), new Vec3(0.5, 0, 0.1), new Vec3(0.5, 0, 0.9)};
            case NORTH:
            case SOUTH:
            case EAST:
            case WEST:
                double x = side.getStepX() == 0 ? 0.5 : (1 + side.getStepX()) / 2D;
                double z = side.getStepZ() == 0 ? 0.5 : (1 + side.getStepZ()) / 2D;
                return new Vec3[]{new Vec3(x, 0.25, z), new Vec3(x, 0.75, z)};
            default: // null
                throw new IllegalStateException("Unexpected side " + side);
        }
    }

    @Override
    public PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel) {
        return onTick(calcFailed, isSafeToCancel, 0);
    }

    private PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel, int recursions) {
        if (recursions > 100) { // onTick calls itself, don't crash
            return new PathingCommand(null, PathingCommandType.SET_GOAL_AND_PATH);
        }
        traceTick++;
        if (traceLastFeet == null || !traceLastFeet.equals(ctx.playerFeet())) {
            traceLastFeet = new BetterBlockPos(ctx.playerFeet());
            traceLastFeetChange = traceTick;
        }
        approxPlaceable = approxPlaceable(36);
        if (baritone.getInputOverrideHandler().isInputForcedDown(Input.CLICK_LEFT)) {
            ticks = 5;
        } else {
            ticks--;
        }
        baritone.getInputOverrideHandler().clearAllKeys();
        if (paused) {
            return new PathingCommand(null, PathingCommandType.CANCEL_AND_SET_GOAL);
        }
        if (Baritone.settings().buildInLayers.value) {
            if (realSchematic == null) {
                realSchematic = schematic;
            }
            ISchematic realSchematic = this.realSchematic; // wrap this properly, dont just have the inner class refer to the builderprocess.this
            int minYInclusive;
            int maxYInclusive;
            // layer = 0 should be nothing
            // layer = realSchematic.heightY() should be everything
            if (Baritone.settings().layerOrder.value) { // top to bottom
                maxYInclusive = realSchematic.heightY() - 1;
                minYInclusive = realSchematic.heightY() - layer * Baritone.settings().layerHeight.value;
            } else {
                maxYInclusive = layer * Baritone.settings().layerHeight.value - 1;
                minYInclusive = 0;
            }
            schematic = new ISchematic() {
                @Override
                public BlockState desiredState(int x, int y, int z, BlockState current, List<BlockState> approxPlaceable) {
                    return realSchematic.desiredState(x, y, z, current, BuilderProcess.this.approxPlaceable);
                }

                @Override
                public boolean inSchematic(int x, int y, int z, BlockState currentState) {
                    return ISchematic.super.inSchematic(x, y, z, currentState) && y >= minYInclusive && y <= maxYInclusive && realSchematic.inSchematic(x, y, z, currentState);
                }

                @Override
                public void reset() {
                    realSchematic.reset();
                }

                @Override
                public int widthX() {
                    return realSchematic.widthX();
                }

                @Override
                public int heightY() {
                    return realSchematic.heightY();
                }

                @Override
                public int lengthZ() {
                    return realSchematic.lengthZ();
                }
            };
        }
        BuilderCalculationContext bcc = new BuilderCalculationContext();
        if (!recalc(bcc)) {
            if (Baritone.settings().buildInLayers.value && layer * Baritone.settings().layerHeight.value < stopAtHeight) {
                logDirect("Starting layer " + layer);
                layer++;
                return onTick(calcFailed, isSafeToCancel, recursions + 1);
            }
            Vec3i repeat = Baritone.settings().buildRepeat.value;
            int max = Baritone.settings().buildRepeatCount.value;
            numRepeats++;
            if (repeat.equals(new Vec3i(0, 0, 0)) || (max != -1 && numRepeats >= max)) {
                logDirect("Done building");
                if (Baritone.settings().notificationOnBuildFinished.value) {
                    logNotification("Done building", false);
                }
                onLostControl();
                return null;
            }
            // build repeat time
            layer = 0;
            origin = new BlockPos(origin).offset(repeat);
            if (!Baritone.settings().buildRepeatSneaky.value) {
                schematic.reset();
            }
            logDirect("Repeating build in vector " + repeat + ", new origin is " + origin);
            return onTick(calcFailed, isSafeToCancel, recursions + 1);
        }
        if (Baritone.settings().distanceTrim.value) {
            trim();
        }
        if (isCreativeBuild()) {
            creativeGive(bcc);
        }

        Optional<Tuple<BetterBlockPos, Rotation>> toBreak = toBreakNearPlayer(bcc);
        if (toBreak.isPresent() && isSafeToCancel && ctx.player().onGround()) {
            // we'd like to pause to break this block
            // only change look direction if it's safe (don't want to fuck up an in progress parkour for example
            Rotation rot = toBreak.get().getB();
            BetterBlockPos pos = toBreak.get().getA();
            baritone.getLookBehavior().updateTarget(rot, true);
            MovementHelper.switchToBestToolFor(ctx, bcc.get(pos));
            if (ctx.player().isCrouching()) {
                // really horrible bug where a block is visible for breaking while sneaking but not otherwise
                // so you can't see it, it goes to place something else, sneaks, then the next tick it tries to break
                // and is unable since it's unsneaked in the intermediary tick
                baritone.getInputOverrideHandler().setInputForceState(Input.SNEAK, true);
            }
            boolean clicking = ctx.isLookingAt(pos) || ctx.playerRotations().isReallyCloseTo(rot);
            buildTrace(String.format("BREAK %s at %s (now=%s) target(%s) player(%s) clicking=%s",
                    describeState(bcc.bsi.get0(pos)), pos, describeState(bcc.getSchematic(pos.x, pos.y, pos.z, bcc.bsi.get0(pos))),
                    describeRotation(rot), describeRotation(ctx.playerRotations()), clicking));
            if (clicking) {
                baritone.getInputOverrideHandler().setInputForceState(Input.CLICK_LEFT, true);
            }
            return new PathingCommand(null, PathingCommandType.CANCEL_AND_SET_GOAL);
        }
        List<BlockState> desirableOnHotbar = new ArrayList<>();
        Optional<Placement> toPlace = searchForPlacables(bcc, desirableOnHotbar);

        // Stand-still watchdog: if we have not moved for 5 seconds while the builder is in control, dump a
        // full snapshot (position, camera, target, inventory) so a stuck build can be diagnosed from the
        // log. This runs before the placement return below, otherwise it would never fire while the bot is
        // stuck repeating a placement it can never actually perform.
        if (!baritone.getPathingBehavior().isPathing()
                && traceTick - traceLastFeetChange >= 100
                && traceTick - lastStallLogTick >= 100) {
            lastStallLogTick = traceTick;
            buildTrace(String.format(
                    "STALL %.1fs at %s | toPlace=%s pathing=%s player(%s) | left=%d need=[%s]",
                    (traceTick - traceLastFeetChange) / 20.0, ctx.playerFeet(), toPlace.isPresent(),
                    baritone.getPathingBehavior().isPathing(), describeRotation(ctx.playerRotations()),
                    incorrectPositions == null ? 0 : incorrectPositions.size(), summarizeNeeded(desirableOnHotbar)));
            logStuckDiagnostics(desirableOnHotbar);
        }

        if (toPlace.isPresent() && isSafeToCancel && ctx.player().onGround() && ticks <= 0) {
            Rotation rot = toPlace.get().rot;
            baritone.getLookBehavior().updateTarget(rot, true);
            ctx.player().getInventory().selected = toPlace.get().hotbarSelection;
            baritone.getInputOverrideHandler().setInputForceState(Input.SNEAK, true);
            boolean lookingAtTarget = ctx.isLookingAt(toPlace.get().placeAgainst) && ((BlockHitResult) ctx.objectMouseOver()).getDirection().equals(toPlace.get().side);
            boolean closeToRot = ctx.playerRotations().isReallyCloseTo(rot);
            BlockPos placePos = toPlace.get().placeAgainst.relative(toPlace.get().side);
            if (lookingAtTarget || closeToRot) {
                placeAttemptPos = null;
                placeAttemptTicks = 0;
            } else if (placePos.equals(placeAttemptPos)) {
                placeAttemptTicks++;
            } else {
                placeAttemptPos = placePos;
                placeAttemptTicks = 1;
            }
            BlockState desired = bcc.getSchematic(placePos.getX(), placePos.getY(), placePos.getZ(), bcc.bsi.get0(placePos));
            String placeKey = placePos + "|" + (desired == null ? "null" : desired.toString());
            if (!placeKey.equals(lastPlaceTraceKey) || traceTick - lastPlaceTraceTick > 20) {
                lastPlaceTraceKey = placeKey;
                lastPlaceTraceTick = traceTick;
                ItemStack held = ctx.player().getInventory().items.get(toPlace.get().hotbarSelection);
                HitResult over = ctx.objectMouseOver();
                String overStr;
                if (over instanceof BlockHitResult) {
                    BlockHitResult bhr = (BlockHitResult) over;
                    overStr = bhr.getBlockPos() + "/" + bhr.getDirection();
                } else {
                    overStr = over == null ? "null" : over.getType().name();
                }
                buildTrace(String.format("PLACE %s at %s against %s side=%s slot=%d held=%s feet=%s eye=(%.2f,%.2f,%.2f) target(%s) player(%s) over=%s lookingAtTarget=%s closeToRot=%s",
                        desired, placePos, toPlace.get().placeAgainst, toPlace.get().side, toPlace.get().hotbarSelection,
                        held.isEmpty() ? "-" : BuiltInRegistries.ITEM.getKey(held.getItem()), ctx.playerFeet(),
                        ctx.player().getEyePosition().x, ctx.player().getEyePosition().y, ctx.player().getEyePosition().z,
                        describeRotation(rot), describeRotation(ctx.playerRotations()), overStr, lookingAtTarget, closeToRot));
            }
            if (lookingAtTarget || closeToRot) {
                baritone.getInputOverrideHandler().setInputForceState(Input.CLICK_RIGHT, true);
            } else if (placeAttemptTicks > 20) {
                // We have been aiming at this exact placement for a second and the client still is not
                // looking at the required face, so it can never be clicked from here. If the target is far
                // away, walk to it; if we are already next to it, shuffle to a different stance.
                buildTrace(String.format("PLACE stuck %dt at %s from %s, repositioning", placeAttemptTicks, placePos, ctx.playerFeet()));
                placeAttemptPos = null;
                placeAttemptTicks = 0;
                double distSq = ctx.player().getEyePosition()
                        .distanceToSqr(placePos.getX() + 0.5, placePos.getY() + 0.5, placePos.getZ() + 0.5);
                if (distSq > 2.25) { // more than 1.5 blocks away: go stand next to the target
                    return new PathingCommandContext(new GoalGetToBlock(placePos), PathingCommandType.SET_GOAL_AND_PATH, bcc);
                }
                triedNudges.add(ctx.playerFeet());
                Goal nudge = unstickGoal(triedNudges);
                if (nudge != null) {
                    return new PathingCommandContext(nudge, PathingCommandType.SET_GOAL_AND_PATH, bcc);
                }
            }
            return new PathingCommand(null, PathingCommandType.CANCEL_AND_SET_GOAL);
        }

        if (Baritone.settings().allowInventory.value || isCreativeBuild()) {
            ArrayList<Integer> usefulSlots = new ArrayList<>();
            List<BlockState> noValidHotbarOption = new ArrayList<>();
            outer:
            for (BlockState desired : desirableOnHotbar) {
                for (int i = 0; i < 9; i++) {
                    if (canPlaceBlock(approxPlaceable.get(i), desired)) {
                        usefulSlots.add(i);
                        continue outer;
                    }
                }
                noValidHotbarOption.add(desired);
            }

            outer:
            for (int i = 9; i < 36; i++) {
                for (BlockState desired : noValidHotbarOption) {
                    if (canPlaceBlock(approxPlaceable.get(i), desired)) {
                        if (!baritone.getInventoryBehavior().attemptToPutOnHotbar(i, usefulSlots::contains)) {
                            // awaiting inventory move, so pause
                            return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
                        }
                        break outer;
                    }
                }
            }
        }

        // The goal returned below is a GoalComposite satisfied by being adjacent to ANY target, so if we
        // are already adjacent but cannot place anything (e.g. the only reachable face is occluded), no path
        // is computed and we stand still until the player nudges us. Detect that and force a reposition.
        // We must not nudge while a /give is still on cooldown (the item may simply not have arrived yet) nor
        // while an inventory move is queued, and to avoid ping-ponging between two equally bad spots we only
        // ever try each stand position once per stuck episode. If every candidate has been tried we stop.
        boolean awaitingCreativeGive = isCreativeBuild() && creativeGiveCooldown > 0;
        if (toPlace.isPresent() || baritone.getPathingBehavior().isPathing() || awaitingCreativeGive) {
            stuckTicks = 0;
            if (toPlace.isPresent()) {
                triedNudges.clear();
            }
        } else if (incorrectPositions != null && !incorrectPositions.isEmpty()) {
            if (++stuckTicks > 30) {
                stuckTicks = 0;
                logStuckDiagnostics(desirableOnHotbar);
                Goal nudge = unstickGoal(triedNudges);
                if (nudge != null) {
                    logDirect("Stuck, repositioning to continue building");
                    logDebug("Builder could not place anything from here, repositioning to " + nudge);
                    return new PathingCommandContext(nudge, PathingCommandType.SET_GOAL_AND_PATH, bcc);
                }
            }
        } else {
            stuckTicks = 0;
        }

        Goal goal = assemble(bcc, approxPlaceable.subList(0, 9));
        if (goal == null) {
            goal = assemble(bcc, approxPlaceable, true); // we're far away, so assume that we have our whole inventory to recalculate placeable properly
            if (goal == null) {
                if (Baritone.settings().skipFailedLayers.value && Baritone.settings().buildInLayers.value && layer * Baritone.settings().layerHeight.value < realSchematic.heightY()) {
                    logDirect("Skipping layer that I cannot construct! Layer #" + layer);
                    layer++;
                    return onTick(calcFailed, isSafeToCancel, recursions + 1);
                }
                logDirect("Unable to do it. Pausing. resume to resume, cancel to cancel");
                paused = true;
                return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
            }
        }
        return new PathingCommandContext(goal, PathingCommandType.FORCE_REVALIDATE_GOAL_AND_PATH, bcc);
    }

    /**
     * Detailed build trace, written to the game log (as [STDOUT]) without filling the chat. Every state
     * change the builder goes through is recorded here so a stuck build can be diagnosed after the fact.
     */
    private static void buildTrace(String message) {
        System.out.println("[Baritone-Build] " + message);
    }

    private static String describeRotation(Rotation rot) {
        return String.format("yaw=%.1f pitch=%.1f", rot.getYaw(), rot.getPitch());
    }

    /**
     * Compact listing of the first few distinct blockstates the builder still wants to place.
     */
    private static String summarizeNeeded(List<BlockState> desirableOnHotbar) {
        Map<BlockState, Integer> counts = new LinkedHashMap<>();
        for (BlockState desired : desirableOnHotbar) {
            counts.merge(desired, 1, Integer::sum);
        }
        StringBuilder sb = new StringBuilder();
        int shown = 0;
        for (Map.Entry<BlockState, Integer> entry : counts.entrySet()) {
            if (shown++ >= 6) {
                sb.append("...");
                break;
            }
            if (shown > 1) {
                sb.append(' ');
            }
            sb.append(describeState(entry.getKey())).append('x').append(entry.getValue());
        }
        return sb.toString();
    }

    /**
     * Build-status diagnostic, emitted (throttled) only when the builder is stuck. Reports what blocks
     * are still needed and how many of each, whether each is on the hotbar / in the inventory / nowhere,
     * what is currently on the hotbar, how much inventory room is left, and whether pathing is active.
     * This is what makes a stuck build debuggable from the log.
     */
    private void logStuckDiagnostics(List<BlockState> desirableOnHotbar) {
        List<BlockState> hotbar = approxPlaceable.subList(0, 9);
        int emptySlots = 0;
        Set<Block> invBlocks = new HashSet<>();
        for (ItemStack stack : ctx.player().getInventory().items) {
            if (stack.isEmpty()) {
                emptySlots++;
            } else if (stack.getItem() instanceof BlockItem) {
                invBlocks.add(((BlockItem) stack.getItem()).getBlock());
            }
        }

        // group still-needed blocks by their exact blockstate, preserving insertion order
        Map<BlockState, Integer> counts = new LinkedHashMap<>();
        for (BlockState desired : desirableOnHotbar) {
            counts.merge(desired, 1, Integer::sum);
        }
        StringBuilder need = new StringBuilder();
        int shown = 0;
        int onHotbar = 0;
        int inInventory = 0;
        int nowhere = 0;
        for (Map.Entry<BlockState, Integer> entry : counts.entrySet()) {
            BlockState desired = entry.getKey();
            boolean hot = containsBlockState(hotbar, desired) || anyHotbarCanPlace(desired);
            boolean inv = invBlocks.contains(desired.getBlock());
            if (hot) {
                onHotbar++;
            } else if (inv) {
                inInventory++;
            } else {
                nowhere++;
            }
            if (shown++ < 6) {
                need.append(describeState(desired)).append('x').append(entry.getValue())
                    .append(hot ? "(hotbar)" : inv ? "(inventory)" : "(NOWHERE)")
                    .append(' ');
            }
        }
        if (counts.size() > 6) {
            need.append("...");
        }

        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = ctx.player().getInventory().items.get(i);
            bar.append(i).append(':').append(stack.isEmpty() ? "-" : BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath()).append(' ');
        }

        logDirect(String.format(
                "Build status: %d left, %d types need [%s] hotbar=%d inventory=%d nowhere=%d | slots[%s] empty=%d invTypes=%d pathing=%s",
                incorrectPositions.size(), counts.size(), need.toString().trim(),
                onHotbar, inInventory, nowhere, bar.toString().trim(),
                emptySlots, invBlocks.size(), baritone.getPathingBehavior().isPathing()));
    }

    private boolean anyHotbarCanPlace(BlockState desired) {
        for (int i = 0; i < 9; i++) {
            if (canPlaceBlock(approxPlaceable.get(i), desired)) {
                return true;
            }
        }
        return false;
    }

    private static String describeState(BlockState state) {
        StringBuilder sb = new StringBuilder(BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath());
        if (!state.getValues().isEmpty()) {
            sb.append('[');
            boolean first = true;
            for (Map.Entry<Property<?>, Comparable<?>> e : state.getValues().entrySet()) {
                if (!first) {
                    sb.append(',');
                }
                sb.append(e.getKey().getName()).append('=').append(e.getValue());
                first = false;
            }
            sb.append(']');
        }
        return sb.toString();
    }

    /**
     * When the builder is adjacent to its target but cannot place anything from where it stands, pick a
     * standable spot next to the nearest block still needing work so that pathing moves us somewhere we
     * can actually build from. Candidates already tried during this stuck episode are skipped so that we
     * never bounce between two spots; returns null once every reachable candidate has been tried.
     */
    private Goal unstickGoal(Set<BlockPos> tried) {
        BetterBlockPos feet = ctx.playerFeet();
        BlockPos nearest = null;
        double bestDist = Double.MAX_VALUE;
        for (BetterBlockPos pos : incorrectPositions) {
            double dist = pos.distSqr(feet);
            if (dist < bestDist) {
                bestDist = dist;
                nearest = pos;
            }
        }
        if (nearest == null) {
            return null;
        }
        List<BlockPos> candidates = new ArrayList<>();
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos side = nearest.relative(dir);
            candidates.add(side);
            candidates.add(side.above());
        }
        candidates.add(nearest);
        candidates.add(nearest.above());
        BlockPos best = null;
        double bestScore = Double.MAX_VALUE;
        for (BlockPos candidate : candidates) {
            if (candidate.equals(feet) || tried.contains(candidate) || !standable(candidate)) {
                continue;
            }
            double score = candidate.distSqr(feet);
            if (score < bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        if (best == null) {
            return null;
        }
        tried.add(best);
        return new GoalBlock(best);
    }

    /**
     * Whether the player could stand with their feet at the given position: both the feet and head blocks
     * passable, and the block below solid enough to stand on.
     */
    private boolean standable(BlockPos pos) {
        BlockState feet = ctx.world().getBlockState(pos);
        BlockState head = ctx.world().getBlockState(pos.above());
        if (!feet.getCollisionShape(ctx.world(), pos).isEmpty()) {
            return false;
        }
        if (!head.getCollisionShape(ctx.world(), pos.above()).isEmpty()) {
            return false;
        }
        return !ctx.world().getBlockState(pos.below()).getCollisionShape(ctx.world(), pos.below()).isEmpty();
    }

    private static final int CREATIVE_GIVE_COOLDOWN = 40;
    private static final int CREATIVE_GIVE_MAX_ATTEMPTS = 5;
    private static final int CREATIVE_GIVE_RADIUS = 32;

    /**
     * Creative mode: obtain, via /give, blocks the schematic needs that we don't have in the inventory.
     * Only the number of nearby positions that actually need the block is requested, capped at one stack,
     * and only one block type is given per invocation. Giving a full stack of every block type fills the
     * inventory and starves the other blocks, which is exactly what we must avoid.
     * Throttled so the item has time to arrive, and gives up (marking the block unobtainable) after a few
     * attempts so a permission-less server isn't spammed.
     */
    private void creativeGive(BuilderCalculationContext bcc) {
        if (creativeGiveCooldown > 0) {
            creativeGiveCooldown--;
            return;
        }
        if (incorrectPositions == null) {
            return;
        }
        BlockPos playerPos = ctx.player().blockPosition();
        int radiusSq = CREATIVE_GIVE_RADIUS * CREATIVE_GIVE_RADIUS;
        Map<Block, Integer> needed = new LinkedHashMap<>();
        for (BetterBlockPos pos : incorrectPositions) {
            if (pos.distSqr(playerPos) > radiusSq) {
                continue; // only stock up for what's near us, not the entire schematic at once
            }
            BlockState current = bcc.bsi.get0(pos);
            if (!(current.getBlock() instanceof AirBlock)) {
                continue;
            }
            BlockState desired = bcc.getSchematic(pos.x, pos.y, pos.z, current);
            if (desired == null || desired.getBlock() instanceof AirBlock) {
                continue;
            }
            Block block = desired.getBlock();
            if (!giveable(desired) || creativeUnGiveable.contains(block)) {
                continue;
            }
            if (hasBlockInInventory(block)) {
                continue;
            }
            needed.merge(block, 1, Integer::sum);
        }
        if (needed.isEmpty()) {
            return;
        }
        // pick the block we need the most of, so a limited number of gives goes as far as possible
        Block block = null;
        int bestCount = 0;
        for (Map.Entry<Block, Integer> entry : needed.entrySet()) {
            if (entry.getValue() > bestCount) {
                bestCount = entry.getValue();
                block = entry.getKey();
            }
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(block.asItem());
        if (id == null) {
            creativeUnGiveable.add(block);
            return;
        }
        int attempts = creativeGiveAttempts.getOrDefault(block, 0);
        if (attempts >= CREATIVE_GIVE_MAX_ATTEMPTS) {
            creativeUnGiveable.add(block);
            logDirect(String.format("Could not obtain %s via /give; skipping it (is /give allowed here?)", id));
            return;
        }
        if (ctx.player().connection == null || !canReceiveGive(block)) {
            return; // no room to receive it; a give now would just drop on the ground
        }
        int amount = Math.min(64, bestCount);
        creativeGiveAttempts.put(block, attempts + 1);
        ctx.player().connection.sendCommand(String.format("give @s %s %d", id, amount));
        logDebug("Creative auto-give: " + amount + " " + id);
        creativeGiveCooldown = CREATIVE_GIVE_COOLDOWN;
    }

    /**
     * Whether a /give of this block would actually fit: either there is an empty slot, or there is an
     * existing stack of the same item that still has room. Without this, once every slot holds a different
     * block type the inventory is full and no further block can be obtained.
     */
    private boolean canReceiveGive(Block block) {
        Item item = block.asItem();
        for (ItemStack stack : ctx.player().getInventory().items) {
            if (stack.isEmpty()) {
                return true;
            }
            if (stack.getItem() == item && stack.getCount() < stack.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    private boolean recalc(BuilderCalculationContext bcc) {
        if (incorrectPositions == null) {
            incorrectPositions = new HashSet<>();
            fullRecalc(bcc);
            if (incorrectPositions.isEmpty()) {
                return false;
            }
        }
        recalcNearby(bcc);
        if (incorrectPositions.isEmpty()) {
            fullRecalc(bcc);
        }
        return !incorrectPositions.isEmpty();
    }

    private void trim() {
        HashSet<BetterBlockPos> copy = new HashSet<>(incorrectPositions);
        copy.removeIf(pos -> pos.distSqr(ctx.player().blockPosition()) > 200);
        if (!copy.isEmpty()) {
            incorrectPositions = copy;
        }
    }

    private void recalcNearby(BuilderCalculationContext bcc) {
        BetterBlockPos center = ctx.playerFeet();
        int radius = Baritone.settings().builderTickScanRadius.value;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int x = center.x + dx;
                    int y = center.y + dy;
                    int z = center.z + dz;
                    BlockState desired = bcc.getSchematic(x, y, z, bcc.bsi.get0(x, y, z));
                    if (desired != null) {
                        // we care about this position
                        BetterBlockPos pos = new BetterBlockPos(x, y, z);
                        if (unobtainable(desired) || valid(bcc.bsi.get0(x, y, z), desired, false)) {
                            incorrectPositions.remove(pos);
                            observedCompleted.add(BetterBlockPos.longHash(pos));
                        } else {
                            incorrectPositions.add(pos);
                            observedCompleted.remove(BetterBlockPos.longHash(pos));
                        }
                    }
                }
            }
        }
    }

    private void fullRecalc(BuilderCalculationContext bcc) {
        incorrectPositions = new HashSet<>();
        for (int y = 0; y < schematic.heightY(); y++) {
            for (int z = 0; z < schematic.lengthZ(); z++) {
                for (int x = 0; x < schematic.widthX(); x++) {
                    int blockX = x + origin.getX();
                    int blockY = y + origin.getY();
                    int blockZ = z + origin.getZ();
                    BlockState current = bcc.bsi.get0(blockX, blockY, blockZ);
                    if (!schematic.inSchematic(x, y, z, current)) {
                        continue;
                    }
                    BlockState desired = schematic.desiredState(x, y, z, current, this.approxPlaceable);
                    if (unobtainable(desired)) {
                        // no item form (or we gave up on /give); this can never be built, so treat as done
                        observedCompleted.add(BetterBlockPos.longHash(blockX, blockY, blockZ));
                        continue;
                    }
                    if (bcc.bsi.worldContainsLoadedChunk(blockX, blockZ)) { // check if its in render distance, not if its in cache
                        // we can directly observe this block, it is in render distance
                        if (valid(current, desired, false)) {
                            observedCompleted.add(BetterBlockPos.longHash(blockX, blockY, blockZ));
                        } else {
                            incorrectPositions.add(new BetterBlockPos(blockX, blockY, blockZ));
                            observedCompleted.remove(BetterBlockPos.longHash(blockX, blockY, blockZ));
                            if (incorrectPositions.size() > Baritone.settings().incorrectSize.value) {
                                return;
                            }
                        }
                        continue;
                    }
                    // this is not in render distance
                    if (!observedCompleted.contains(BetterBlockPos.longHash(blockX, blockY, blockZ))) {
                        // and we've never seen this position be correct
                        // therefore mark as incorrect
                        incorrectPositions.add(new BetterBlockPos(blockX, blockY, blockZ));
                        if (incorrectPositions.size() > Baritone.settings().incorrectSize.value) {
                            return;
                        }
                    }
                }
            }
        }
    }

    private Goal assemble(BuilderCalculationContext bcc, List<BlockState> approxPlaceable) {
        return assemble(bcc, approxPlaceable, false);
    }

    private Goal assemble(BuilderCalculationContext bcc, List<BlockState> approxPlaceable, boolean logMissing) {
        List<BetterBlockPos> placeable = new ArrayList<>();
        List<BetterBlockPos> breakable = new ArrayList<>();
        List<BetterBlockPos> sourceLiquids = new ArrayList<>();
        List<BetterBlockPos> flowingLiquids = new ArrayList<>();
        Map<BlockState, Integer> missing = new HashMap<>();
        List<BetterBlockPos> outOfBounds = new ArrayList<>();
        incorrectPositions.forEach(pos -> {
            BlockState state = bcc.bsi.get0(pos);
            if (state.getBlock() instanceof AirBlock) {
                BlockState desired = bcc.getSchematic(pos.x, pos.y, pos.z, state);
                if (desired == null) {
                    outOfBounds.add(pos);
                } else if (unobtainable(desired)) {
                    // cannot be placed (no item form, or /give gave up); ignore it instead of pausing
                } else if (isCreativeBuild() || containsBlockState(approxPlaceable, desired)) {
                    // in creative we can /give the exact block on demand, so treat it as placeable
                    placeable.add(pos);
                } else {
                    missing.put(desired, 1 + missing.getOrDefault(desired, 0));
                }
            } else {
                if (state.getBlock() instanceof LiquidBlock) {
                    // if the block itself is JUST a liquid (i.e. not just a waterlogged block), we CANNOT break it
                    // TODO for 1.13 make sure that this only matches pure water, not waterlogged blocks
                    if (!MovementHelper.possiblyFlowing(state)) {
                        // if it's a source block then we want to replace it with a throwaway
                        sourceLiquids.add(pos);
                    } else {
                        flowingLiquids.add(pos);
                    }
                } else {
                    breakable.add(pos);
                }
            }
        });
        incorrectPositions.removeAll(outOfBounds);
        List<Goal> toBreak = new ArrayList<>();
        breakable.forEach(pos -> toBreak.add(breakGoal(pos, bcc)));
        List<Goal> toPlace = new ArrayList<>();
        // List.contains for every position was quadratic, and placeable is in the thousands on a big build
        Set<BetterBlockPos> placeableSet = new HashSet<>(placeable);
        placeable.forEach(pos -> {
            if (!placeableSet.contains(pos.below()) && !placeableSet.contains(pos.below(2))) {
                toPlace.add(placementGoal(pos, bcc));
            }
        });
        sourceLiquids.forEach(pos -> toPlace.add(new GoalBlock(pos.above())));

        if (!toPlace.isEmpty()) {
            return new JankyGoalComposite(new GoalComposite(toPlace.toArray(new Goal[0])), new GoalComposite(toBreak.toArray(new Goal[0])));
        }
        if (toBreak.isEmpty()) {
            if (logMissing && !missing.isEmpty()) {
                logDirect("Missing materials for at least:");
                logDirect(missing.entrySet().stream()
                        .map(e -> String.format("%sx %s", e.getValue(), e.getKey()))
                        .collect(Collectors.joining("\n")));
            }
            if (logMissing && !flowingLiquids.isEmpty()) {
                logDirect("Unreplaceable liquids at at least:");
                logDirect(flowingLiquids.stream()
                        .map(p -> String.format("%s %s %s", p.x, p.y, p.z))
                        .collect(Collectors.joining("\n")));
            }
            return null;
        }
        return new GoalComposite(toBreak.toArray(new Goal[0]));
    }

    public static class JankyGoalComposite implements Goal {

        private final Goal primary;
        private final Goal fallback;

        public JankyGoalComposite(Goal primary, Goal fallback) {
            this.primary = primary;
            this.fallback = fallback;
        }


        @Override
        public boolean isInGoal(int x, int y, int z) {
            return primary.isInGoal(x, y, z) || fallback.isInGoal(x, y, z);
        }

        @Override
        public double heuristic(int x, int y, int z) {
            return primary.heuristic(x, y, z);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            JankyGoalComposite goal = (JankyGoalComposite) o;
            return Objects.equals(primary, goal.primary)
                    && Objects.equals(fallback, goal.fallback);
        }

        @Override
        public int hashCode() {
            int hash = -1701079641;
            hash = hash * 1196141026 + primary.hashCode();
            hash = hash * -80327868 + fallback.hashCode();
            return hash;
        }

        @Override
        public String toString() {
            return "JankyComposite Primary: " + primary + " Fallback: " + fallback;
        }
    }

    public static class GoalBreak extends GoalGetToBlock {

        public GoalBreak(BlockPos pos) {
            super(pos);
        }

        @Override
        public boolean isInGoal(int x, int y, int z) {
            // can't stand right on top of a block, that might not work (what if it's unsupported, can't break then)
            if (y > this.y) {
                return false;
            }
            // but any other adjacent works for breaking, including inside or below
            return super.isInGoal(x, y, z);
        }

        @Override
        public String toString() {
            return String.format(
                    "GoalBreak{x=%s,y=%s,z=%s}",
                    SettingsUtil.maybeCensor(x),
                    SettingsUtil.maybeCensor(y),
                    SettingsUtil.maybeCensor(z)
            );
        }

        @Override
        public int hashCode() {
            return super.hashCode() * 1636324008;
        }
    }

    private Goal placementGoal(BlockPos pos, BuilderCalculationContext bcc) {
        if (!(ctx.world().getBlockState(pos).getBlock() instanceof AirBlock)) {  // TODO can this even happen?
            return new GoalPlace(pos);
        }
        boolean allowSameLevel = !(ctx.world().getBlockState(pos.above()).getBlock() instanceof AirBlock);
        BlockState current = ctx.world().getBlockState(pos);
        int minStand = Math.max(1, Baritone.settings().builderMinStandDistance.value);
        for (Direction facing : Movement.HORIZONTALS_BUT_ALSO_DOWN_____SO_EVERY_DIRECTION_EXCEPT_UP) {
            //noinspection ConstantConditions
            if (MovementHelper.canPlaceAgainst(ctx, pos.relative(facing)) && placementPlausible(pos, bcc.getSchematic(pos.getX(), pos.getY(), pos.getZ(), current))) {
                // optionally stand further back, on the side the placement face is visible from, so the
                // block is placed from a comfortable angle instead of right on top of it
                for (int d = minStand; d >= 2; d--) {
                    BlockPos stand = pos.relative(facing.getOpposite(), d);
                    if (standable(stand)) {
                        return new GoalBlock(stand);
                    }
                }
                return new GoalAdjacent(pos, pos.relative(facing), allowSameLevel);
            }
        }
        return new GoalPlace(pos);
    }

    private Goal breakGoal(BlockPos pos, BuilderCalculationContext bcc) {
        if (Baritone.settings().goalBreakFromAbove.value && bcc.bsi.get0(pos.above()).getBlock() instanceof AirBlock && bcc.bsi.get0(pos.above(2)).getBlock() instanceof AirBlock) { // TODO maybe possible without the up(2) check?
            return new JankyGoalComposite(new GoalBreak(pos), new GoalGetToBlock(pos.above()) {
                @Override
                public boolean isInGoal(int x, int y, int z) {
                    if (y > this.y || (x == this.x && y == this.y && z == this.z)) {
                        return false;
                    }
                    return super.isInGoal(x, y, z);
                }
            });
        }
        return new GoalBreak(pos);
    }

    public static class GoalAdjacent extends GoalGetToBlock {

        private boolean allowSameLevel;
        private BlockPos no;

        public GoalAdjacent(BlockPos pos, BlockPos no, boolean allowSameLevel) {
            super(pos);
            this.no = no;
            this.allowSameLevel = allowSameLevel;
        }

        @Override
        public boolean isInGoal(int x, int y, int z) {
            if (x == this.x && y == this.y && z == this.z) {
                return false;
            }
            if (x == no.getX() && y == no.getY() && z == no.getZ()) {
                return false;
            }
            if (!allowSameLevel && y == this.y - 1) {
                return false;
            }
            if (y < this.y - 1) {
                return false;
            }
            return super.isInGoal(x, y, z);
        }

        @Override
        public double heuristic(int x, int y, int z) {
            // prioritize lower y coordinates
            return this.y * 100 + super.heuristic(x, y, z);
        }

        @Override
        public boolean equals(Object o) {
            if (!super.equals(o)) {
                return false;
            }

            GoalAdjacent goal = (GoalAdjacent) o;
            return allowSameLevel == goal.allowSameLevel
                    && Objects.equals(no, goal.no);
        }

        @Override
        public int hashCode() {
            int hash = 806368046;
            hash = hash * 1412661222 + super.hashCode();
            hash = hash * 1730799370 + (int) BetterBlockPos.longHash(no.getX(), no.getY(), no.getZ());
            hash = hash * 260592149 + (allowSameLevel ? -1314802005 : 1565710265);
            return hash;
        }

        @Override
        public String toString() {
            return String.format(
                    "GoalAdjacent{x=%s,y=%s,z=%s}",
                    SettingsUtil.maybeCensor(x),
                    SettingsUtil.maybeCensor(y),
                    SettingsUtil.maybeCensor(z)
            );
        }
    }

    public static class GoalPlace extends GoalBlock {

        public GoalPlace(BlockPos placeAt) {
            super(placeAt.above());
        }

        @Override
        public double heuristic(int x, int y, int z) {
            // prioritize lower y coordinates
            return this.y * 100 + super.heuristic(x, y, z);
        }

        @Override
        public int hashCode() {
            return super.hashCode() * 1910811835;
        }

        @Override
        public String toString() {
            return String.format(
                    "GoalPlace{x=%s,y=%s,z=%s}",
                    SettingsUtil.maybeCensor(x),
                    SettingsUtil.maybeCensor(y),
                    SettingsUtil.maybeCensor(z)
            );
        }
    }

    @Override
    public void onLostControl() {
        incorrectPositions = null;
        name = null;
        schematic = null;
        realSchematic = null;
        layer = Baritone.settings().startAtLayer.value;
        numRepeats = 0;
        paused = false;
        observedCompleted = null;
        creativeGiveAttempts.clear();
        creativeUnGiveable.clear();
        creativeGiveCooldown = 0;
        stuckTicks = 0;
        triedNudges.clear();
    }

    @Override
    public String displayName0() {
        return paused ? "Builder Paused" : "Building " + name;
    }

    @Override
    public Optional<Integer> getMinLayer() {
        if (Baritone.settings().buildInLayers.value) {
            return Optional.of(this.layer);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Integer> getMaxLayer() {
        if (Baritone.settings().buildInLayers.value) {
            return Optional.of(this.stopAtHeight);
        }
        return Optional.empty();
    }

    private List<BlockState> approxPlaceable(int size) {
        List<BlockState> result = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            ItemStack stack = ctx.player().getInventory().items.get(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) {
                result.add(Blocks.AIR.defaultBlockState());
                continue;
            }
            // <toxic cloud>
            BlockState itemState = ((BlockItem) stack.getItem())
                .getBlock()
                .getStateForPlacement(
                    new BlockPlaceContext(
                        new UseOnContext(ctx.world(), ctx.player(), InteractionHand.MAIN_HAND, stack, new BlockHitResult(new Vec3(ctx.player().position().x, ctx.player().position().y, ctx.player().position().z), Direction.UP, ctx.playerFeet(), false)) {}
                    )
                );
            if (itemState != null) {
                result.add(itemState);
            } else {
                result.add(Blocks.AIR.defaultBlockState());
            }
            // </toxic cloud>
        }
        return result;
    }

    /**
     * Whether the item currently occupying a slot could place the desired block, ignoring the orientation
     * it happens to have. Used for hotbar moves: a log in inventory with axis=Y can still place an
     * axis=X log by clicking a different face, so requiring an exact blockstate here would leave
     * directional blocks stuck in the inventory forever.
     */
    private static boolean canPlaceBlock(BlockState itemState, BlockState desired) {
        if (itemState.getBlock() instanceof AirBlock) {
            return false;
        }
        return itemState.getBlock() == desired.getBlock();
    }

    private static boolean sameBlockstate(BlockState first, BlockState second) {
        if (first.getBlock() != second.getBlock()) {
            return false;
        }
        boolean ignoreDirection = Baritone.settings().buildIgnoreDirection.value;
        List<String> ignoredProps = Baritone.settings().buildIgnoreProperties.value;
        Map<Property<?>, Comparable<?>> map1 = first.getValues();
        Map<Property<?>, Comparable<?>> map2 = second.getValues();
        for (Property<?> prop : map1.keySet()) {
            if (!Objects.equals(map1.get(prop), map2.get(prop))
                    && !WORLD_DETERMINED_PROPS.contains(prop)
                    && !(ignoreDirection && ORIENTATION_PROPS.contains(prop))
                    && !ignoredProps.contains(prop.getName())) {
                return false;
            }
        }
        return true;
    }

    private static boolean containsBlockState(Collection<BlockState> states, BlockState state) {
        for (BlockState testee : states) {
            if (sameBlockstate(testee, state)) {
                return true;
            }
        }
        return false;
    }

    private static boolean valid(BlockState current, BlockState desired, boolean itemVerify) {
        if (desired == null) {
            return true;
        }
        if (current.getBlock() instanceof LiquidBlock && Baritone.settings().okIfWater.value) {
            return true;
        }
        if (current.getBlock() instanceof AirBlock && desired.getBlock() instanceof AirBlock) {
            return true;
        }
        if (current.getBlock() instanceof AirBlock && Baritone.settings().okIfAir.value.contains(desired.getBlock())) {
            return true;
        }
        if (desired.getBlock() instanceof AirBlock && Baritone.settings().buildIgnoreBlocks.value.contains(current.getBlock())) {
            return true;
        }
        if (!(current.getBlock() instanceof AirBlock) && Baritone.settings().buildIgnoreExisting.value && !itemVerify) {
            return true;
        }
        if (Baritone.settings().buildValidSubstitutes.value.getOrDefault(desired.getBlock(), Collections.emptyList()).contains(current.getBlock()) && !itemVerify) {
            return true;
        }
        if (current.equals(desired)) {
            return true;
        }
        return sameBlockstate(current, desired);
    }

    public class BuilderCalculationContext extends CalculationContext {

        private final List<BlockState> placeable;
        private final ISchematic schematic;
        private final int originX;
        private final int originY;
        private final int originZ;

        public BuilderCalculationContext() {
            super(BuilderProcess.this.baritone, true); // wew lad
            this.placeable = approxPlaceable(9);
            this.schematic = BuilderProcess.this.schematic;
            this.originX = origin.getX();
            this.originY = origin.getY();
            this.originZ = origin.getZ();

            this.jumpPenalty += 10;
            this.backtrackCostFavoringCoefficient = 1;
        }

        private BlockState getSchematic(int x, int y, int z, BlockState current) {
            if (schematic.inSchematic(x - originX, y - originY, z - originZ, current)) {
                return schematic.desiredState(x - originX, y - originY, z - originZ, current, BuilderProcess.this.approxPlaceable);
            } else {
                return null;
            }
        }

        @Override
        public double costOfPlacingAt(int x, int y, int z, BlockState current) {
            if (isPossiblyProtected(x, y, z) || !worldBorder.canPlaceAt(x, z)) { // make calculation fail properly if we can't build
                return COST_INF;
            }
            BlockState sch = getSchematic(x, y, z, current);
            if (sch != null) {
                // TODO this can return true even when allowPlace is off.... is that an issue?
                if (sch.getBlock() instanceof AirBlock) {
                    // we want this to be air, but they're asking if they can place here
                    // this won't be a schematic block, this will be a throwaway
                    return placeBlockCost * Baritone.settings().placeIncorrectBlockPenaltyMultiplier.value; // we're going to have to break it eventually
                }
                if (placeable.contains(sch)) {
                    return 0; // thats right we gonna make it FREE to place a block where it should go in a structure
                    // no place block penalty at all 😎
                    // i'm such an idiot that i just tried to copy and paste the epic gamer moment emoji too
                    // get added to unicode when?
                }
                if (isCreativeBuild() && giveable(sch) && !creativeUnGiveable.contains(sch.getBlock())) {
                    // creative: we can /give the exact block on demand, so it is as good as held
                    return 0;
                }
                if (!hasThrowaway) {
                    return COST_INF;
                }
                // we want it to be something that we don't have
                // even more of a pain to place something wrong
                return placeBlockCost * 1.5 * Baritone.settings().placeIncorrectBlockPenaltyMultiplier.value;
            } else {
                if (hasThrowaway || isCreativeBuild()) {
                    return placeBlockCost;
                } else {
                    return COST_INF;
                }
            }
        }

        @Override
        public double breakCostMultiplierAt(int x, int y, int z, BlockState current) {
            if ((!allowBreak && !allowBreakAnyway.contains(current.getBlock())) || isPossiblyProtected(x, y, z)) {
                return COST_INF;
            }
            BlockState sch = getSchematic(x, y, z, current);
            if (sch != null) {
                if (sch.getBlock() instanceof AirBlock) {
                    // it should be air
                    // regardless of current contents, we can break it
                    return 1;
                }
                // it should be a real block
                // is it already that block?
                if (valid(bsi.get0(x, y, z), sch, false)) {
                    return Baritone.settings().breakCorrectBlockPenaltyMultiplier.value;
                } else {
                    // can break if it's wrong
                    // would be great to return less than 1 here, but that would actually make the cost calculation messed up
                    // since we're breaking a block, if we underestimate the cost, then it'll fail when it really takes the correct amount of time
                    return 1;

                }
                // TODO do blocks in render distace only?
                // TODO allow breaking blocks that we have a tool to harvest and immediately place back?
            } else {
                return 1; // why not lol
            }
        }
    }
}
