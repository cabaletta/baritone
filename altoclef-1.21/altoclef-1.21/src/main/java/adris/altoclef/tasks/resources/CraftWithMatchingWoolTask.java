package adris.altoclef.tasks.resources;

import adris.altoclef.util.CraftingRecipe;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.ItemHelper;
import net.minecraft.item.Item;

import java.util.function.Function;

// This is literally identical to its parent, but we give it a name for psychological reasons
public abstract class CraftWithMatchingWoolTask extends CraftWithMatchingMaterialsTask {

    private final Function<ItemHelper.ColorfulItems, Item> _getMajorityMaterial;
    private final Function<ItemHelper.ColorfulItems, Item> _getTargetItem;

    public CraftWithMatchingWoolTask(ItemTarget target, Function<ItemHelper.ColorfulItems, Item> getMajorityMaterial, Function<ItemHelper.ColorfulItems, Item> getTargetItem, CraftingRecipe recipe, boolean[] sameMask) {
        super(target, recipe, sameMask);
        _getMajorityMaterial = getMajorityMaterial;
        _getTargetItem = getTargetItem;
    }


    @Override
    protected Item getSpecificItemCorrespondingToMajorityResource(Item majority) {
        for (ItemHelper.ColorfulItems colorfulItem : ItemHelper.getColorfulItems()) {
            if (_getMajorityMaterial.apply(colorfulItem) == majority) {
                return _getTargetItem.apply(colorfulItem);
            }
        }
        return null;
    }
}
