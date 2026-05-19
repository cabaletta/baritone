package adris.altoclef.tasks.construction.compound;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.InteractWithBlockTask;
import adris.altoclef.tasks.construction.ClearLiquidTask;
import adris.altoclef.tasks.construction.DestroyBlockTask;
import adris.altoclef.tasks.construction.PlaceStructureBlockTask;
import adris.altoclef.tasks.movement.GetToBlockTask;
import adris.altoclef.tasks.movement.TimeoutWanderTask;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.time.TimerGame;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;

import java.util.Optional;

@SuppressWarnings("ALL")
@Deprecated
/**
 * NOTE: This is unreliable, I'd give it roughly 70% odds of success at best.
 * The problem here is that the water source ocassionally spills everywhere, and this causes
 * Baritone to get stuck
 * Use "ConstructNetherPortalBucketTask" which is much more methodical and doesn't have this pitfall.
 */
public class ConstructNetherPortalSpeedrunTask extends adris.altoclef.tasksystem.Task {

    // The "portalable" region includes the portal (1 x 6 x 4 structure) and an outer buffer for its construction and water bullshit.
    // The "portal origin relative to region" corresponds to the portal origin with respect to the "portalable" region (see _portalOrigin).
    // This can only really be explained visually, sorry!
    private static final Vec3i PORTALABLE_REGION_SIZE = new Vec3i(4, 6, 6);
    // Destroy these blocks too.
    private static final Vec3i[] PORTALABLE_REGION_EXTRA = new Vec3i[]{
            // Bottom two slots
            new Vec3i(0, -1, 0),
            new Vec3i(0, -1, 1),
            // Water entry to reduce extra water
            new Vec3i(2, -1, 0),
            new Vec3i(2, -1, 1)
    };
    private static final Vec3i PORTAL_ORIGIN_RELATIVE_TO_REGION = new Vec3i(1, 0, 2);
    // Relative to portal origin
    private static final Vec3i[] PORTAL_CONSTRUCTION_FRAME = new Vec3i[]{
            // Left upside down L: Starting at bottom
            new Vec3i(1, 0, -1),
            new Vec3i(1, 1, -1),
            new Vec3i(1, 2, -1),
            new Vec3i(1, 3, -1),
            new Vec3i(0, 3, -1),

            // T/right side extension
            new Vec3i(1, 0, 0),
            new Vec3i(1, 0, 1),
            new Vec3i(1, 1, 1),
            new Vec3i(1, 0, 2),
            // Bonus right side nudge for blocking water
            new Vec3i(1, 1, 2),
            new Vec3i(1, 2, 2),
            new Vec3i(2, 0, 2),

            // Bottom part below the bottom 2 obsidian
            new Vec3i(0, -2, 0),
            new Vec3i(0, -2, 1)
    };
    // How the lava will be placed to make the portal. (place relative to origin AND what direction it is placed on)
    // !! Also represents the ORDER at which the lava will be placed.
    private static final LavaTarget[] PORTAL_FRAME_LAVA = new LavaTarget[]{
            // Left side
            new LavaTarget(0, 0, -1, Direction.fromVector(-1, 0, 0)),
            new LavaTarget(0, 1, -1, Direction.fromVector(-1, 0, 0)),
            new LavaTarget(0, 2, -1, Direction.fromVector(0, 1, 0)),
            // Right side
            new LavaTarget(0, 0, 2, Direction.fromVector(-1, 0, 0)),
            new LavaTarget(0, 1, 2, Direction.fromVector(0, 1, 0)),
            new LavaTarget(0, 2, 2, Direction.fromVector(0, 1, 0)),
            // Bottom
            new LavaTarget(0, -1, 0, Direction.fromVector(0, 1, 0)),
            new LavaTarget(0, -1, 1, Direction.fromVector(0, 1, 0)),
            // Top
            new LavaTarget(0, 3, 0, Direction.fromVector(0, 0, 1)),
            new LavaTarget(0, 3, 1, Direction.fromVector(0, 0, 1))
    };
    private static final Vec3i[] PORTAL_INTERIOR = new Vec3i[]{
            new Vec3i(0, 0, 0),
            new Vec3i(0, 1, 0),
            new Vec3i(0, 2, 0),
            new Vec3i(0, 0, 1),
            new Vec3i(0, 1, 1),
            new Vec3i(0, 2, 1)
    };
    private static final Vec3i WATER_SOURCE_ORIGIN = new Vec3i(1, 3, 0);
    private final TimerGame _lavaSearchTimer = new TimerGame(5);
    private final adris.altoclef.tasksystem.Task _collectLavaTask = TaskCatalogue.getItemTask(Items.LAVA_BUCKET, 1);
    private final TimerGame _placeLavaWeCanBreakAgainTimer = new TimerGame(5);
    private final TimerGame _specialBottomCaseCloserTimer = new TimerGame(10);
    private final TimerGame _specialBottomCaseCloserTimerForcePlace = new TimerGame(5);
    // Corresponds to the LEFT most side of where the player will stand on the portal.
    private BlockPos _portalOrigin = null;
    private boolean _isPlacingLiquid;
    private boolean _portalFrameBuilt;
    private BlockPos _destroyTarget = null;
    private boolean _firstSearch = false;

