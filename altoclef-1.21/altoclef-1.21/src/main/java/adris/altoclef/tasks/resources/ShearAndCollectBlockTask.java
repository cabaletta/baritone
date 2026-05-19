package adris.altoclef.tasks.resources;

import adris.altoclef.AltoClef;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.MiningRequirement;
import adris.altoclef.util.helpers.ItemHelper;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

public class ShearAndCollectBlockTask extends MineAndCollectTask {

    public ShearAndCollectBlockTask(ItemTarget[] itemTargets, Block... blocksToMine) {
        super(itemTargets, blocksToMine, MiningRequirement.HAND);
    }

    public ShearAndCollectBlockTask(Item[] items, int count, Block... blocksToMine) {
        this(new ItemTarget[]{new ItemTarget(items, count)}, blocksToMine);
    }

    public ShearAndCollectBlockTask(Item item, int count, Block... blocksToMine) {
        this(new Item[]{item}, count, blocksToMine);
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.getBehaviour().push();
        mod.getBehaviour().forceUseTool((blockState, itemStack) ->
                itemStack.getItem() == Items.SHEARS && ItemHelper.areShearsEffective(blockState.getBlock())
        );
        super.onStart(mod);
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getBehaviour().pop();
        super.onStop(mod, interruptTask);
    }

    @Override
    protected Task onResourceTick(AltoClef mod) {
        if (!mod.getItemStorage().hasItem(Items.SHEARS)) {
            return TaskCatalogue.getItemTask(Items.SHEARS, 1);
        }
        return super.onResourceTick(mod);
    }
}
