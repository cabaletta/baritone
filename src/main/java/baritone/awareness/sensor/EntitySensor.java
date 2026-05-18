package baritone.awareness.sensor;

import baritone.api.utils.IPlayerContext;
import baritone.awareness.model.EntityCategory;
import baritone.awareness.model.TrackedEntity;
import baritone.awareness.model.TrackedEntity.EntityState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Scans nearby entities every tick, classifies them, and tracks position delta (velocity)
 * and line-of-sight. Instances are pooled by UUID to avoid per-tick allocation.
 */
public final class EntitySensor {

    private static final int SCAN_RADIUS = 32;
    private static final double SCAN_RADIUS_SQ = (double) SCAN_RADIUS * SCAN_RADIUS;
    private static final int FULL_SCAN_INTERVAL = 5;
    /** LOS raycast only performed within this distance squared. */
    private static final double LOS_MAX_DIST_SQ = 400.0;

    private final IPlayerContext ctx;
    private final Map<UUID, TrackedEntity> tracked = new HashMap<>();
    private final Map<UUID, Vec3> prevPositions = new HashMap<>();
    private List<TrackedEntity> snapshot = new ArrayList<>();
    private int tickCount = 0;

    public EntitySensor(IPlayerContext ctx) {
        this.ctx = ctx;
    }

    public void update() {
        tickCount++;

        if (tickCount % FULL_SCAN_INTERVAL == 0) {
            Set<UUID> inRange = new HashSet<>();
            ctx.entitiesStream()
                .filter(e -> !e.equals(ctx.player()))
                .filter(e -> e.distanceToSqr(ctx.player()) <= SCAN_RADIUS_SQ)
                .forEach(e -> inRange.add(e.getUUID()));
            tracked.keySet().retainAll(inRange);
            prevPositions.keySet().retainAll(inRange);
        }

        ctx.entitiesStream()
            .filter(e -> !e.equals(ctx.player()))
            .filter(e -> e.distanceToSqr(ctx.player()) <= SCAN_RADIUS_SQ)
            .forEach(this::updateOrAdd);

        snapshot = new ArrayList<>(tracked.values());
    }

    private void updateOrAdd(Entity entity) {
        UUID id = entity.getUUID();
        TrackedEntity te = tracked.computeIfAbsent(id, k -> new TrackedEntity());
        te.entity = entity;
        te.category = classify(entity);
        te.distance = entity.distanceTo(ctx.player());

        Vec3 prev = prevPositions.getOrDefault(id, entity.position());
        te.velocity = entity.position().subtract(prev);
        prevPositions.put(id, entity.position());

        te.hasLineOfSight = entity.distanceToSqr(ctx.player()) <= LOS_MAX_DIST_SQ
            && checkLos(entity);
        te.state = deriveState(entity);
        te.dangerLevel = rawDanger(te);
    }

    private boolean checkLos(Entity entity) {
        Vec3 from = ctx.player().getEyePosition(1.0f);
        Vec3 to = entity.getEyePosition(1.0f);
        HitResult result = ctx.world().clip(new ClipContext(
            from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, ctx.player()));
        return result.getType() == HitResult.Type.MISS;
    }

    private EntityState deriveState(Entity entity) {
        if (entity instanceof Mob) {
            Mob mob = (Mob) entity;
            if (mob.getTarget() != null && mob.getTarget().equals(ctx.player())) {
                return EntityState.CHASING;
            }
        }
        return EntityState.IDLE;
    }

    private float rawDanger(TrackedEntity te) {
        switch (te.category) {
            case PASSIVE_MOB: case OTHER: case VEHICLE: case TEAMMATE: return 0f;
            default: break;
        }
        float base;
        switch (te.category) {
            case ENEMY_PLAYER: base = 0.8f; break;
            case EXPLOSIVE:    base = 0.9f; break;
            case PROJECTILE:   base = 0.7f; break;
            case HOSTILE_MOB:  base = te.state == EntityState.CHASING ? 0.6f : 0.3f; break;
            default:           base = 0.1f; break;
        }
        return base * Math.max(0f, 1f - (float) te.distance / SCAN_RADIUS);
    }

    private EntityCategory classify(Entity entity) {
        if (entity instanceof Player)          return EntityCategory.ENEMY_PLAYER;
        if (entity instanceof Projectile)      return EntityCategory.PROJECTILE;
        if (entity instanceof PrimedTnt)       return EntityCategory.EXPLOSIVE;
        if (entity.getType() == EntityType.END_CRYSTAL) return EntityCategory.EXPLOSIVE;
        if (entity instanceof Monster)         return EntityCategory.HOSTILE_MOB;
        if (entity instanceof Animal)          return EntityCategory.PASSIVE_MOB;
        if (entity instanceof AbstractMinecart
            || entity instanceof Boat)         return EntityCategory.VEHICLE;
        return EntityCategory.OTHER;
    }

    public List<TrackedEntity> getSnapshot() {
        return snapshot;
    }
}
