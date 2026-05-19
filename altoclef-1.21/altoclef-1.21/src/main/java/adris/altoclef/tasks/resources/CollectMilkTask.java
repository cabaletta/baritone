package adris.altoclef.tasks.resources;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasks.entity.AbstractDoToEntityTask;
import adris.altoclef.tasksystem.Task;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

import java.util.Optional;

public class CollectMilkTask extends ResourceTask {

    private final int _count;

    public CollectMilkTask(int targetCount) {
        super(Items.MILK_BUCKET, targetCount);
        _count = targetCount;
    }

    @Override
    protected boolean shouldAvoidPickingUp(AltoClef mod) {
        return false;
    }

    @Override
    protected void onResourceStart(AltoClef mod) {
    }

    @Override
    protected Task onResourceTick(AltoClef mod) {
        // Make sure we have a bucket.
        if (!mod.getItemStorage().hasItem(Items.BUCKET)) {
            return TaskCatalogue.getItemTask(Items.BUCKET, 1);
        }
        // Dimension
        if (!mod.getEntityTracker().entityFound(CowEntity.class) && isInWrongDimension(mod)) {
            return getToCorrectDimensionTask(mod);
        }
        return new MilkCowTask();
    }

    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {

    }

    @Override
    protected boolean isEqualResource(ResourceTask other) {
        return other instanceof CollectMilkTask;
    }

    @Override
    protected String toDebugStringName() {
        return "Collecting " + _count + " milk buckets.";
    }

    static class MilkCowTask extends AbstractDoToEntityTask {

        public MilkCowTask() {
            super(0, -1, -1);
        }

        @Override
        protected boolean isSubEqual(AbstractDoToEntityTask other) {
            return other instanceof MilkCowTask;
        }

        @Override
        protected Task onEntityInteract(AltoClef mod, Entity entity) {
            if (!mod.getItemStorage().hasItem(Items.BUCKET)) {
                Debug.logWarning("Failed to milk cow because you have no bucket.");
                return null;
            }
            if (mod.getSlotHandler().forceEquipItem(Items.BUCKET)) {
                mod.getController().interactEntity(mod.getPlayer(), entity, Hand.MAIN_HAND);
            }


            return null;
        }

        @Override
        protected Optional<Entity> getEntityTarget(AltoClef mod) {
            return mod.getEntityTracker().getClosestEntity(mod.getPlayer().getPos(), CowEntity.class);
        }

        @Override
        protected String toDebugString() {
            return "Milking Cow";
        }
    }
}
