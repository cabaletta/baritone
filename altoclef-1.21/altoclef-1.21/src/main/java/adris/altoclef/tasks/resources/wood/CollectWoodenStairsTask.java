package adris.altoclef.tasks.resources.wood;

import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.resources.CraftWithMatchingPlanksTask;
import adris.altoclef.util.CraftingRecipe;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.ItemHelper;
import net.minecraft.item.Item;

public class CollectWoodenStairsTask extends CraftWithMatchingPlanksTask {

    public CollectWoodenStairsTask(Item[] targets, ItemTarget planks, int count) {
        super(targets, woodItems -> woodItems.stairs, createRecipe(planks), new boolean[]{true, false, false, true, true, false, true, true, true}, count);
    }

    public CollectWoodenStairsTask(Item target, String plankCatalogueName, int count) {
        this(new Item[]{target}, new ItemTarget(plankCatalogueName, 1), count);
    }

    public CollectWoodenStairsTask(int count) {
        this(ItemHelper.WOOD_STAIRS, TaskCatalogue.getItemTarget("planks", 1), count);
    }

    private static CraftingRecipe createRecipe(ItemTarget planks) {
        ItemTarget p = planks;
        ItemTarget o = null;
        return CraftingRecipe.newShapedRecipe(new ItemTarget[]{p, o, o, p, p, o, p, p, p}, 4);
    }
}