    @Override
    protected void onStart(AltoClef mod) {
        _isPlacingLiquid = false;
        _portalFrameBuilt = false;
        mod.getBlockTracker().trackBlock(Blocks.LAVA);
        mod.getBehaviour().push();
        //mod.getConfigState().setAllowWalkThroughFlowingWater(true);
        // Avoid breaking frame.
        mod.getBehaviour().avoidBlockBreaking((block) -> {
            if (_portalOrigin != null) {

                for (Vec3i framePosRelative : PORTAL_CONSTRUCTION_FRAME) {
                    BlockPos framePos = _portalOrigin.add(framePosRelative);
                    if (block.equals(framePos)) return true;
                }
                // If we're the water source block...
                if (block.equals(_portalOrigin.add(WATER_SOURCE_ORIGIN))) {
                    if (MinecraftClient.getInstance().world.getBlockState(block).getBlock() == Blocks.WATER)
                        return true;
                }
            }
            return false;
        });

        _lavaSearchTimer.reset();
        _firstSearch = true;

    }

    @Override
    protected adris.altoclef.tasksystem.Task onTick(AltoClef mod) {

        // Pre-affirmed thing
        mod.getBehaviour().setAllowWalkThroughFlowingWater(false);

        // Get bucket if we don't have one.
        if (!mod.getItemStorage().hasItem(Items.BUCKET) && !mod.getItemStorage().hasItem(Items.WATER_BUCKET) && !mod.getItemStorage().hasItem(Items.LAVA_BUCKET)) {
            setDebugState("Getting bucket");
            return TaskCatalogue.getItemTask(Items.BUCKET, 1);
        }

        // Get flint & steel if we don't have one
        if (!mod.getItemStorage().hasItem(Items.FLINT_AND_STEEL)) {
            setDebugState("Getting flint & steel");
            return TaskCatalogue.getItemTask(Items.FLINT_AND_STEEL, 1);
        }

        boolean needsToLookForPortal = _portalOrigin == null;
        if (needsToLookForPortal) {
            if (!mod.getItemStorage().hasItem(Items.WATER_BUCKET)) {
                setDebugState("Getting water");
                return TaskCatalogue.getItemTask(Items.WATER_BUCKET, 1);
            }

            boolean foundSpot = false;

            if (_firstSearch || _lavaSearchTimer.elapsed()) {
                _firstSearch = false;
                _lavaSearchTimer.reset();
                Debug.logMessage("(Searching for lava lake with portalable spot nearby...)");
                BlockPos lavaPos = findLavaLake(mod);
                if (lavaPos != null) {
                    // We have a lava lake, set our portal origin!
                    BlockPos foundPortalRegion = getPortalableRegion(lavaPos, mod.getPlayer().getBlockPos(), new Vec3i(-1, 0, 0), PORTALABLE_REGION_SIZE, 20);
                    if (foundPortalRegion == null) {
                        Debug.logWarning("Failed to find portalable region nearby. Consider increasing the search timeout range");
                    } else {
                        _portalOrigin = foundPortalRegion.add(PORTAL_ORIGIN_RELATIVE_TO_REGION);
                        foundSpot = true;
                    }
                } else {
                    Debug.logMessage("(lava lake not found)");
                }
            }

            if (!foundSpot) {
                setDebugState("(timeout: Looking for lava lake)");
                return new TimeoutWanderTask(100);
            }
        }

        // Now... Build the foundation

        if (!_portalFrameBuilt) {
            BlockPos requiredFrame = getRequiredFrameLeft();
            if (requiredFrame != null) {
                setDebugState("Creating construction frame");
                return new PlaceStructureBlockTask(requiredFrame);
            }
        }

        // Clear the spot
        if (!_portalFrameBuilt && !_isPlacingLiquid) {
            BlockPos toDestroy = getPortalRegionUnclearedBlock();
            if (toDestroy != null) {
                setDebugState("Clearing Portal Region");
                _placeLavaWeCanBreakAgainTimer.reset();
                _destroyTarget = toDestroy;
                return new DestroyBlockTask(toDestroy);//new ClearRegionTask(getPortalRegionCorner(), getPortalRegionCorner().add(PORTALABLE_REGION_SIZE));
            }
        }

        // Place our water source
        if (!_portalFrameBuilt) {
            BlockPos waterSourcePos = _portalOrigin.add(WATER_SOURCE_ORIGIN);
            if (MinecraftClient.getInstance().world.getBlockState(waterSourcePos).getBlock() != Blocks.WATER) {
                if (!mod.getItemStorage().hasItem(Items.WATER_BUCKET)) {
                    setDebugState("Getting water");
                    return TaskCatalogue.getItemTask(Items.WATER_BUCKET, 1);
                }
                setDebugState("Placing water: " + waterSourcePos);
                _isPlacingLiquid = true;
                // Place water
                // south corresponds to +z
                Direction placeWaterFrom = Direction.SOUTH;
                return new InteractWithBlockTask(new ItemTarget(Items.WATER_BUCKET, 1), placeWaterFrom, waterSourcePos.offset(placeWaterFrom.getOpposite()), true);
            }
        }
        //_isPlacingLiquid = false;


        // Place lava
        for (LavaTarget lavaTarget : PORTAL_FRAME_LAVA) {
            //mod.getConfigState().setAllowWalkThroughFlowingWater(true);
            if (!lavaTarget.isSatisfied(_portalOrigin)) {

                // Get lava if we don't have it.
                if (!mod.getItemStorage().hasItem(Items.LAVA_BUCKET)) {
                    setDebugState("Getting Lava");
                    _isPlacingLiquid = true;
                    return _collectLavaTask;
                }

                if (_placeLavaWeCanBreakAgainTimer.elapsed()) {
                    _isPlacingLiquid = false;
                    _placeLavaWeCanBreakAgainTimer.reset();
                }
                _portalFrameBuilt = false;
                // Walk through water to get to the bottom, we have to get there to further guarantee placement.
                mod.getBehaviour().setAllowWalkThroughFlowingWater(lavaTarget.isBelow());

                // Special case: Get close enough to our base if we're placing in the bad zone
                if (lavaTarget.isBelow()) {
                    BlockPos posClose = _portalOrigin.add(lavaTarget.where).add(-1, 1, 0);
                    // If we're not right at that point and we're registered to keep fighting for it, go for it.
                    if (!mod.getPlayer().getBlockPos().equals(posClose)) {
                        if (!_specialBottomCaseCloserTimer.elapsed()) {
                            setDebugState("Special Case: Getting near bottom lava to place it.");
                            _specialBottomCaseCloserTimerForcePlace.reset();
                            return new GetToBlockTask(posClose, false);
                        } else {
                            if (_specialBottomCaseCloserTimerForcePlace.elapsed()) {
                                _specialBottomCaseCloserTimer.reset();
                            }
                        }
                    }
                }

                _isPlacingLiquid = true;
                setDebugState("Placing Obsidian");
                return lavaTarget.placeTask(_portalOrigin, lavaTarget.isBelow());
            }
        }
        mod.getBehaviour().setAllowWalkThroughFlowingWater(false);

        _portalFrameBuilt = true;

        // Delete water source
        BlockPos waterSourcePos = _portalOrigin.add(WATER_SOURCE_ORIGIN);
        BlockState waterSource = MinecraftClient.getInstance().world.getBlockState(waterSourcePos);
        if (waterSource.getBlock() == Blocks.WATER) {
            setDebugState("Removing water source");

            return new ClearLiquidTask(waterSourcePos);
        }

        // Clear inside of portal
        for (Vec3i offs : PORTAL_INTERIOR) {
            BlockPos p = _portalOrigin.add(offs);
            if (!MinecraftClient.getInstance().world.getBlockState(p).isAir()) {
                setDebugState("Clearing inside of portal");
                return new DestroyBlockTask(p);
            }
        }
        setDebugState("Flinting and Steeling");

        // Flint and steel it baby
        return new InteractWithBlockTask(new ItemTarget(Items.FLINT_AND_STEEL, 1), Direction.UP, _portalOrigin.down(), true);

        // Pick up water
        // Clear inner portal area
        // Flint and we're done.

        // If no portal position current:
        //      - Get water if we don't have it.
        //      - Timer. Run "findLavaLake (rename to SCAN) and find the nearest lava lake
        //      - If we found a lava lake, find a spot nearby for the portal that is big enough (figure out the size) and set the portal position to be
        //        the center of that.
        // Otherwise, we have a portal position and must begin grabbing lava

        // - Find lava lake/area with a lot of lava nearby
        // - Clear an area nearby (that doesn't have obsidian or lava in it, for now)
        // - Construct the speedrun structure (empty spot for the bottom, upside down L left, upside down T right, water flow that goes all the way down)
        // - Once structure is done (and the flowing water is all the way down), begin placing lava at each point in the portal. (Bonus: If the lava spreads even a little, grab it back and try again before abandoning this portal)
        // - Once the portal is constructed, pick up the original water source block. Wait for flowing water to no longer exist all the way down. (and have a timeout or something)
        // - Light portal.

    }

