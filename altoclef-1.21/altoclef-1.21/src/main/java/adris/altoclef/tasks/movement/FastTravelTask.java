package adris.altoclef.tasks.movement;

import adris.altoclef.AltoClef;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.construction.compound.ConstructNetherPortalObsidianTask;
import adris.altoclef.tasks.speedrun.MarvionBeatMinecraftTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.time.TimerGame;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.lang3.NotImplementedException;

import java.util.Objects;
import java.util.Optional;

@SuppressWarnings("ConstantConditions")
public class FastTravelTask extends Task {

    // Consider ourselves "mostly close enough" when we're in this range of our ideal portal building location in the nether.
    private static final double IN_NETHER_CLOSE_ENOUGH_THRESHOLD = 15;

    // Collect flint+steel and diamond pickaxe before entering. Or just walk.
    private final boolean _collectPortalMaterialsIfAbsent;
    private final BlockPos _target;
    private final Integer _threshold;
    // If we fail to move to the precise center after we're "close enough" to our threshold, just call it quits and place the portal.
    private final TimerGame _attemptToMoveToIdealNetherCoordinateTimeout = new TimerGame(15);
    private boolean _forceOverworldWalking;
    private Task _goToOverworldTask;

    /**
     * Creates fast travel task instance.
     *
     * @param overworldTarget                target location in overworld after post travel
     * @param threshold                      Threshold for when to fast travel vs when to walk
     * @param collectPortalMaterialsIfAbsent if we don't have (10 obsidian or a diamond pickaxe) and (a flint and steel or fire charge), collect these items. Otherwise just walk the whole way.
     */
    public FastTravelTask(BlockPos overworldTarget, Integer threshold, boolean collectPortalMaterialsIfAbsent) {
        _target = overworldTarget;
        _threshold = null;
        _collectPortalMaterialsIfAbsent = collectPortalMaterialsIfAbsent;
    }

    /**
     * Creates fast travel task instance
     *
     * @param overworldTarget                target location in overworld after post travel
     *                                       Bot will use nether travel based on the threshold value in settings.
     * @param collectPortalMaterialsIfAbsent if we don't have (10 obsidian or a diamond pickaxe) and (a flint and steel or fire charge), collect these items. Otherwise just walk the whole way.
     */
    public FastTravelTask(BlockPos overworldTarget, boolean collectPortalMaterialsIfAbsent) {
        this(overworldTarget, null, collectPortalMaterialsIfAbsent);
    }

    @Override
    protected void onStart(AltoClef mod) {
        _goToOverworldTask = new EnterNetherPortalTask(new ConstructNetherPortalObsidianTask(), Dimension.OVERWORLD, checkPos -> {
            // Make sure the portal we enter is NOT close to our exit portal...
            Optional<BlockPos> lastPortal = mod.getMiscBlockTracker().getLastUsedNetherPortal(Dimension.NETHER);
            return lastPortal.isEmpty() || !WorldHelper.inRangeXZ(lastPortal.get(), checkPos, 3);
        });
    }

