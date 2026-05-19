package adris.altoclef.tasks.misc;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.construction.DestroyBlockTask;
import adris.altoclef.tasks.container.LootContainerTask;
import adris.altoclef.tasksystem.Task;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

import java.util.List;

public class LootDesertTempleTask extends Task {
    public final Vec3i[] CHEST_POSITIONS_RELATIVE = {
            new Vec3i(2, 0, 0),
            new Vec3i(-2, 0, 0),
            new Vec3i(0, 0, 2),
            new Vec3i(0, 0, -2)
    };
    private final BlockPos _temple;
    private final List<Item> _wanted;
    private Task _lootTask;
    private short _looted = 0;

    public LootDesertTempleTask(BlockPos temple, List<Item> wanted) {
        _temple = temple;
        _wanted = wanted;
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.getClientBaritoneSettings().blocksToAvoid.value.add(Blocks.STONE_PRESSURE_PLATE);
    }

    @Override
    protected Task onTick(AltoClef mod) {
        if (_lootTask != null) {
            if (!_lootTask.isFinished(mod)) {
                setDebugState("Looting a desert temple chest");
                return _lootTask;
            }
            _looted++;
        }
        if (mod.getWorld().getBlockState(_temple).getBlock() == Blocks.STONE_PRESSURE_PLATE) {
            setDebugState("Breaking pressure plate");
            return new DestroyBlockTask(_temple);
        }
        if (_looted < 4) {
            setDebugState("Looting a desert temple chest");
            _lootTask = new LootContainerTask(_temple.add(CHEST_POSITIONS_RELATIVE[_looted]), _wanted);
            return _lootTask;
        }
        setDebugState("Why is this still running? Report this");
        return null;
    }

    @Override
    protected void onStop(AltoClef mod, Task task) {
        mod.getClientBaritoneSettings().blocksToAvoid.value.remove(Blocks.STONE_PRESSURE_PLATE);
    }

    @Override
    protected boolean isEqual(Task other) {
        return other instanceof LootDesertTempleTask && ((LootDesertTempleTask) other).getTemplePos() == _temple;
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return _looted == 4;
    }

    @Override
    protected String toDebugString() {
        return "Looting Desert Temple";
    }

    public BlockPos getTemplePos() {
        return _temple;
    }
}