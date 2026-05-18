package baritone.awareness.decision;

import baritone.Baritone;
import baritone.api.utils.IPlayerContext;
import baritone.api.utils.input.Input;
import baritone.awareness.AwarenessContext;
import baritone.awareness.model.EntityCategory;
import baritone.awareness.model.SelfState;
import baritone.awareness.model.ThreatEntry;
import baritone.utils.InputOverrideHandler;

/**
 * Fires immediate input overrides (sub-100ms reflexes) in response to imminent threats.
 * Operates independently of the decision engine — reflexes bypass deliberation.
 * Only CLICK_RIGHT (shield raise) is managed here; movement stays in CombatProcess.
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

        // Reset any shield raise from the previous tick
        input.setInputForceState(Input.CLICK_RIGHT, false);

        ThreatEntry primary = awarenessCtx.getPrimaryThreat();
        if (primary == null) return;

        SelfState self = awarenessCtx.getSelf();

        // Reflex: block incoming projectile with LOS within 12 blocks
        if (primary.tracked.category == EntityCategory.PROJECTILE
            && primary.tracked.hasLineOfSight
            && primary.tracked.distance < 12
            && self.shieldEquipped) {
            input.setInputForceState(Input.CLICK_RIGHT, true);
            return;
        }

        // Reflex: raise shield against close-range attacker when attack cooldown is low
        // (don't shield when we're fully charged and about to swing ourselves)
        if (primary.tracked.distance < 3
            && primary.tracked.hasLineOfSight
            && self.shieldEquipped
            && self.attackCooldown < 0.7f) {
            input.setInputForceState(Input.CLICK_RIGHT, true);
        }
    }
}
