package baritone.awareness.behavior;

import baritone.Baritone;
import baritone.api.event.events.TickEvent;
import baritone.awareness.AwarenessContext;
import baritone.awareness.decision.DecisionEngine;
import baritone.awareness.decision.ReactionSystem;
import baritone.awareness.model.ActionIntent;
import baritone.awareness.model.EntityCategory;
import baritone.awareness.model.ThreatEntry;
import baritone.awareness.model.TrackedEntity;
import baritone.awareness.scoring.ThreatScorer;
import baritone.awareness.sensor.CombatStatSensor;
import baritone.awareness.sensor.EntitySensor;
import baritone.awareness.sensor.SelfSensor;
import baritone.awareness.sensor.TerrainSensor;
import baritone.behavior.Behavior;

import java.util.List;

/**
 * Registered as the first Behavior in Baritone so the awareness pipeline runs before
 * any process sees the tick. Orchestrates: sensors -> threat scoring -> utility
 * decision -> reaction system, then commits the result to AwarenessContext.
 */
public final class AwarenessBehavior extends Behavior {

    private final EntitySensor entitySensor;
    private final TerrainSensor terrainSensor;
    private final SelfSensor selfSensor;
    private final CombatStatSensor combatStatSensor;
    private final ThreatScorer threatScorer;
    private final DecisionEngine decisionEngine;
    private final ReactionSystem reactionSystem;
    private final AwarenessContext awarenessContext;

    public AwarenessBehavior(Baritone baritone) {
        super(baritone);
        this.awarenessContext = baritone.getAwarenessContext();
        this.entitySensor = new EntitySensor(baritone.getPlayerContext());
        this.terrainSensor = new TerrainSensor(baritone.getPlayerContext());
        this.selfSensor = new SelfSensor(baritone.getPlayerContext());
        this.combatStatSensor = new CombatStatSensor(baritone.getPlayerContext());
        this.threatScorer = new ThreatScorer();
        this.decisionEngine = new DecisionEngine();
        // Pass baritone reference so ReactionSystem resolves InputOverrideHandler lazily
        // (it is registered after AwarenessBehavior, so getInputOverrideHandler() is null here)
        this.reactionSystem = new ReactionSystem(baritone.getPlayerContext(), baritone);
    }

    @Override
    public void onTick(TickEvent event) {
        if (event.getType() != TickEvent.Type.IN) return;
        if (ctx.player() == null || ctx.world() == null) return;

        // 1. Sensors
        entitySensor.update();
        terrainSensor.update();
        selfSensor.update();
        combatStatSensor.update(entitySensor.getSnapshot());

        // 2. Threat scoring
        List<TrackedEntity> entities = entitySensor.getSnapshot();
        List<ThreatEntry> threats = threatScorer.score(entities, ctx);

        // 3. Overall danger (sum of scores, capped at 1.0)
        float danger = 0f;
        for (ThreatEntry t : threats) {
            danger = Math.min(1f, danger + t.score * 0.4f);
        }

        // 4. Pre-decision context publish so DecisionEngine can read threat/self/terrain data
        awarenessContext.update(entities, threats, terrainSensor.getSnapshot(),
            selfSensor.getSnapshot(), ActionIntent.IDLE, danger);

        // 5. Mark blast zone from entity scan results
        boolean inBlast = false;
        for (ThreatEntry t : threats) {
            if (t.tracked.category == EntityCategory.EXPLOSIVE && t.tracked.distance < 8) {
                inBlast = true;
                break;
            }
        }
        terrainSensor.getSnapshot().inBlastZone = inBlast;

        // 6. Decision engine (hysteresis-guarded utility selection)
        ActionIntent intent = decisionEngine.decide(awarenessContext);

        // 7. Final context commit including chosen intent
        awarenessContext.update(entities, threats, terrainSensor.getSnapshot(),
            selfSensor.getSnapshot(), intent, danger);

        // 8. Reflex reactions (immediate input overrides, e.g. shield raise)
        reactionSystem.evaluate(awarenessContext);
    }
}