    @Override
    protected void onStop(AltoClef mod, adris.altoclef.tasksystem.Task interruptTask) {
        mod.getBlockTracker().stopTracking(Blocks.LAVA);
        mod.getBehaviour().pop();
    }

    @Override
    protected boolean isEqual(adris.altoclef.tasksystem.Task other) {
        return other instanceof ConstructNetherPortalSpeedrunTask;
    }

    @Override
    protected String toDebugString() {
        return "Construct Nether Portal (the cool way)";
    }


    // Scans to find the nearest lava lake (collection of lava bigger than 12 blocks)
    private BlockPos findLavaLake(AltoClef mod) {
        BlockPos nearestLake = null;
        if (mod.getBlockTracker().isTracking(Blocks.LAVA)) {
            Optional<BlockPos> lavas = mod.getBlockTracker().getNearestTracking(Blocks.LAVA);
            if (lavas.isPresent()) {
                int depth = getNumberOfBlocksAdjacent(lavas.get());
                if (depth != 0) {
                    Debug.logMessage("Found with depth " + depth);
                    if (depth >= 12) {
                        nearestLake = lavas.get();
                    }
                }
            }
        }
        return nearestLake;
    }

    // Used to flood-scan for blocks of lava.
    private int getNumberOfBlocksAdjacent(BlockPos origin) {
        // Base case: We hit a non-full lava block.
        assert MinecraftClient.getInstance().world != null;
        BlockState s = MinecraftClient.getInstance().world.getBlockState(origin);
        if (s.getBlock() != Blocks.LAVA) {
            return 0;
        } else {
            // We may not be a full lava block
            if (!s.getFluidState().isStill()) return 0;
            int level = s.getFluidState().getLevel();
            //Debug.logMessage("TEST LEVEL: " + level + ", " + height);
            // Only accept FULL SOURCE BLOCKS
            if (level != 8) return 0;
        }

        BlockPos[] toCheck = new BlockPos[]{origin.north(), origin.south(), origin.east(), origin.west(), origin.up(), origin.down()};

        int bonus = 0;
        for (BlockPos check : toCheck) {
            // This block is new! Explore out from it.
            bonus += getNumberOfBlocksAdjacent(check);
        }

        return bonus + 1;
    }

