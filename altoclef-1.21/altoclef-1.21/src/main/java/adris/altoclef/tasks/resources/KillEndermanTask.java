package adris.altoclef.tasks.resources;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasks.entity.KillEntitiesTask;
import adris.altoclef.tasks.entity.KillEntityTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.time.TimerGame;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.item.Items;

import java.util.Optional;

public class KillEndermanTask extends ResourceTask {

    private final int _count;

    private final TimerGame _lookDelay = new TimerGame(0.2);

    public KillEndermanTask(int count) {
        super(new ItemTarget(Items.ENDER_PEARL, count));
        _count = count;
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
        // Dimension
        if (!mod.getEntityTracker().entityFound(EndermanEntity.class)) {
            return getToCorrectDimensionTask(mod);
        }

        // Kill the angry one
        Optional<Entity> enderman = mod.getEntityTracker().getClosestEntity(EndermanEntity.class);
        if (enderman.isPresent()) {
            EndermanEntity endermanEntity = (EndermanEntity) enderman.get();
            final int TOO_FAR_AWAY = 256;
            if (endermanEntity.isAngry() && endermanEntity.getPos().isInRange(mod.getPlayer().getPos(), TOO_FAR_AWAY)) {
                return new KillEntityTask(endermanEntity);
            }
        }
        // Attack the closest one
        return new KillEntitiesTask(EndermanEntity.class);
    }

    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {

    }

    @Override
    protected boolean isEqualResource(ResourceTask other) {
        if (other instanceof KillEndermanTask task) {
            return task._count == _count;
        }
        return false;
    }

    @Override
    protected String toDebugStringName() {
        return "Hunting enderman for " + _count + " pearls.";
    }
}