package adris.altoclef.tasks.movement;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.helpers.WorldHelper;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.BiomeKeys;

public class LocateDesertTempleTask extends Task {

    private BlockPos _finalPos;

    @Override
    protected void onStart(AltoClef mod) {
        // Track desert pyramid blocks
        mod.getBlockTracker().trackBlock(Blocks.STONE_PRESSURE_PLATE);
    }

    @Override
    protected Task onTick(AltoClef mod) {
        BlockPos desertTemplePos = WorldHelper.getADesertTemple(mod);
        if (desertTemplePos != null) {
            _finalPos = desertTemplePos.up(14);
        }
        if (_finalPos != null) {
            setDebugState("Going to found desert temple");
            return new GetToBlockTask(_finalPos, false);
        }
        return new SearchWithinBiomeTask(BiomeKeys.DESERT);
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getBlockTracker().stopTracking(Blocks.STONE_PRESSURE_PLATE);
    }

    @Override
    protected boolean isEqual(Task other) {
        return other instanceof LocateDesertTempleTask;
    }

    @Override
    protected String toDebugString() {
        return "Searchin' for temples";
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return mod.getPlayer().getBlockPos().equals(_finalPos);
    }
}