    // Get a region that a portal can fit into
    private BlockPos getPortalableRegion(BlockPos lava, BlockPos playerPos, Vec3i sizeOffset, Vec3i sizeAllocation, int timeoutRange) {
        Vec3i[] directions = new Vec3i[]{new Vec3i(1, 0, 0), new Vec3i(-1, 0, 0), new Vec3i(0, 0, 1), new Vec3i(0, 0, -1)};

        double minDistanceToPlayer = Double.POSITIVE_INFINITY;
        BlockPos bestPos = null;

        for (Vec3i direction : directions) {

            // Inch along
            for (int offs = 1; offs < timeoutRange; ++offs) {

                Vec3i offset = new Vec3i(direction.getX() * offs, direction.getY() * offs, direction.getZ() * offs);

                boolean found = true;
                // check for collision with lava in box
                moveAlongLine:
                // We have an extra buffer to make sure we never break a block NEXT to lava.
                for (int dx = -1; dx < sizeAllocation.getX() + 1; ++dx) {
                    for (int dz = -1; dz < sizeAllocation.getZ() + 1; ++dz) {
                        for (int dy = -1; dy < sizeAllocation.getY(); ++dy) {
                            BlockPos toCheck = lava.add(offset).add(sizeOffset).add(dx, dy, dz);
                            BlockState state = MinecraftClient.getInstance().world.getBlockState(toCheck);
                            if (state.getBlock() == Blocks.LAVA || state.getBlock() == Blocks.BEDROCK) {
                                found = false;
                                break moveAlongLine;
                            }
                        }
                    }
                }

                if (found) {
                    BlockPos foundBoxCorner = lava.add(offset).add(sizeOffset);
                    double sqDistance = foundBoxCorner.getSquaredDistance(playerPos);
                    if (sqDistance < minDistanceToPlayer) {
                        minDistanceToPlayer = sqDistance;
                        bestPos = foundBoxCorner;
                    }
                    break;
                }
            }

        }

        return bestPos;
    }