    @Override
    protected Task onTick(AltoClef mod) {

        BlockPos netherTarget = new BlockPos(_target.getX() / 8, _target.getY(), _target.getZ() / 8);

        boolean canBuildPortal = mod.getItemStorage().hasItem(Items.DIAMOND_PICKAXE) || mod.getItemStorage().getItemCount(Items.OBSIDIAN) >= 10;
        boolean canLightPortal = mod.getItemStorage().hasItem(Items.FLINT_AND_STEEL, Items.FIRE_CHARGE);

        // EDGE CASE: We die in the nether, stop force walking, we want to start over.
        if (MinecraftClient.getInstance().currentScreen instanceof DeathScreen) {
            _forceOverworldWalking = false;
        }

        switch (WorldHelper.getCurrentDimension()) {
            case OVERWORLD -> {
                _attemptToMoveToIdealNetherCoordinateTimeout.reset();
                // WALK
                if (_forceOverworldWalking || WorldHelper.inRangeXZ(mod.getPlayer(), _target, getOverworldThreshold(mod))) {
                    _forceOverworldWalking = true;
                    setDebugState("Walking: We're close enough to our target");
                    return new GetToBlockTask(_target);
                }
                // SUPPLIES
                if (!canBuildPortal || !canLightPortal) {
                    if (_collectPortalMaterialsIfAbsent) {
                        setDebugState("Collecting portal building materials");
                        if (!canBuildPortal)
                            return TaskCatalogue.getItemTask(Items.DIAMOND_PICKAXE, 1);
                        if (!canLightPortal)
                            return TaskCatalogue.getItemTask(Items.FLINT_AND_STEEL, 1);
                    } else {
                        setDebugState("Walking: We don't have portal building materials");
                        return new GetToBlockTask(_target);
                    }
                }
                // GO TO NETHER
                return new DefaultGoToDimensionTask(Dimension.NETHER);
            }
            case NETHER -> {

                if (!_forceOverworldWalking) {
                    // After walking a bit, the moment we go back into the overworld, walk again.
                    Optional<BlockPos> portalEntrance = mod.getMiscBlockTracker().getLastUsedNetherPortal(Dimension.NETHER);
                    if (portalEntrance.isPresent() && !portalEntrance.get().isWithinDistance(mod.getPlayer().getPos(), 3)) {
                        _forceOverworldWalking = true;
                    }
                }

                // If we're going to the overworld, keep going.
                if (_goToOverworldTask.isActive() && !_goToOverworldTask.isFinished(mod)) {
                    setDebugState("Going back to overworld");
                    if (MarvionBeatMinecraftTask.getConfig().renderDistanceManipulation) {
                        MinecraftClient.getInstance().options.getViewDistance().setValue(32);
                    }
                    return _goToOverworldTask;
                }

                // PICKUP DROPPED STUFF if we need it
                if (mod.getItemStorage().getItemCount(Items.OBSIDIAN) < 10) {
                    setDebugState("Making sure we can build our portal");
                    return TaskCatalogue.getItemTask(Items.OBSIDIAN, 10);
                }
                if (!canLightPortal && mod.getEntityTracker().itemDropped(Items.FLINT_AND_STEEL, Items.FIRE_CHARGE)) {
                    setDebugState("Making sure we can light our portal");
                    return new PickupDroppedItemTask(new ItemTarget(new Item[]{Items.FLINT_AND_STEEL, Items.FIRE_CHARGE}), true);
                }

                if (WorldHelper.inRangeXZ(mod.getPlayer(), netherTarget, IN_NETHER_CLOSE_ENOUGH_THRESHOLD) &&
                        mod.getClientBaritone().getPathingBehavior().isSafeToCancel()) {
                    // If we're precisely at our target XZ or if we've tried long enough
                    if ((mod.getPlayer().getBlockX() == netherTarget.getX() && mod.getPlayer().getBlockZ() == netherTarget.getZ()) || _attemptToMoveToIdealNetherCoordinateTimeout.elapsed()) {
                        return _goToOverworldTask;
                    }
                }
                _attemptToMoveToIdealNetherCoordinateTimeout.reset();
                setDebugState("Traveling to ideal coordinates");
                return new GetToXZTask(netherTarget.getX(), netherTarget.getZ());
            }
            case END -> {
                setDebugState("Why are you running this here?");
                return new DefaultGoToDimensionTask(Dimension.OVERWORLD);
            }
        }
        throw new NotImplementedException("Unimplemented dimension: " + WorldHelper.getCurrentDimension());
        /*
            if we're in the overworld:
                if we're outside of TRAVEL_THRESHHOLD and NOT forcefully walking:
                    if we need to collect extra flint & steel (or fire charge) AND a diamond pickaxe:
                        collect
                    else
                        walk
                else:
                    force walk
                    walk
                GO TO NETHER
            if we're in the nether:

                if we were building the portal:
                    keep building

                if we drop a diamond pickaxe, pick it up
                if we have no flint and steel & fire charge and dropped any, pick it up
                if we dropped obsidian and have less than 10, pick it up

                if we're close enough to our calculated XZ coordinates:
                    run
                    build portal
                go to calculated nether coordinates
            else (we're in the end, highly unlikely but may as well)
                go to overworld
         */

    }

    private int getOverworldThreshold(AltoClef mod) {
        int threshold;
        //noinspection ReplaceNullCheck
        if (_threshold == null) {
            threshold = mod.getModSettings().getNetherFastTravelWalkingRange();
        } else {
            threshold = _threshold;
        }
        // We should never leave the nether and STILL be outside our walk zone.
        threshold = Math.max((int) (IN_NETHER_CLOSE_ENOUGH_THRESHOLD * 8) + 32, threshold);
        // Nether portals less than 16 blocks point to the same portal (128 overworld), so make sure we don't redo work. Just a redundancy check
        threshold = Math.max(16 * 8, threshold);
        return threshold;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {

    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof FastTravelTask task) {
            return task._target.equals(_target) && task._collectPortalMaterialsIfAbsent == _collectPortalMaterialsIfAbsent && Objects.equals(task._threshold, _threshold);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Fast travelling to " + _target.toShortString();
    }
}
