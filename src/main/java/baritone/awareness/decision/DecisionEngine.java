package baritone.awareness.decision;

import baritone.awareness.AwarenessContext;
import baritone.awareness.model.ActionIntent;
import baritone.awareness.scoring.UtilityEvaluator;

/**
 * Wraps UtilityEvaluator with a hysteresis threshold to prevent rapid intent switching
 * when two actions have nearly equal utility scores.
 */
public final class DecisionEngine {

    /** Challenger must beat current intent by this margin to cause a switch. */
    private static final float HYSTERESIS = 0.1f;

    private final UtilityEvaluator evaluator = new UtilityEvaluator();
    private ActionIntent currentIntent = ActionIntent.IDLE;

    public ActionIntent decide(AwarenessContext ctx) {
        ActionIntent candidate = evaluator.evaluate(ctx);

        if (candidate.utility > currentIntent.utility + HYSTERESIS) {
            currentIntent = candidate;
        }
        if (currentIntent.utility < 0.05f) {
            currentIntent = ActionIntent.IDLE;
        }
        return currentIntent;
    }
}