    private BlockPos getPortalRegionUnclearedBlock() {
        if (_destroyTarget != null) {
            BlockState state = MinecraftClient.getInstance().world.getBlockState(_destroyTarget);
            Block block = state.getBlock();
            if (state.isAir() || block == Blocks.WATER) {
                _destroyTarget = null;
            }
        }
        if (_destroyTarget != null) return _destroyTarget;
        // Region
        for (int dx = 0; dx < PORTALABLE_REGION_SIZE.getX(); ++dx) {
            for (int dz = 0; dz < PORTALABLE_REGION_SIZE.getZ(); ++dz) {
                for (int dy = 0; dy < PORTALABLE_REGION_SIZE.getY(); ++dy) {
                    BlockPos toCheck = getPortalRegionCorner().add(dx, dy, dz);
                    if (shouldBeDestroyed(toCheck)) return toCheck;
                }
            }
        }
        // Extra places
        for (Vec3i relativeToOrigin : PORTALABLE_REGION_EXTRA) {
            BlockPos toCheck = _portalOrigin.add(relativeToOrigin);
            if (shouldBeDestroyed(toCheck)) return toCheck;
        }

        return null;
    }

    private boolean shouldBeDestroyed(BlockPos toCheck) {
        BlockState state = MinecraftClient.getInstance().world.getBlockState(toCheck);
        Block block = state.getBlock();

        // Ignore air
        if (state.isAir()) {
            return false;
        }

        // If it's water ignore it.
        if (block == Blocks.WATER) return false;

        // If we're supposed to have structures here, ignore.
        Vec3i relativeToOrigin = toCheck.subtract(_portalOrigin);//new Vec3i(dx - PORTAL_ORIGIN_RELATIVE_TO_REGION.getX(), dy  - PORTAL_ORIGIN_RELATIVE_TO_REGION.getY(), dz - PORTAL_ORIGIN_RELATIVE_TO_REGION.getZ());
        boolean foundFrame = false;
        for (Vec3i framePos : PORTAL_CONSTRUCTION_FRAME) {
            if (framePos.equals(relativeToOrigin)) {
                return false;
            }
        }
        for (LavaTarget frame : PORTAL_FRAME_LAVA) {
            if (frame.where.equals(relativeToOrigin) && (block == Blocks.LAVA || block == Blocks.OBSIDIAN)) {
                return false;
            }
        }
        return true;
    }

    private BlockPos getRequiredFrameLeft() {
        for (Vec3i framePos : PORTAL_CONSTRUCTION_FRAME) {
            BlockPos worldPos = _portalOrigin.add(framePos);
            if (!MinecraftClient.getInstance().world.getBlockState(worldPos).isSolidBlock(MinecraftClient.getInstance().world, worldPos)) {
                return worldPos;
            }
        }
        return null;
    }

    private BlockPos getPortalRegionCorner() {
        if (_portalOrigin == null) return null;
        return _portalOrigin.subtract(PORTAL_ORIGIN_RELATIVE_TO_REGION);
    }

    private static class LavaTarget {
        public Vec3i where;
        public Direction fromWhere;

        public LavaTarget(int dx, int dy, int dz, Direction fromWhere) {
            where = new Vec3i(dx, dy, dz);
            this.fromWhere = fromWhere;
        }

        public boolean isBelow() {
            return where.getY() == -1;
        }

        // Place lava at a point, but from a direction.
        private adris.altoclef.tasksystem.Task placeTask(BlockPos portalOrigin, boolean below) {
            BlockPos placeAt = portalOrigin.add(where);
            BlockPos placeOn = placeAt.offset(fromWhere.getOpposite());
            // Clear first
            BlockState b = MinecraftClient.getInstance().world.getBlockState(placeAt);
            if (!b.isAir() && b.getBlock() != Blocks.WATER) {
                return new DestroyBlockTask(placeAt);
            }

            // Place lava there
            return new InteractWithBlockTask(new ItemTarget(Items.LAVA_BUCKET, 1), fromWhere, placeOn, below);
        }

        private boolean isSatisfied(BlockPos portalOrigin) {
            Block b = MinecraftClient.getInstance().world.getBlockState(portalOrigin.add(where)).getBlock();
            return b == Blocks.OBSIDIAN || b == Blocks.LAVA;
        }
    }

}
