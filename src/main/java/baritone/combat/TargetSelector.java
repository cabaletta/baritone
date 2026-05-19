package baritone.combat;

import baritone.awareness.AwarenessContext;
import baritone.awareness.model.ThreatEntry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Creeper;

import java.util.List;

/**
 * Picks the highest-priority living target from the current threat list,
 * with hard target-locking to prevent mid-fight target switching.
 *
 * Lock rules:
 *   - Stick to the current locked target as long as it is alive and within 24 blocks.
 *   - Only switch when the locked target dies, flees, or de-spawns.
 *   - On first selection (or after lock breaks), pick best by priority:
 *       1. Skeleton (ranged — kill first)
 *       2. Highest-scored alive non-creeper
 *       3. Creeper (solo only)
 */
public final class TargetSelector {

    private static final double LOCK_MAX_DISTANCE = 24.0;

    private Entity lockedTarget = null;

    public ThreatEntry select(AwarenessContext ctx) {
        List<ThreatEntry> threats = ctx.getThreats();
        if (threats.isEmpty()) {
            lockedTarget = null;
            return null;
        }

        // Maintain lock while target is alive and close
        if (lockedTarget != null && lockedTarget.isAlive()) {
            for (ThreatEntry t : threats) {
                if (t.tracked.entity == lockedTarget
                        && t.tracked.distance < LOCK_MAX_DISTANCE) {
                    return t;
                }
            }
        }

        // Lock expired — pick the best new target
        ThreatEntry selected = selectBest(threats);
        lockedTarget = selected != null ? selected.tracked.entity : null;
        return selected;
    }

    private ThreatEntry selectBest(List<ThreatEntry> threats) {
        for (ThreatEntry t : threats) {
            if (t.tracked.entity instanceof AbstractSkeleton && t.tracked.entity.isAlive()) return t;
        }
        for (ThreatEntry t : threats) {
            if (!(t.tracked.entity instanceof Creeper) && t.tracked.entity.isAlive()) return t;
        }
        for (ThreatEntry t : threats) {
            if (t.tracked.entity.isAlive()) return t;
        }
        return null;
    }
}
