package adris.altoclef.tasks.stupid;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.eventbus.EventBus;
import adris.altoclef.eventbus.Subscription;
import adris.altoclef.eventbus.events.BlockBrokenEvent;
import adris.altoclef.tasks.DoToClosestBlockTask;
import adris.altoclef.tasks.construction.PlaceBlockTask;
import adris.altoclef.tasks.movement.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.ItemHelper;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Optional;

public class ReplaceBlocksTask extends Task {

    // We won't be asked to collect more materials than this at a single time.
    private static final int MAX_MATERIALS_NEEDED_AT_A_TIME = 64;

    private final Block[] _toFind;
    private final ItemTarget _toReplace;

    private final BlockPos _from;
    private final BlockPos _to;
    private final Deque<BlockPos> _forceReplace = new ArrayDeque<>();
    private Task _collectMaterialsTask;
    private Task _replaceTask;
    private Subscription<BlockBrokenEvent> _blockBrokenSubscription;

    public ReplaceBlocksTask(ItemTarget toReplace, BlockPos from, BlockPos to, Block... toFind) {
        _toFind = toFind;
        _toReplace = toReplace;
        _from = from;
        _to = to;
    }

    public ReplaceBlocksTask(ItemTarget toReplace, Block... toFind) {
        this(toReplace, null, null, toFind);
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.getBehaviour().push();
        mod.getBehaviour().addProtectedItems(_toReplace.getMatches());
        // TODO: Bug: We may want to replace a block that's considered a CONSTRUCTION block.
        // If that's the case, we are in trouble.

        mod.getBlockTracker().trackBlock(_toFind);

        //_forceReplace.clear();
        _blockBrokenSubscription = EventBus.subscribe(BlockBrokenEvent.class, evt -> {
            if (evt.player.equals(MinecraftClient.getInstance().player)) {
                if (isWithinRange(evt.blockPos)) {
                    boolean wasAReplacable = ArrayUtils.contains(_toFind, evt.blockState.getBlock());
                    if (wasAReplacable) {
                        Debug.logMessage("ADDED REPLACEABLE FORCE: " + evt.blockPos);
                        _forceReplace.push(evt.blockPos);
                    } else {
                        Debug.logMessage("Destroyed a non replaceable block (delete this print if things are good lol)");
                    }
                } else {
                    Debug.logMessage("Not within range (TODO: DELETE THIS PRINT)");
                }
            } else {
                Debug.logMessage("IN-EQUAL PLAYER (delete this print if things are good lol)");
            }
        });
    }

    @Override
    protected Task onTick(AltoClef mod) {

        if (_collectMaterialsTask != null && _collectMaterialsTask.isActive() && !_collectMaterialsTask.isFinished(mod)) {
            setDebugState("Collecting materials...");
            return _collectMaterialsTask;
        }

        if (_replaceTask != null && _replaceTask.isActive() && !_replaceTask.isFinished(mod)) {
            setDebugState("Replacing a block");
            return _replaceTask;
        }

        // Get to replace item
        if (!mod.getItemStorage().hasItem(_toReplace.getMatches())) {
            int need = 0;
            for (Block toFind : _toFind) {
                if (mod.getBlockTracker().isTracking(toFind)) {
                    Optional<BlockPos> location = mod.getBlockTracker().getNearestTracking(toFind);
                    if (location.isPresent() && isWithinRange(location.get()) && need < MAX_MATERIALS_NEEDED_AT_A_TIME) {
                        need++;
                    }
                }
            }
            if (need == 0) {
                setDebugState("No replaceable blocks found, wandering.");
                return new TimeoutWanderTask();
            }
            _collectMaterialsTask = TaskCatalogue.getItemTask(new ItemTarget(_toReplace, need));
            return _collectMaterialsTask;
            //return TaskCatalogue.getItemTask(_toReplace);
        }

        Block[] blocksToPlace = ItemHelper.itemsToBlocks(_toReplace.getMatches());

        // If we are forced to replace something we broke, do it now.
        while (!_forceReplace.isEmpty()) {
            BlockPos toReplace = _forceReplace.pop();
            if (!ArrayUtils.contains(blocksToPlace, mod.getWorld().getBlockState(toReplace).getBlock())) {
                _replaceTask = new PlaceBlockTask(toReplace, blocksToPlace, false, true);
                return _replaceTask;
            }
        }

        // Now replace
        setDebugState("Searching for blocks to replace...");
        return new DoToClosestBlockTask(whereToPlace -> {
            _replaceTask = new PlaceBlockTask(whereToPlace, blocksToPlace, false, true);
            return _replaceTask;
        },
                this::isWithinRange,
                _toFind
        );
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        EventBus.unsubscribe(_blockBrokenSubscription);
        mod.getBehaviour().pop();
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof ReplaceBlocksTask task) {
            return task._toReplace.equals(_toReplace) && Arrays.equals(task._toFind, _toFind);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Replacing " + Arrays.toString(_toFind) + " with " + _toReplace;
    }

    private boolean isWithinRange(BlockPos pos) {
        if (_from != null) {
            if (_from.getX() > pos.getX() || _from.getY() > pos.getY() || _from.getZ() > pos.getZ()) {
                return false;
            }
        }
        if (_to != null) {
            return _to.getX() >= pos.getX() && _to.getY() >= pos.getY() && _to.getZ() >= pos.getZ();
        }
        return true;
    }
}
