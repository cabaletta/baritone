package adris.altoclef.tasks.resources;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.CraftingRecipe;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.RecipeTarget;
import adris.altoclef.util.helpers.StorageHelper;
import net.minecraft.item.Item;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.HashMap;

// Collects everything that's catalogued for a recipe.
public class CollectRecipeCataloguedResourcesTask extends Task {

    private final RecipeTarget[] _targets;
    private final boolean _ignoreUncataloguedSlots;
    private boolean _finished = false;

    public CollectRecipeCataloguedResourcesTask(boolean ignoreUncataloguedSlots, RecipeTarget... targets) {
        _targets = targets;
        _ignoreUncataloguedSlots = ignoreUncataloguedSlots;
    }

    @Override
    protected void onStart(AltoClef mod) {
        _finished = false;
    }

    @Override
    protected Task onTick(AltoClef mod) {
        // TODO: Cache this once instead of doing it every frame.

        // Stuff to get, both catalogued + individual items.
        HashMap<String, Integer> catalogueCount = new HashMap<>();
        HashMap<Item, Integer> itemCount = new HashMap<>();

        for (RecipeTarget target : _targets) {
            // Ignore this recipe if we have its item.
            //if (mod.getItemStorage().targetMet(target.getItem())) continue;

            // null = empty which is always met.
            if (target == null) continue;

            int weNeed = target.getTargetCount() - mod.getItemStorage().getItemCount(target.getOutputItem());

            if (weNeed > 0) {
                CraftingRecipe recipe = target.getRecipe();
                // Default, just go through the recipe slots and collect the first one.
                for (int i = 0; i < recipe.getSlotCount(); ++i) {
                    ItemTarget slot = recipe.getSlot(i);
                    if (slot == null || slot.isEmpty()) continue;
                    int numberOfRepeats = (int) Math.floor(-0.1 + (double) weNeed / target.getRecipe().outputCount()) + 1;
                    if (!slot.isCatalogueItem()) {
                        if (slot.getMatches().length != 1) {
                            if (!_ignoreUncataloguedSlots) {
                                Debug.logWarning("Recipe collection for recipe " + recipe + " slot " + i
                                        + " is not catalogued. Please define an explicit"
                                        + " collectRecipeSubTask() function for this item target:" + slot
                                );
                            }
                        } else {
                            Item item = slot.getMatches()[0];
                            itemCount.put(item, itemCount.getOrDefault(item, 0) + numberOfRepeats);
                        }
                    } else {
                        String targetName = slot.getCatalogueName();
                        // How many "repeats" of a recipe we will need.
                        catalogueCount.put(targetName, catalogueCount.getOrDefault(targetName, 0) + numberOfRepeats);
                    }
                }
            }
        }


        // (Cache this with the above stuff!!)
        // Grab materials
        for (String catalogueMaterialName : catalogueCount.keySet()) {
            int count = catalogueCount.get(catalogueMaterialName);
            if (count > 0) {
                ItemTarget itemTarget = new ItemTarget(catalogueMaterialName, count);
                if (!StorageHelper.itemTargetsMet(mod, itemTarget)) {
                    setDebugState("Getting " + itemTarget);
                    return TaskCatalogue.getItemTask(catalogueMaterialName, count);
                }
            }
        }
        for (Item item : itemCount.keySet()) {
            int count = itemCount.get(item);
            if (count > 0) {
                if (mod.getItemStorage().getItemCount(item) < count) {
                    setDebugState("Getting " + item.getTranslationKey());
                    return TaskCatalogue.getItemTask(item, count);
                }
            }
        }
        _finished = true;

        return null;
    }


    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {

    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof CollectRecipeCataloguedResourcesTask task) {
            return Arrays.equals(task._targets, _targets);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Collect Recipe Resources: " + ArrayUtils.toString(_targets);
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        if (_finished) {
            if (!StorageHelper.hasRecipeMaterialsOrTarget(mod, this._targets)) {
                _finished = false;
                Debug.logMessage("Invalid collect recipe \"finished\" state, resetting.");
            }
        }
        return _finished;
    }
}
