package baritone.awareness;

import baritone.awareness.model.ActionIntent;
import baritone.awareness.model.SelfState;
import baritone.awareness.model.TerrainSnapshot;
import baritone.awareness.model.ThreatEntry;
import baritone.awareness.model.TrackedEntity;

import java.util.Collections;
import java.util.List;

/**
 * Shared read-only hub holding the latest tick's awareness snapshot.
 * Written exclusively by AwarenessBehavior; read by CombatProcess, HUDBehavior, etc.
 * All access happens on the MC main thread, so no synchronization is needed.
 */
public final class AwarenessContext {

    private List<TrackedEntity> trackedEntities = Collections.emptyList();
    private List<ThreatEntry> threats = Collections.emptyList();
    private TerrainSnapshot terrain = new TerrainSnapshot();
    private SelfState self = new SelfState();
    private ActionIntent intent = ActionIntent.IDLE;
    private float overallDangerLevel = 0f;

    public void update(List<TrackedEntity> entities, List<ThreatEntry> threats,
                       TerrainSnapshot terrain, SelfState self,
                       ActionIntent intent, float dangerLevel) {
        this.trackedEntities = entities;
        this.threats = threats;
        this.terrain = terrain;
        this.self = self;
        this.intent = intent;
        this.overallDangerLevel = dangerLevel;
    }

    public List<TrackedEntity> getTrackedEntities() { return trackedEntities; }
    public List<ThreatEntry> getThreats() { return threats; }
    public TerrainSnapshot getTerrain() { return terrain; }
    public SelfState getSelf() { return self; }
    public ActionIntent getIntent() { return intent; }
    public float getOverallDangerLevel() { return overallDangerLevel; }

    public ThreatEntry getPrimaryThreat() {
        return threats.isEmpty() ? null : threats.get(0);
    }

    /** True when the overall danger level exceeds the CombatProcess activation threshold. */
    public boolean isUnderThreat() {
        return overallDangerLevel > 0.3f;
    }
}
