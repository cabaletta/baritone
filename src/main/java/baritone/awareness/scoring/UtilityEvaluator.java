package baritone.awareness.scoring;

import baritone.awareness.AwarenessContext;
import baritone.awareness.model.ActionIntent;
import baritone.awareness.model.ActionMode;
import baritone.awareness.model.EntityCategory;
import baritone.awareness.model.SelfState;
import baritone.awareness.model.TerrainSnapshot;
import baritone.awareness.model.ThreatEntry;

/**
 * Evaluates a utility score (0-1) for each possible ActionMode and returns the
 * highest-scoring intent. Weights are tunable and additive rather than hard-coded if/else.
 */
public final class UtilityEvaluator {

    public ActionIntent evaluate(AwarenessContext ctx) {
        ActionIntent best = ActionIntent.IDLE;
        ActionIntent[] candidates = {
            evaluateRetreat(ctx),
            evaluateHeal(ctx),
            evaluateShield(ctx),
            evaluateAttack(ctx),
            evaluateReposition(ctx),
            evaluateEscape(ctx)
        };
        for (ActionIntent c : candidates) {
            if (c.utility > best.utility) best = c;
        }
        return best;
    }

    private ActionIntent evaluateRetreat(AwarenessContext ctx) {
        SelfState self = ctx.getSelf();
        float danger = ctx.getOverallDangerLevel();
        float hpFrac = self.health / Math.max(1f, self.maxHealth);
        float utility = Math.min(1f, danger * (1f - hpFrac) * 1.5f);
        if (utility < 0.2f) return new ActionIntent(ActionMode.RETREAT, 0f);
        ThreatEntry primary = ctx.getPrimaryThreat();
        return new ActionIntent(ActionMode.RETREAT, utility,
            primary != null ? primary.tracked.entity : null, null);
    }

    private ActionIntent evaluateHeal(AwarenessContext ctx) {
        SelfState self = ctx.getSelf();
        if (!self.hasGapple && !self.hasPotion) return new ActionIntent(ActionMode.HEAL, 0f);
        float hpFrac = self.health / Math.max(1f, self.maxHealth);
        if (hpFrac > 0.7f) return new ActionIntent(ActionMode.HEAL, 0f);
        float safeMoment = 1f - ctx.getOverallDangerLevel();
        return new ActionIntent(ActionMode.HEAL, Math.min(1f, (1f - hpFrac) * safeMoment * 0.8f));
    }

    private ActionIntent evaluateShield(AwarenessContext ctx) {
        SelfState self = ctx.getSelf();
        if (!self.shieldEquipped) return new ActionIntent(ActionMode.SHIELD, 0f);
        ThreatEntry primary = ctx.getPrimaryThreat();
        if (primary == null) return new ActionIntent(ActionMode.SHIELD, 0f);
        float utility = 0f;
        if (primary.tracked.category == EntityCategory.PROJECTILE && primary.tracked.hasLineOfSight) {
            utility = 0.85f;
        } else if (primary.tracked.distance < 4 && primary.tracked.hasLineOfSight) {
            utility = 0.7f;
        }
        return new ActionIntent(ActionMode.SHIELD, utility);
    }

    private ActionIntent evaluateAttack(AwarenessContext ctx) {
        SelfState self = ctx.getSelf();
        ThreatEntry primary = ctx.getPrimaryThreat();
        if (primary == null) return new ActionIntent(ActionMode.ATTACK, 0f);
        float hpFrac = self.health / Math.max(1f, self.maxHealth);
        float losBonus = primary.tracked.hasLineOfSight ? 0.8f : 0.3f;
        float threatPenalty = Math.min(ctx.getThreats().size(), 5) * 0.15f + 0.7f;
        float utility = Math.min(1f,
            hpFrac * (1f - ctx.getOverallDangerLevel() * 0.3f) * losBonus / threatPenalty);
        return new ActionIntent(ActionMode.ATTACK, utility, primary.tracked.entity, null);
    }

    private ActionIntent evaluateReposition(AwarenessContext ctx) {
        TerrainSnapshot terrain = ctx.getTerrain();
        float utility = 0f;
        if (terrain.fallDanger) utility += 0.4f;
        if (terrain.inBlastZone) utility += 0.5f;
        if (terrain.nearestHazardType != TerrainSnapshot.HazardType.NONE
            && terrain.nearestHazardDistance < 5f) utility += 0.3f;
        return new ActionIntent(ActionMode.REPOSITION, Math.min(1f, utility));
    }

    private ActionIntent evaluateEscape(AwarenessContext ctx) {
        SelfState self = ctx.getSelf();
        if (!self.hasPearl) return new ActionIntent(ActionMode.ESCAPE, 0f);
        TerrainSnapshot terrain = ctx.getTerrain();
        boolean trapped = terrain.escapeDirections.isEmpty();
        if (!trapped || ctx.getOverallDangerLevel() < 0.8f) return new ActionIntent(ActionMode.ESCAPE, 0f);
        return new ActionIntent(ActionMode.ESCAPE, 0.9f);
    }
}
