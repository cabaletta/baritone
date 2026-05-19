package adris.altoclef.tasks.container;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import org.apache.commons.lang3.NotImplementedException;

public class CraftInAnvilTask extends DoStuffInContainerTask {
    public CraftInAnvilTask() {
        super(new Block[]{Blocks.ANVIL, Blocks.CHIPPED_ANVIL, Blocks.DAMAGED_ANVIL}, new ItemTarget("anvil"));
    }

    @Override
    protected boolean isSubTaskEqual(DoStuffInContainerTask other) {
        throw new NotImplementedException("Anvil Not Implemented, whoops");
    }

    @Override
    protected boolean isContainerOpen(AltoClef mod) {
        throw new NotImplementedException("Anvil Not Implemented, whoops");
    }

    @Override
    protected Task containerSubTask(AltoClef mod) {
        throw new NotImplementedException("Anvil Not Implemented, whoops");
    }

    @Override
    protected double getCostToMakeNew(AltoClef mod) {
        throw new NotImplementedException("Anvil Not Implemented, whoops");
    }
}
