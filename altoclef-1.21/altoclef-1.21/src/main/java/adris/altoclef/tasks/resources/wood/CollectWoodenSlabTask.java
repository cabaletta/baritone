package adris.altoclef.tasks.resources.wood;

import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.resources.CraftWithMatchingPlanksTask;
import adris.altoclef.util.CraftingRecipe;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.ItemHelper;
import net.minecraft.item.Item;

public class CollectWoodenSlabTask extends CraftWithMatchingPlanksTask {

    public CollectWoodenSlabTask(Item[] targets, ItemTarget planks, int count) {
        super(targets, woodItems -> woodItems.slab, createRecipe(planks), new boolean[]{true, true, true, false, false, false, false, false, false}, count);
    }

    public CollectWoodenSlabTask(Item target, String plankCatalogueName, int count) {
        this(new Item[]{target}, new ItemTarget(plankCatalogueName, 1), count);
    }

    public CollectWoodenSlabTask(int count) {
        this(ItemHelper.WOOD_SLAB, TaskCatalogue.getItemTarget("planks", 1), count);
    }

    private static CraftingRecipe createRecipe(ItemTarget planks) {
        ItemTarget p = planks;
        ItemTarget o = null;
        return CraftingRecipe.newShapedRecipe(new ItemTarget[]{p, p, p, o, o, o, o, o, o}, 6);
    }
}
