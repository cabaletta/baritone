package baritone.awareness.process;

import baritone.Baritone;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.pathing.goals.GoalNear;
import baritone.api.pathing.goals.GoalRunAway;
import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;
import baritone.api.utils.BetterBlockPos;
import baritone.awareness.AwarenessContext;
import baritone.awareness.model.ActionIntent;
import baritone.awareness.model.TerrainSnapshot;
import baritone.awareness.model.ThreatEntry;
import baritone.utils.BaritoneProcessHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Translates the current ActionIntent from AwarenessContext into a Baritone Goal.
 * Runs at priority 10.0, above all built-in processes, but only activates when
 * the overall danger level exceeds the AwarenessContext threshold.
 */
public final class CombatProcess extends BaritoneProcessHelper {

    private static final double RETREAT_DISTANCE = 20.0;
    private static final float  MIN_RETREAT_SCORE = 0.15f;

    private final AwarenessContext awarenessCtx;

    public CombatProcess(Baritone baritone) {
        super(baritone);
        this.awarenessCtx = baritone.getAwarenessContext();
    }

    @Override
    public boolean isActive() {
        return awarenessCtx.isUnderThreat();
    }

    @Override
    public PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel) {
        ActionIntent intent = awarenessCtx.getIntent();
        List<ThreatEntry> threats = awarenessCtx.getThreats();

        switch (intent.mode) {
            case RETREAT: {
                // Collect positions of every threat above the minimum score threshold.
                List<BlockPos> fromPositions = new ArrayList<>();
                for (ThreatEntry t : threats) {
                    if (t.score >= MIN_RETREAT_SCORE && t.tracked.entity.isAlive()) {
                        fromPositions.add(t.tracked.entity.blockPosition());
                    }
                }
                if (fromPositions.isEmpty()) {
                    return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
                }
                Goal goal = new GoalRunAway(RETREAT_DISTANCE,
                    fromPositions.toArray(new BlockPos[0]));
                return new PathingCommand(goal, PathingCommandType.REVALIDATE_GOAL_AND_PATH);
            }
            case REPOSITION: {
                TerrainSnapshot terrain = awarenessCtx.getTerrain();
                if (!terrain.escapeDirections.isEmpty()) {
                    Vec3 dir = terrain.escapeDirections.get(0);
                    BetterBlockPos feet = ctx.playerFeet();
                    BlockPos target = new BlockPos(
                        feet.x + (int) (dir.x * 8),
                        feet.y,
                        feet.z + (int) (dir.z * 8)
                    );
                    return new PathingCommand(new GoalBlock(target),
                        PathingCommandType.REVALIDATE_GOAL_AND_PATH);
                }
                return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
            }
            case ATTACK: {
                // Prefer a target that is alive and has LOS; fall back to highest-scored alive target.
                ThreatEntry best = null;
                for (ThreatEntry t : threats) {
                    if (!t.tracked.entity.isAlive()) continue;
                    if (best == null) best = t;
                    if (t.tracked.hasLineOfSight) { best = t; break; }
                }
                if (best != null) {
                    return new PathingCommand(
                        new GoalNear(best.tracked.entity.blockPosition(), 2),
                        PathingCommandType.REVALIDATE_GOAL_AND_PATH);
                }
                return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
            }
            default:
                return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
        }
    }

    @Override
    public void onLostControl() {}

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
