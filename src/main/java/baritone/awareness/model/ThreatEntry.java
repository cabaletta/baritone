package baritone.awareness.model;

/** Associates a tracked entity with its computed danger score (0-1). */
public final class ThreatEntry {

    public final TrackedEntity tracked;
    public final float score;

    public ThreatEntry(TrackedEntity tracked, float score) {
        this.tracked = tracked;
        this.score = score;
    }
}
