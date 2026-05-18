package baritone.combat;

import baritone.awareness.AwarenessContext;
import baritone.awareness.model.ThreatEntry;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Creeper;

import java.util.List;

/**
 * Picks the highest-priority living target from the current threat list.
 *
 * Priority order:
 *   1. Any skeleton (ranged damage disrupts all other combat)
 *   2. Highest-scored alive non-creeper (creepers handled by CreepeTactics)
 *   3. Creeper if nothing else is alive
 */
public final class TargetSelector {

    public ThreatEntry select(AwarenessContext ctx) {
        List<ThreatEntry> threats = ctx.getThreats();
        if (threats.isEmpty()) return null;

        // 1. Skeleton first
        for (ThreatEntry t : threats) {
            if (t.tracked.entity instanceof AbstractSkeleton && t.tracked.entity.isAlive()) {
                return t;
            }
        }

        // 2. Best non-creeper (list is already sorted by threat score)
        for (ThreatEntry t : threats) {
            if (!(t.tracked.entity instanceof Creeper) && t.tracked.entity.isAlive()) {
                return t;
            }
        }

        // 3. Creeper only if it is the sole remaining threat
        for (ThreatEntry t : threats) {
            if (t.tracked.entity.isAlive()) return t;
        }

        return null;
    }
}
