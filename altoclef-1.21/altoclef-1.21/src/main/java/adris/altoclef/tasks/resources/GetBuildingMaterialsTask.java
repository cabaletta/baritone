package adris.altoclef.tasks.resources;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.MiningRequirement;
import adris.altoclef.util.helpers.StorageHelper;
import net.minecraft.item.Item;

public class GetBuildingMaterialsTask extends Task {
    private final int _count;

    public GetBuildingMaterialsTask(int count) {
        _count = count;
    }

    @Override
    protected void onStart(AltoClef mod) {

    }

    @Override
    protected Task onTick(AltoClef mod) {
        Item[] throwaways = mod.getModSettings().getThrowawayItems(mod, true);
        return new MineAndCollectTask(new ItemTarget[]{new ItemTarget(throwaways, _count)}, MiningRequirement.WOOD);
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {

    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof GetBuildingMaterialsTask task) {
            return task._count == _count;
        }
        return false;
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return StorageHelper.getBuildingMaterialCount(mod) >= _count;
    }

    @Override
    protected String toDebugString() {
        return "Collecting " + _count + " building materials.";
    }
}
