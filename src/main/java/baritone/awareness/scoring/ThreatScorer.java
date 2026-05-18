package baritone.awareness.scoring;

import baritone.api.utils.IPlayerContext;
import baritone.awareness.model.EntityCategory;
import baritone.awareness.model.ThreatEntry;
import baritone.awareness.model.TrackedEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts a list of TrackedEntity snapshots into scored ThreatEntry objects.
 * Scoring factors: distance, velocity toward player, line of sight, entity category.
 */
public final class ThreatScorer {

    public List<ThreatEntry> score(List<TrackedEntity> entities, IPlayerContext ctx) {
        List<ThreatEntry> result = new ArrayList<>();
        for (TrackedEntity te : entities) {
            if (te.category == EntityCategory.PASSIVE_MOB
                || te.category == EntityCategory.OTHER
                || te.category == EntityCategory.VEHICLE
                || te.category == EntityCategory.TEAMMATE) continue;
            if (!te.entity.isAlive()) continue;
            float score = computeScore(te, ctx);
            if (score > 0.01f) result.add(new ThreatEntry(te, score));
        }
        result.sort((a, b) -> Float.compare(b.score, a.score));
        return result;
    }

    private float computeScore(TrackedEntity te, IPlayerContext ctx) {
        // 1. Distance factor: 0 at 32 blocks, 1 at 0 blocks
        float distFactor = Math.max(0f, 1f - (float) te.distance / 32f);

        // 2. Velocity-toward-player factor
        float velFactor = 1f;
        if (te.velocity.lengthSqr() > 0.0001) {
            Vec3 toPlayer = ctx.player().position().subtract(te.entity.position()).normalize();
            double approaching = te.velocity.normalize().dot(toPlayer);
            velFactor = 0.7f + 0.3f * (float) Math.max(-1.0, Math.min(1.0, approaching));
        }

        // 3. LOS factor
        float losFactor = te.hasLineOfSight ? 1.4f : 0.6f;

        // 4. Category base
        float base;
        switch (te.category) {
            case ENEMY_PLAYER: base = 0.9f; break;
            case EXPLOSIVE:    base = 0.95f; break;
            case PROJECTILE:   base = 0.85f; break;
            case HOSTILE_MOB:  base = te.state == TrackedEntity.EntityState.CHASING ? 0.65f : 0.35f; break;
            default:           base = 0.1f; break;
        }

        return Math.min(1f, base * distFactor * velFactor * losFactor);
    }
}
