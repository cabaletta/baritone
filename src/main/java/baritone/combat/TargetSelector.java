package baritone.combat;

import baritone.awareness.AwarenessContext;
import baritone.awareness.model.ThreatEntry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Creeper;

import java.util.List;

public final class TargetSelector {

    private static final double LOCK_MAX_DISTANCE = 24.0;

    private Entity lockedTarget = null;

    public ThreatEntry select(AwarenessContext ctx) {
        List<ThreatEntry> threats = ctx.getThreats();
        if (threats.isEmpty()) {
            lockedTarget = null;
            return null;
        }

        if (lockedTarget != null && lockedTarget.isAlive()) {
            for (ThreatEntry t : threats) {
                if (t.tracked.entity == lockedTarget
                        && t.tracked.distance < LOCK_MAX_DISTANCE) {
                    return t;
                }
            }
        }

        ThreatEntry selected = selectBest(threats);
        lockedTarget = selected != null ? selected.tracked.entity : null;
        return selected;
    }

    private ThreatEntry selectBest(List<ThreatEntry> threats) {
        for (ThreatEntry t : threats) {
            EntityType<?> et = t.tracked.entity.getType();
            if ((et == EntityType.SKELETON || et == EntityType.STRAY || et == EntityType.WITHER_SKELETON)
                    && t.tracked.entity.isAlive()) return t;
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
