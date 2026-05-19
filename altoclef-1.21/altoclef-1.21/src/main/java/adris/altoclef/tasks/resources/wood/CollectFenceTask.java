package adris.altoclef.tasks.resources.wood;

import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.resources.CraftWithMatchingPlanksTask;
import adris.altoclef.util.CraftingRecipe;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.ItemHelper;
import net.minecraft.item.Item;

public class CollectFenceTask extends CraftWithMatchingPlanksTask {

    public CollectFenceTask(Item[] targets, ItemTarget planks, int count) {
        super(targets, woodItems -> woodItems.fence, createRecipe(planks), new boolean[]{true, false, true, true, false, true, false, false, false}, count);
    }

    public CollectFenceTask(Item target, String plankCatalogueName, int count) {
        this(new Item[]{target}, new ItemTarget(plankCatalogueName, 1), count);
    }

    public CollectFenceTask(int count) {
        this(ItemHelper.WOOD_FENCE, TaskCatalogue.getItemTarget("planks", 1), count);
    }

    private static CraftingRecipe createRecipe(ItemTarget planks) {
        ItemTarget p = planks;
        ItemTarget s = TaskCatalogue.getItemTarget("stick", 1);
        return CraftingRecipe.newShapedRecipe(new ItemTarget[]{p, s, p, p, s, p, null, null, null}, 3);
    }
}
