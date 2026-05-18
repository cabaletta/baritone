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

/**
 * Translates the current ActionIntent from AwarenessContext into a Baritone Goal.
 * Runs at priority 10.0, above all built-in processes, but only activates when
 * the overall danger level exceeds the AwarenessContext threshold.
 */
public final class CombatProcess extends BaritoneProcessHelper {

    private static final double RETREAT_DISTANCE = 20.0;

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

        switch (intent.mode) {
            case RETREAT: {
                ThreatEntry primary = awarenessCtx.getPrimaryThreat();
                if (primary == null || !primary.tracked.entity.isAlive()) {
                    return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
                }
                BlockPos threatPos = primary.tracked.entity.blockPosition();
                Goal goal = new GoalRunAway(RETREAT_DISTANCE, threatPos);
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
                    return new PathingCommand(new GoalBlock(target), PathingCommandType.REVALIDATE_GOAL_AND_PATH);
                }
                return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
            }
            case ATTACK: {
                if (intent.targetEntity != null && intent.targetEntity.isAlive()) {
                    BlockPos targetPos = intent.targetEntity.blockPosition();
                    return new PathingCommand(new GoalNear(targetPos, 2),
                        PathingCommandType.REVALIDATE_GOAL_AND_PATH);
                }
                return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
            }
            default:
                return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
        }
    }

    @Override
    public void onLostControl() {
        // Awareness context drives activation; no state to clear here.
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
            + String.format("%.2f", awarenessCtx.getOverallDangerLevel());
    }
}
