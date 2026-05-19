package baritone.awareness.process;

import baritone.Baritone;
import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;
import baritone.awareness.AwarenessContext;
import baritone.awareness.model.ThreatEntry;
import baritone.combat.CombatEngine;
import baritone.utils.BaritoneProcessHelper;

import java.util.List;

/**
 * Activates when overall danger exceeds the AwarenessContext threshold (0.3).
 * All combat logic is delegated to CombatEngine.
 */
public final class CombatProcess extends BaritoneProcessHelper {

    private final AwarenessContext awarenessCtx;
    private final CombatEngine     combatEngine;

    public CombatProcess(Baritone baritone) {
        super(baritone);
        this.awarenessCtx = baritone.getAwarenessContext();
        this.combatEngine = new CombatEngine(baritone, awarenessCtx);
    }

    @Override
    public boolean isActive() {
        // Stay active during a post-explosion creeper escape even if no threats remain
        if (combatEngine.hasPendingEscape()) return true;
        List<ThreatEntry> threats = awarenessCtx.getThreats();
        if (threats.isEmpty()) return false;
        ThreatEntry primary = threats.get(0);
        return primary.tracked.distance < 20 || awarenessCtx.getOverallDangerLevel() > 0.15f;
    }

    @Override
    public PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel) {
        return combatEngine.tick();
    }

    @Override
    public void onLostControl() {
        // Clear all forced inputs so no movement key stays held after combat ends
        baritone.getInputOverrideHandler().clearAllKeys();
    }

    @Override
    public double priority() {
        return 10.0;
    }

    @Override
    public boolean isTemporary() {
        return false;
    }

    @Override
    public String displayName0() {
        return "Combat [" + awarenessCtx.getIntent().mode.name() + "] danger="
            + String.format("%.2f", awarenessCtx.getOverallDangerLevel())
            + " threats=" + awarenessCtx.getThreats().size();
    }
}
