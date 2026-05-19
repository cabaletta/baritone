package adris.altoclef.tasks.movement;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.baritone.GoalRunAwayFromEntities;
import baritone.api.pathing.goals.Goal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.SkeletonEntity;

import java.util.List;
import java.util.Optional;

public class RunAwayFromHostilesTask extends CustomBaritoneGoalTask {

    private final double _distanceToRun;
    private final boolean _includeSkeletons;

    public RunAwayFromHostilesTask(double distance, boolean includeSkeletons) {
        _distanceToRun = distance;
        _includeSkeletons = includeSkeletons;
    }

    public RunAwayFromHostilesTask(double distance) {
        this(distance, false);
    }


    @Override
    protected Goal newGoal(AltoClef mod) {
        // We want to run away NOW
        mod.getClientBaritone().getPathingBehavior().forceCancel();
        return new GoalRunAwayFromHostiles(mod, _distanceToRun);
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof RunAwayFromHostilesTask task) {
            return Math.abs(task._distanceToRun - _distanceToRun) < 1;
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "NIGERUNDAYOO, SUMOOKEYY!";
    }

    private class GoalRunAwayFromHostiles extends GoalRunAwayFromEntities {

        public GoalRunAwayFromHostiles(AltoClef mod, double distance) {
            super(mod, distance, false, 0.8);
        }

        @Override
        protected Optional<Entity> getEntities(AltoClef mod) {
            List<Entity> hostiles = mod.getEntityTracker().getHostiles();
            if (!hostiles.isEmpty()) {
                for (Entity hostile : hostiles) {
                    Optional<Entity> closestHostile = mod.getEntityTracker().getClosestEntity(hostile.getClass());
                    if (closestHostile.isPresent()) {
                        if (!_includeSkeletons && closestHostile.get() instanceof SkeletonEntity) {
                            return Optional.empty();
                        }
                        return closestHostile;
                    }
                }
            }
            return Optional.empty();
        }
    }
}
