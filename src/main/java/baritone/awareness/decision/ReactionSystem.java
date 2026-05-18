package baritone.awareness.decision;

import baritone.Baritone;
import baritone.api.utils.IPlayerContext;
import baritone.api.utils.input.Input;
import baritone.awareness.AwarenessContext;
import baritone.awareness.model.EntityCategory;
import baritone.awareness.model.SelfState;
import baritone.awareness.model.ThreatEntry;
import baritone.utils.InputOverrideHandler;

import java.util.List;

/**
 * Fires immediate input overrides (sub-100ms reflexes) in response to imminent threats.
 * Scans ALL tracked threats, not just the primary, so multiple simultaneous dangers
 * (e.g. a projectile + a melee attacker) are both considered each tick.
 */
public final class ReactionSystem {

    private final IPlayerContext ctx;
    private final Baritone baritone;

    public ReactionSystem(IPlayerContext ctx, Baritone baritone) {
        this.ctx = ctx;
        this.baritone = baritone;
    }

    public void evaluate(AwarenessContext awarenessCtx) {
        InputOverrideHandler input = baritone.getInputOverrideHandler();
        if (input == null) return;

        // Reset any shield raise from the previous tick.
        input.setInputForceState(Input.CLICK_RIGHT, false);

        List<ThreatEntry> threats = awarenessCtx.getThreats();
        if (threats.isEmpty()) return;

        SelfState self = awarenessCtx.getSelf();
        if (!self.shieldEquipped) return;

        // Reflex: block any incoming projectile with LOS within 12 blocks.
        for (ThreatEntry t : threats) {
            if (t.tracked.category == EntityCategory.PROJECTILE
                    && t.tracked.hasLineOfSight
                    && t.tracked.distance < 12) {
                input.setInputForceState(Input.CLICK_RIGHT, true);
                return;
            }
        }

        // Reflex: raise shield against any close melee attacker with low attack cooldown.
        for (ThreatEntry t : threats) {
            if (t.tracked.distance < 3
                    && t.tracked.hasLineOfSight
                    && self.attackCooldown < 0.7f) {
                input.setInputForceState(Input.CLICK_RIGHT, true);
                return;
            }
        }
    }
}
