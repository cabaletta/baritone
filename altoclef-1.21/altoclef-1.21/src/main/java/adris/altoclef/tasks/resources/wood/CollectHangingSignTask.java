package adris.altoclef.tasks.resources.wood;

import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.resources.CraftWithMatchingStrippedLogsTask;
import adris.altoclef.util.CraftingRecipe;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.ItemHelper;
import net.minecraft.item.Item;

public class CollectHangingSignTask extends CraftWithMatchingStrippedLogsTask {

    public CollectHangingSignTask(Item[] targets, ItemTarget strippedLogs, int count) {
        // Bottom 6 are stripped logs, must be the same.
        super(targets, woodItems -> woodItems.hangingSign, createRecipe(strippedLogs), new boolean[]{false, false, false, true, true, true, true, true, true}, count);
    }

    public CollectHangingSignTask(Item target, String strippedLogCatalogueName, int count) {
        this(new Item[]{target}, new ItemTarget(strippedLogCatalogueName, 1), count);
    }

    public CollectHangingSignTask(int count) {
        this(ItemHelper.WOOD_HANGING_SIGN, TaskCatalogue.getItemTarget("stripped_logs", 1), count);
    }


    private static CraftingRecipe createRecipe(ItemTarget strippedLogs) {
        ItemTarget s = strippedLogs;
        ItemTarget chain = TaskCatalogue.getItemTarget("chain", 1);
        return CraftingRecipe.newShapedRecipe(new ItemTarget[]{chain, null, chain, s, s, s, s, s, s}, 6);
    }
}
