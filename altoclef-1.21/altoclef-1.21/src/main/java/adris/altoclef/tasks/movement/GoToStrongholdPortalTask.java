package adris.altoclef.tasks.movement;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.helpers.WorldHelper;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;


public class GoToStrongholdPortalTask extends Task {

    private final LocateStrongholdCoordinatesTask _locateCoordsTask;
    private final int _targetEyes;
    private final int MINIMUM_EYES = 12;
    private BlockPos _strongholdCoordinates;

    public GoToStrongholdPortalTask(int targetEyes) {
        _targetEyes = targetEyes;
        _strongholdCoordinates = null;
        _locateCoordsTask = new LocateStrongholdCoordinatesTask(targetEyes);
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.getBlockTracker().trackBlock(Blocks.END_PORTAL_FRAME);
    }

    @Override
    protected Task onTick(AltoClef mod) {
        /*
            If we don't know where stronghold is, find out where stronghold is.
            If we do know where stronghold is, fast travel there
            If there search it
         */
        if (_strongholdCoordinates == null) {
            _strongholdCoordinates = _locateCoordsTask.getStrongholdCoordinates().orElse(null);
            if (_strongholdCoordinates == null) {
                if (mod.getItemStorage().getItemCount(Items.ENDER_EYE) < MINIMUM_EYES && mod.getEntityTracker().itemDropped(Items.ENDER_EYE)) {
                    setDebugState("Picking up dropped eye");
                    return new PickupDroppedItemTask(Items.ENDER_EYE, MINIMUM_EYES);
                }
                setDebugState("Triangulating stronghold...");
                return _locateCoordsTask;
            }
        }
        // Search stone brick chunks, but while we're wandering, go to the nether
        setDebugState("Searching for Stronghold...");
        return new SearchChunkForBlockTask(Blocks.STONE_BRICKS) {
            @Override
            protected Task onTick(AltoClef mod) {
                if (WorldHelper.getCurrentDimension() != Dimension.OVERWORLD) {
                    return getWanderTask(mod);
                }
                return super.onTick(mod);
            }

            @Override
            protected Task getWanderTask(AltoClef mod) {
                return new FastTravelTask(_strongholdCoordinates, 300, true);
            }
        };
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getBlockTracker().stopTracking(Blocks.END_PORTAL_FRAME);
    }

    @Override
    protected boolean isEqual(Task other) {
        return other instanceof GoToStrongholdPortalTask;
    }

    @Override
    protected String toDebugString() {
        return "Locating Stronghold";
    }
}