package adris.altoclef.tasks.movement;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.construction.compound.ConstructNetherPortalBucketTask;
import adris.altoclef.tasks.construction.compound.ConstructNetherPortalObsidianTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.helpers.WorldHelper;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

/**
 * Some generic tasks require us to go to the nether/overworld/end.
 * <p>
 * The user should be able to specify how this should be done in settings
 * (ex, craft a new portal from scratch or check particular portal areas first or highway or whatever)
 */
public class DefaultGoToDimensionTask extends Task {

    private final Dimension _target;
    // Cached to keep build properties alive if this task pauses/resumes.
    private final Task _cachedNetherBucketConstructionTask = new ConstructNetherPortalBucketTask();

    public DefaultGoToDimensionTask(Dimension target) {
        _target = target;
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.getBlockTracker().trackBlock(Blocks.NETHER_PORTAL);
    }

    @Override
    protected Task onTick(AltoClef mod) {
        if (WorldHelper.getCurrentDimension() == _target) return null;

        switch (_target) {
            case OVERWORLD:
                switch (WorldHelper.getCurrentDimension()) {
                    case NETHER:
                        return goToOverworldFromNetherTask(mod);
                    case END:
                        return goToOverworldFromEndTask(mod);
                }
                break;
            case NETHER:
                switch (WorldHelper.getCurrentDimension()) {
                    case OVERWORLD:
                        return goToNetherFromOverworldTask(mod);
                    case END:
                        // First go to the overworld
                        return goToOverworldFromEndTask(mod);
                }
                break;
            case END:
                switch (WorldHelper.getCurrentDimension()) {
                    case NETHER:
                        // First go to the overworld
                        return goToOverworldFromNetherTask(mod);
                    case OVERWORLD:
                        return goToEndTask(mod);
                }
                break;
        }

        setDebugState(WorldHelper.getCurrentDimension() + " -> " + _target + " is NOT IMPLEMENTED YET!");
        return null;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getBlockTracker().stopTracking(Blocks.NETHER_PORTAL);
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof DefaultGoToDimensionTask task) {
            return task._target == _target;
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Going to dimension: " + _target + " (default version)";
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return WorldHelper.getCurrentDimension() == _target;
    }

    private Task goToOverworldFromNetherTask(AltoClef mod) {
        if (netherPortalIsClose(mod)) {
            setDebugState("Going to nether portal");
            return new EnterNetherPortalTask(Dimension.NETHER);
        }

        Optional<BlockPos> closest = mod.getMiscBlockTracker().getLastUsedNetherPortal(Dimension.NETHER);
        if (closest.isPresent()) {
            setDebugState("Going to last nether portal pos");
            return new GetToBlockTask(closest.get());
        }

        setDebugState("Constructing nether portal with obsidian");
        return new ConstructNetherPortalObsidianTask();
    }

    private Task goToOverworldFromEndTask(AltoClef mod) {
        setDebugState("TODO: Go to center portal (at 0,0). If it doesn't exist, kill ender dragon lol");
        return null;
    }

    private Task goToNetherFromOverworldTask(AltoClef mod) {
        if (netherPortalIsClose(mod)) {
            setDebugState("Going to nether portal");
            return new EnterNetherPortalTask(Dimension.NETHER);
        }
        return switch (mod.getModSettings().getOverworldToNetherBehaviour()) {
            case BUILD_PORTAL_VANILLA -> _cachedNetherBucketConstructionTask;
            case GO_TO_HOME_BASE -> new GetToBlockTask(mod.getModSettings().getHomeBasePosition());
        };
    }

    private Task goToEndTask(AltoClef mod) {
        // Keep in mind that getting to the end requires going to the nether first.
        setDebugState("TODO: Get to End, Same as BeatMinecraft");
        return null;
    }

    private boolean netherPortalIsClose(AltoClef mod) {
        if (mod.getBlockTracker().anyFound(Blocks.NETHER_PORTAL)) {
            Optional<BlockPos> closest = mod.getBlockTracker().getNearestTracking(mod.getPlayer().getPos(), Blocks.NETHER_PORTAL);
            return closest.isPresent() && closest.get().isWithinDistance(mod.getPlayer().getPos(), 2000);
        }
        return false;
    }

    public enum OVERWORLD_TO_NETHER_BEHAVIOUR {
        BUILD_PORTAL_VANILLA,
        GO_TO_HOME_BASE
    }
}
