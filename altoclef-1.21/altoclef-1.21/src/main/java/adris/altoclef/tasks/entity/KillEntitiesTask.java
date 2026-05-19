package adris.altoclef.tasks.entity;

import net.minecraft.entity.Entity;

import java.util.function.Predicate;

/**
 * Kill all entities of a type
 */
public class KillEntitiesTask extends DoToClosestEntityTask {

    public KillEntitiesTask(Predicate<Entity> shouldKill, Class<?>... entities) {
        super(KillEntityTask::new, shouldKill, entities);
    }

    public KillEntitiesTask(Class<?>... entities) {
        super(KillEntityTask::new, entities);
    }
}
