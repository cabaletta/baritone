package adris.altoclef.tasks.resources;

import adris.altoclef.AltoClef;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.InteractWithBlockTask;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasks.movement.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.MiningRequirement;
import adris.altoclef.util.helpers.ItemHelper;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

public class CollectStrippedLogTask extends ResourceTask {
    private static final Item[] _axes = new Item[]{Items.WOODEN_AXE, Items.STONE_AXE, Items.GOLDEN_AXE, Items.IRON_AXE,
            Items.DIAMOND_AXE, Items.NETHERITE_AXE};
    private final Item[] _strippedLogs;
    private final Item[] _strippableLogs;
    private final int _targetCount;

    public CollectStrippedLogTask(Item[] strippedLogs, Item[] strippableLogs, int count) {
        super(new ItemTarget(strippedLogs, count));
        _strippedLogs = strippedLogs;
        _strippableLogs = strippableLogs;
        _targetCount = count;
    }

    public CollectStrippedLogTask(int count) {
        this(ItemHelper.STRIPPED_LOGS, ItemHelper.STRIPPABLE_LOGS, count);
    }

    public CollectStrippedLogTask(Item strippedLogs, Item strippableLogs, int count) {
        this(new Item[]{strippedLogs}, new Item[]{strippableLogs}, count);
    }

    public CollectStrippedLogTask(Item strippedLog, int count) {
        this(strippedLog, ItemHelper.strippedToLogs(strippedLog), count);
    }

    @Override
    protected boolean shouldAvoidPickingUp(AltoClef mod) {
        return false;
    }

    @Override
    protected void onResourceStart(AltoClef mod) {
        mod.getBlockTracker().trackBlock(ItemHelper.itemsToBlocks(_strippedLogs));
        mod.getBlockTracker().trackBlock(ItemHelper.itemsToBlocks(_strippableLogs));
    }

    @Override
    protected Task onResourceTick(AltoClef mod) {
        if (!mod.getItemStorage().hasItem(_axes)) {
            setDebugState("Getting axe for stripping");
            return TaskCatalogue.getItemTask(Items.WOODEN_AXE, 1);
        }
        if (mod.getItemStorage().getItemCount(_strippedLogs) < _targetCount) {
            Optional<BlockPos> strippedLogBlockPos = mod.getBlockTracker().getNearestTracking(ItemHelper.itemsToBlocks(_strippedLogs));
            if (strippedLogBlockPos.isPresent()) {
                setDebugState("Getting stripped log");
                return new MineAndCollectTask(new ItemTarget(_strippedLogs), ItemHelper.itemsToBlocks(_strippedLogs), MiningRequirement.HAND);
            }
        }
        Optional<BlockPos> strippableLogBlockPos = mod.getBlockTracker().getNearestTracking(ItemHelper.itemsToBlocks(_strippableLogs));
        if (strippableLogBlockPos.isPresent()) {
            setDebugState("Stripping log");
            return new InteractWithBlockTask(new ItemTarget(_axes), strippableLogBlockPos.get());
        }
        setDebugState("Searching log");
        return new TimeoutWanderTask();
    }

    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {
        mod.getBlockTracker().stopTracking(ItemHelper.itemsToBlocks(_strippedLogs));
        mod.getBlockTracker().stopTracking(ItemHelper.itemsToBlocks(_strippableLogs));
    }

    @Override
    protected boolean isEqualResource(ResourceTask other) {
        if (other instanceof CollectStrippedLogTask task) {
            return task._targetCount == _targetCount;
        }
        return false;
    }

    @Override
    protected String toDebugStringName() {
        return "Collect Stripped Log";
    }
}
