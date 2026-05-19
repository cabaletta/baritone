package adris.altoclef.tasks.squashed;


import adris.altoclef.AltoClef;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasks.container.UpgradeInSmithingTableTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.Slot;
import adris.altoclef.util.slots.SmithingTableSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.SmithingScreenHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SmithingSquasher extends TypeSquasher<UpgradeInSmithingTableTask> {

    @Override
    protected List<ResourceTask> getSquashed(List<UpgradeInSmithingTableTask> tasks) {
        // Group materials + tools together, then return a list of the same UpgradeInSmithing tasks
        List<ResourceTask> result = new ArrayList<>();
        List<ItemTarget> units = new ArrayList<>();
        for (UpgradeInSmithingTableTask task : tasks) {
            units.add(task.getMaterials());
            units.add(task.getTools());
        }
        result.add(new GetMaterialsTask(units.toArray(ItemTarget[]::new)));
        // Afterwards, perform the smithing.
        result.addAll(tasks);
        return result;
    }

    private static class GetMaterialsTask extends ResourceTask {

        public GetMaterialsTask(ItemTarget[] targets) {
            super(targets);
        }

        @Override
        protected boolean shouldAvoidPickingUp(AltoClef mod) {
            return false;
        }

        @Override
        protected void onResourceStart(AltoClef mod) {

        }

        private int getItemsInSlot(AltoClef mod, Slot slot, ItemTarget match) {
            ItemStack stack = StorageHelper.getItemStackInSlot(slot);
            if (!stack.isEmpty() && match.matches(stack.getItem())) {
                return stack.getCount();
            }
            return 0;
        }

        @Override
        protected Task onResourceTick(AltoClef mod) {
            List<ItemTarget> resultingTargets = Arrays.asList(_itemTargets);

            // Subtract required counts if we're in a smithing table, so putting items in the table doesn't remove them.
            boolean inSmithingTable = (mod.getPlayer().currentScreenHandler instanceof SmithingScreenHandler);
            if (inSmithingTable) {
                for (int i = 0; i < resultingTargets.size(); ++i) {
                    ItemTarget target = resultingTargets.get(i);
                    int smithingTableCount = getItemsInSlot(mod, SmithingTableSlot.INPUT_SLOT_MATERIALS, target)
                            + getItemsInSlot(mod, SmithingTableSlot.INPUT_SLOT_TOOL, target)
                            + getItemsInSlot(mod, SmithingTableSlot.OUTPUT_SLOT, target);
                    resultingTargets.set(i, new ItemTarget(target, target.getTargetCount() - smithingTableCount));
                }
            }
            return new CataloguedResourceTask(resultingTargets.toArray(ItemTarget[]::new));
        }

        @Override
        protected void onResourceStop(AltoClef mod, Task interruptTask) {

        }

        @Override
        protected boolean isEqualResource(ResourceTask other) {
            return true; // item targets are the only difference
        }

        @Override
        protected String toDebugStringName() {
            return "Collecting Smithing Materials";
        }
    }
}
