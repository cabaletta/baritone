package baritone.awareness.model;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Mutable snapshot of a single nearby entity; instances are pooled by UUID
 * in EntitySensor to avoid per-tick allocation.
 */
public final class TrackedEntity {

    public enum EntityState {
        IDLE, CHASING, ATTACKING, FLEEING
    }

    public Entity entity;
    public EntityCategory category;
    public double distance;
    public Vec3 velocity = Vec3.ZERO;
    public boolean hasLineOfSight;
    public float dangerLevel;
    public EntityState state = EntityState.IDLE;
}
