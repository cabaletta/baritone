package adris.altoclef.tasks.misc;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.eventbus.EventBus;
import adris.altoclef.eventbus.Subscription;
import adris.altoclef.eventbus.events.ChatMessageEvent;
import adris.altoclef.eventbus.events.GameOverlayEvent;
import adris.altoclef.tasks.DoToClosestBlockTask;
import adris.altoclef.tasks.InteractWithBlockTask;
import adris.altoclef.tasks.construction.DestroyBlockTask;
import adris.altoclef.tasks.construction.PlaceStructureBlockTask;
import adris.altoclef.tasks.entity.KillEntitiesTask;
import adris.altoclef.tasks.movement.DefaultGoToDimensionTask;
import adris.altoclef.tasks.movement.GetToBlockTask;
import adris.altoclef.tasks.movement.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.LookHelper;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import adris.altoclef.util.time.TimerGame;
import baritone.api.utils.input.Input;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.SleepingChatScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import org.apache.commons.lang3.ArrayUtils;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class PlaceBedAndSetSpawnTask extends Task {

    private final TimerGame _regionScanTimer = new TimerGame(9);
    private final Vec3i BED_CLEAR_SIZE = new Vec3i(3, 2, 3);
    private final Vec3i[] BED_BOTTOM_PLATFORM = new Vec3i[]{
            new Vec3i(0, -1, 0),
            new Vec3i(1, -1, 0),
            new Vec3i(2, -1, 0),
            new Vec3i(0, -1, -1),
            new Vec3i(1, -1, -1),
            new Vec3i(2, -1, -1),
            new Vec3i(0, -1, 1),
            new Vec3i(1, -1, 1),
            new Vec3i(2, -1, 1)
    };
    // Kinda silly but who knows if we ever want to change it.
    private final Vec3i BED_PLACE_STAND_POS = new Vec3i(0, 0, 1);
    private final Vec3i BED_PLACE_POS = new Vec3i(1, 0, 1);
    private final Vec3i[] BED_PLACE_POS_OFFSET = new Vec3i[]{
            BED_PLACE_POS,
            BED_PLACE_POS.north(),
            BED_PLACE_POS.south(),
            BED_PLACE_POS.east(),
            BED_PLACE_POS.west(),
            BED_PLACE_POS.add(-1, 0, 1),
            BED_PLACE_POS.add(1, 0, 1),
            BED_PLACE_POS.add(-1, 0, -1),
            BED_PLACE_POS.add(1, 0, -1),
            BED_PLACE_POS.north(2),
            BED_PLACE_POS.south(2),
            BED_PLACE_POS.east(2),
            BED_PLACE_POS.west(2),
            BED_PLACE_POS.add(-2, 0, 1),
            BED_PLACE_POS.add(-2, 0, 2),
            BED_PLACE_POS.add(2, 0, 1),
            BED_PLACE_POS.add(2, 0, 2),
            BED_PLACE_POS.add(-2, 0, -1),
            BED_PLACE_POS.add(-2, 0, -2),
            BED_PLACE_POS.add(2, 0, -1),
            BED_PLACE_POS.add(2, 0, -2)
    };
    private final Direction BED_PLACE_DIRECTION = Direction.UP;
    private final TimerGame _bedInteractTimeout = new TimerGame(5);
    private final TimerGame _inBedTimer = new TimerGame(1);
    private final MovementProgressChecker _progressChecker = new MovementProgressChecker();
    private boolean _stayInBed;
    private BlockPos _currentBedRegion;
    private BlockPos _currentStructure, _currentBreak;
    private boolean _spawnSet;
    private Subscription<ChatMessageEvent> _respawnPointSetMessageCheck;
    private Subscription<GameOverlayEvent> _respawnFailureMessageCheck;
    private boolean _sleepAttemptMade;
    private boolean _wasSleeping;
    private BlockPos _bedForSpawnPoint;

    public PlaceBedAndSetSpawnTask() {

    }

    /**
     * Sets the _stayInBed flag to true and returns the current instance.
     *
     * @return the current instance
     */
    public PlaceBedAndSetSpawnTask stayInBed() {
        this._stayInBed = true;
        return this;
    }

    /**
     * This method is called when the bot is started.
     * It tracks bed blocks, resets progress checkers, and sets up behaviors for placing and breaking blocks near beds.
     * It also resets variables for sleep handling and subscribes to events for respawn point and failure messages.
     */
    @Override
    protected void onStart(AltoClef mod) {
        try {
            // Track bed blocks
            mod.getBlockTracker().trackBlock(ItemHelper.itemsToBlocks(ItemHelper.BED));

            // Push the current behaviour
            mod.getBehaviour().push();

            // Reset progress checker
            _progressChecker.reset();

            // Reset current bed region
            _currentBedRegion = null;

            // Avoid placing blocks near bed
            mod.getBehaviour().avoidBlockPlacing(pos -> {
                if (_currentBedRegion != null) {
                    BlockPos start = _currentBedRegion;
                    BlockPos end = _currentBedRegion.add(BED_CLEAR_SIZE);
                    return start.getX() <= pos.getX() && pos.getX() < end.getX()
                            && start.getZ() <= pos.getZ() && pos.getZ() < end.getZ()
                            && start.getY() <= pos.getY() && pos.getY() < end.getY();
                }
                return false;
            });

            // Avoid breaking blocks near bed
            mod.getBehaviour().avoidBlockBreaking(pos -> {
                if (_currentBedRegion != null) {
                    for (Vec3i baseOffs : BED_BOTTOM_PLATFORM) {
                        BlockPos base = _currentBedRegion.add(baseOffs);
                        if (base.equals(pos)) return true;
                    }
                }
                // Don't ever break beds. If one exists, we will sleep in it.
                if (mod.getWorld() != null) {
                    return mod.getWorld().getBlockState(pos).getBlock() instanceof BedBlock;
                }
                return false;
            });

            // Reset variables for sleep handling
            _spawnSet = false;
            _sleepAttemptMade = false;
            _wasSleeping = false;

            // Subscribe to respawn point set message event
            _respawnPointSetMessageCheck = EventBus.subscribe(ChatMessageEvent.class, evt -> {
                String msg = evt.toString();
                if (msg.contains("Respawn point set")) {
                    _spawnSet = true;
                    _inBedTimer.reset();
                }
            });

            // Subscribe to respawn failure message event
            _respawnFailureMessageCheck = EventBus.subscribe(GameOverlayEvent.class, evt -> {
                final String[] NEUTRAL_MESSAGES = new String[]{
                        "You can sleep only at night",
                        "You can only sleep at night",
                        "You may not rest now; there are monsters nearby"
                };
                for (String checkMessage : NEUTRAL_MESSAGES) {
                    if (evt.message.contains(checkMessage)) {
                        if (!_sleepAttemptMade) {
                            _bedInteractTimeout.reset();
                        }
                        _sleepAttemptMade = true;
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void resetSleep() {
        _spawnSet = false;
        _sleepAttemptMade = false;
        _wasSleeping = false;
    }

    @Override
    protected Task onTick(AltoClef mod) {
        // Summary:
        // If we find a bed nearby, sleep in it.
        // Otherwise, place bed:
        //      Collect bed if we don't have one.
        //      Find a 3x2x1 region and clear it
        //      Stand on the edge of the long (3) side
        //      Place on the middle block, reliably placing the bed.
        if (!_progressChecker.check(mod) && _currentBedRegion != null) {
            _progressChecker.reset();
            Debug.logMessage("Searching new bed region.");
            _currentBedRegion = null;
        }
        if (mod.getPlayer().isTouchingWater() && mod.getItemStorage().hasItem(ItemHelper.BED)) {
            setDebugState("We are in water. Wandering");
            _currentBedRegion = null;
            return new TimeoutWanderTask();
        }
        if (WorldHelper.isInNetherPortal(mod)) {
            setDebugState("We are in nether portal. Wandering");
            _currentBedRegion = null;
            return new TimeoutWanderTask();
        }
        // We cannot do this anywhere but the overworld.
        if (WorldHelper.getCurrentDimension() != Dimension.OVERWORLD) {
            setDebugState("Going to the overworld first.");
            return new DefaultGoToDimensionTask(Dimension.OVERWORLD);
        }
        Screen screen = MinecraftClient.getInstance().currentScreen;
        if (screen instanceof SleepingChatScreen) {
            _progressChecker.reset();
            setDebugState("Sleeping...");
            _wasSleeping = true;
            //Debug.logMessage("Closing sleeping thing");
            _spawnSet = true;
            return null;
        }

        if (_sleepAttemptMade) {
            if (_bedInteractTimeout.elapsed()) {
                Debug.logMessage("Failed to get \"Respawn point set\" message or sleeping, assuming that this bed already contains our spawn.");
                _spawnSet = true;
                return null;
            }
        }
        Block[] copperBlocks = ItemHelper.itemsToBlocks(ItemHelper.COPPER_BLOCKS);
        Optional<BlockPos> nearestBed = mod.getBlockTracker().getNearestTracking(ItemHelper.itemsToBlocks(ItemHelper.BED));
        if (nearestBed.isPresent() && !WorldHelper.isAir(mod, nearestBed.get())) {
            Block bed = mod.getWorld().getBlockState(nearestBed.get()).getBlock();
            for (Block CopperBlock : copperBlocks) {
                Block blockBelow = mod.getWorld().getBlockState(nearestBed.get().down()).getBlock();
                if (blockBelow == CopperBlock) {
                    mod.getBlockTracker().requestBlockUnreachable(nearestBed.get(), 0);
                    return null;
                }
            }
            if (nearestBed.get().isWithinDistance(mod.getPlayer().getPos(), 40)) {
                // Check if there are monsters nearby
                Vec3d vec3d = Vec3d.ofBottomCenter(nearestBed.get());
                List<HostileEntity> list = mod.getWorld().getEntitiesByClass(HostileEntity.class, new Box(vec3d.getX() - 8.0, vec3d.getY() - 5.0, vec3d.getZ() - 8.0, vec3d.getX() + 8.0, vec3d.getY() + 5.0, vec3d.getZ() + 8.0), (entity) -> entity.isAngryAt(mod.getPlayer()));
                if (!list.isEmpty()) {
                    for (HostileEntity entity : list) {
                        setDebugState("Killing monster nearby");
                        Predicate<Entity> valid = e -> e == entity;
                        return new KillEntitiesTask(valid, entity.getClass());
                    }
                }
                // Sleep in the nearest bed
                setDebugState("Going to bed to sleep...");
                return new DoToClosestBlockTask(toSleepIn -> {
                    boolean closeEnough = toSleepIn.isWithinDistance(mod.getPlayer().getPos(), 3);
                    if (closeEnough) {
                        // why 0.2? I'm tired.
                        Vec3d centerBed = new Vec3d(toSleepIn.getX() + 0.5, toSleepIn.getY() + 0.2, toSleepIn.getZ() + 0.5);
                        BlockHitResult hit = LookHelper.raycast(mod.getPlayer(), centerBed, 6);
                        // TODO: Kinda ugly, but I'm tired and fixing for the 2nd attempt speedrun so I will fix this block later
                        closeEnough = false;
                        if (hit.getType() != HitResult.Type.MISS) {
                            // At this poinAt, if we miss, we probably are close enough.
                            BlockPos p = hit.getBlockPos();
                            if (ArrayUtils.contains(new Block[]{bed}, mod.getWorld().getBlockState(p).getBlock())) {
                                // We have a bed!
                                closeEnough = true;
                            }
                        }
                    }
                    _bedForSpawnPoint = WorldHelper.getBedHead(mod, toSleepIn);
                    if (_bedForSpawnPoint == null) {
                        _bedForSpawnPoint = toSleepIn;
                    }
                    if (!closeEnough) {
                        try {
                            Direction face = mod.getWorld().getBlockState(toSleepIn).get(BedBlock.FACING);
                            Direction side = face.rotateYClockwise();
                    /*
                    BlockPos targetMove = toSleepIn.offset(side).offset(side); // Twice, juust to make sure...
                     */
                            return new GetToBlockTask(_bedForSpawnPoint.add(side.getVector()));
                        } catch (IllegalArgumentException e) {
                            // If bed is not loaded, this will happen. In that case just get to the bed first.
                        }
                    } else {
                        _inBedTimer.reset();
                    }
                    if (closeEnough) {
                        _inBedTimer.reset();
                    }
                    // Keep track of where our spawn point is
                    _progressChecker.reset();
                    return new InteractWithBlockTask(_bedForSpawnPoint);
                }, bed);
            }
        }
        if (_currentBedRegion != null) {
            for (Vec3i BedPlacePos : BED_PLACE_POS_OFFSET) {
                Block getBlock = mod.getWorld().getBlockState(_currentBedRegion.add(BedPlacePos)).getBlock();
                if (getBlock instanceof BedBlock) {
                    mod.getBlockTracker().addBlock(getBlock, _currentBedRegion.add(BedPlacePos));
                    break;
                }
            }
        }
        // Get a bed if we don't have one.
        if (!mod.getItemStorage().hasItem(ItemHelper.BED)) {
            setDebugState("Getting a bed first");
            return TaskCatalogue.getItemTask("bed", 1);
        }

        if (_currentBedRegion == null) {
            if (_regionScanTimer.elapsed()) {
                Debug.logMessage("Rescanning for nearby bed place position...");
                _regionScanTimer.reset();
                _currentBedRegion = this.locateBedRegion(mod, mod.getPlayer().getBlockPos());
            }
        }
        if (_currentBedRegion == null) {
            setDebugState("Searching for spot to place bed, wandering...");
            return new TimeoutWanderTask();
        }

        // Clear and make bed foundation

        for (Vec3i baseOffs : BED_BOTTOM_PLATFORM) {
            BlockPos toPlace = _currentBedRegion.add(baseOffs);
            if (!WorldHelper.isSolid(mod, toPlace)) {
                _currentStructure = toPlace;
                break;
            }
        }

        outer:
        for (int dx = 0; dx < BED_CLEAR_SIZE.getX(); ++dx) {
            for (int dz = 0; dz < BED_CLEAR_SIZE.getZ(); ++dz) {
                for (int dy = 0; dy < BED_CLEAR_SIZE.getY(); ++dy) {
                    BlockPos toClear = _currentBedRegion.add(dx, dy, dz);
                    if (WorldHelper.isSolid(mod, toClear)) {
                        _currentBreak = toClear;
                        break outer;
                    }
                }
            }
        }

        if (_currentStructure != null) {
            if (WorldHelper.isSolid(mod, _currentStructure)) {
                _currentStructure = null;
            } else {
                setDebugState("Placing structure for bed");
                return new PlaceStructureBlockTask(_currentStructure);
            }
        }
        if (_currentBreak != null) {
            if (!WorldHelper.isSolid(mod, _currentBreak)) {
                _currentBreak = null;
            } else {
                setDebugState("Clearing region for bed");
                return new DestroyBlockTask(_currentBreak);
            }
        }

        BlockPos toStand = _currentBedRegion.add(BED_PLACE_STAND_POS);
        // Our bed region is READY TO BE PLACED
        if (!mod.getPlayer().getBlockPos().equals(toStand)) {
            return new GetToBlockTask(toStand);
        }

        BlockPos toPlace = _currentBedRegion.add(BED_PLACE_POS);
        if (mod.getWorld().getBlockState(toPlace.offset(BED_PLACE_DIRECTION)).getBlock() instanceof BedBlock) {
            setDebugState("Waiting to rescan + find bed that we just placed. Should be almost instant.");
            _progressChecker.reset();
            return null;
        }
        setDebugState("Placing bed...");

        setDebugState("Filling in Portal");
        if (!_progressChecker.check(mod)) {
            mod.getClientBaritone().getPathingBehavior().cancelEverything();
            mod.getClientBaritone().getPathingBehavior().forceCancel();
            mod.getClientBaritone().getExploreProcess().onLostControl();
            mod.getClientBaritone().getCustomGoalProcess().onLostControl();
            _progressChecker.reset();
        }

        // Scoot backwards if we're trying to place and fail
        if (thisOrChildSatisfies(task -> {
            if (task instanceof InteractWithBlockTask intr)
                return intr.getClickStatus() == InteractWithBlockTask.ClickResponse.CLICK_ATTEMPTED;
            return false;
        })) {
            mod.getInputControls().tryPress(Input.MOVE_BACK);
        }
        return new InteractWithBlockTask(new ItemTarget("bed", 1), BED_PLACE_DIRECTION, toPlace.offset(BED_PLACE_DIRECTION.getOpposite()), false);
    }

    /**
     * Perform cleanup tasks when the task is stopped.
     *
     * @param mod The AltoClef mod instance
     * @param interruptTask The task that caused the interruption
     */
    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        // Stop tracking beds
        if (mod.getBlockTracker() != null) {
            mod.getBlockTracker().stopTracking(ItemHelper.itemsToBlocks(ItemHelper.BED));
        }

        // Pop the behaviour stack
        if (mod.getBehaviour() != null) {
            mod.getBehaviour().pop();
        }

        // Unsubscribe from respawn point set message
        if (_respawnPointSetMessageCheck != null) {
            EventBus.unsubscribe(_respawnPointSetMessageCheck);
        }

        // Unsubscribe from respawn failure message
        if (_respawnFailureMessageCheck != null) {
            EventBus.unsubscribe(_respawnFailureMessageCheck);
        }
    }

    /**
     * Check if the given task is equal to this task.
     *
     * @param other The other task to compare with
     * @return True if the other task is equal to this task, false otherwise
     */
    @Override
    protected boolean isEqual(Task other) {
        if (other == null) {
            return false;
        }
        // Check if the other task is an instance of PlaceBedAndSetSpawnTask
        return other instanceof PlaceBedAndSetSpawnTask;
    }

    /**
     * Returns a debug string representing the action of placing a bed nearby and resetting the spawn point.
     *
     * @return the debug string
     */
    @Override
    protected String toDebugString() {
        try {
            // Place a bed nearby and reset spawn point
            return "Placing a bed nearby + resetting spawn point";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error generating debug string";
        }
    }

    /**
     * Check if the player is finished with the sleeping task.
     *
     * @param mod the AltoClef mod
     * @return true if the player is finished, false otherwise
     */
    @Override
    public boolean isFinished(AltoClef mod) {
        // Check if we are in the overworld
        if (WorldHelper.getCurrentDimension() != Dimension.OVERWORLD) {
            return true; // If not in the overworld, the player is considered finished
        }

        // Check if player is sleeping
        boolean isSleeping = mod.getPlayer() != null && mod.getPlayer().isSleeping();

        // Check if timer has elapsed
        boolean timerElapsed = _inBedTimer != null && _inBedTimer.elapsed();

        // Check if spawnpoint is set, player is not sleeping, and timer has elapsed
        boolean isFinished = _spawnSet && !isSleeping && timerElapsed;

        return isFinished;
    }

    /**
     * Returns the position of the bed where the player last slept.
     * @return the position of the bed where the player last slept
     * @throws IllegalStateException if the bed position has not been set
     */
    public BlockPos getBedSleptPos() {
        if (_bedForSpawnPoint != null) {
            return _bedForSpawnPoint;
        } else {
            throw new IllegalStateException("Bed position has not been set");
        }
    }

    /**
     * Check if the spawn point is set.
     * @return true if the spawn point is set, false otherwise
     */
    public boolean isSpawnSet() {
        return _spawnSet;
    }

    /**
     * Locates the closest good position within a certain range of the origin.
     *
     * @param mod the mod instance
     * @param origin the origin position
     * @return the closest good position within the range of the origin
     */
    private BlockPos locateBedRegion(AltoClef mod, BlockPos origin) {
        final int SCAN_RANGE = 10;

        BlockPos closestGood = null; // The closest good position found so far
        double closestDist = Double.POSITIVE_INFINITY; // The closest distance found so far

        for (int x = origin.getX() - SCAN_RANGE; x < origin.getX() + SCAN_RANGE; ++x) {
            for (int z = origin.getZ() - SCAN_RANGE; z < origin.getZ() + SCAN_RANGE; ++z) {
                for (int y = origin.getY() - SCAN_RANGE; y < origin.getY() + SCAN_RANGE; ++y) {
                    BlockPos attemptPos = new BlockPos(x, y, z);
                    double distance = attemptPos.getSquaredDistance(mod.getPlayer().getPos());

                    if (distance > closestDist) {
                        continue; // Skip if the distance is greater than the closest distance found so far
                    }

                    if (isGoodPosition(mod, attemptPos)) {
                        closestGood = attemptPos;
                        closestDist = distance;
                    }
                }
            }
        }

        return closestGood;
    }

    /**
     * Checks if the position is suitable for placing an object.
     *
     * @param mod the AltoClef object
     * @param pos the position to check
     * @return true if the position is suitable, false otherwise
     */
    private boolean isGoodPosition(AltoClef mod, BlockPos pos) {
        final BlockPos BED_CLEAR_SIZE = new BlockPos(2, 1, 2);

        // Iterate over the area around the position
        for (int x = 0; x < BED_CLEAR_SIZE.getX(); ++x) {
            for (int y = 0; y < BED_CLEAR_SIZE.getY(); ++y) {
                for (int z = 0; z < BED_CLEAR_SIZE.getZ(); ++z) {
                    BlockPos checkPos = pos.add(x, y, z);
                    if (!isGoodToPlaceInsideOrClear(mod, checkPos)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     * Check if it is good to place inside or clear at the given position
     * @param mod the AltoClef object
     * @param pos the position to check
     * @return true if it is good to place inside or clear, false otherwise
     */
    private boolean isGoodToPlaceInsideOrClear(AltoClef mod, BlockPos pos) {
        // Define the offsets to check around the position
        final Vec3i[] CHECK = {
                new Vec3i(0, 0, 0),
                new Vec3i(-1, 0, 0),
                new Vec3i(1, 0, 0),
                new Vec3i(0, 1, 0),
                new Vec3i(0, -1, 0),
                new Vec3i(0, 0, 1),
                new Vec3i(0, 0, -1)
        };

        // Check each offset
        for (Vec3i offset : CHECK) {
            BlockPos newPos = pos.add(offset);
            if (!isGoodAsBorder(mod, newPos)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Check if the given position is suitable as a border.
     *
     * @param mod The AltoClef object
     * @param pos The position to check
     * @return true if the position is suitable as a border, false otherwise
     */
    private boolean isGoodAsBorder(AltoClef mod, BlockPos pos) {
        // Check if the position is solid
        if (WorldHelper.isSolid(mod, pos)) {
            // If solid, check if it can be broken
            return WorldHelper.canBreak(mod, pos);
        } else {
            // If not solid, check if it is air
            return WorldHelper.isAir(mod, pos);
        }
    }
}
