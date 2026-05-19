package adris.altoclef.tasks.movement;

import adris.altoclef.AltoClef;
import adris.altoclef.util.baritone.GoalRunAwayFromEntities;
import baritone.api.pathing.goals.Goal;
import net.minecraft.entity.Entity;

import java.util.Optional;

public abstract class RunAwayFromEntitiesTask extends CustomBaritoneGoalTask {

    private final Entity _runAwaySupplier;

    private final double _distanceToRun;
    private final boolean _xz;
    // See GoalrunAwayFromEntities penalty value
    private final double _penalty;

    public RunAwayFromEntitiesTask(Entity toRunAwayFrom, double distanceToRun, boolean xz, double penalty) {
        _runAwaySupplier = toRunAwayFrom;
        _distanceToRun = distanceToRun;
        _xz = xz;
        _penalty = penalty;
    }

    public RunAwayFromEntitiesTask(Entity toRunAwayFrom, double distanceToRun, double penalty) {
        this(toRunAwayFrom, distanceToRun, false, penalty);
    }


    @Override
    protected Goal newGoal(AltoClef mod) {
        return new GoalRunAwayStuff(mod, _distanceToRun, _xz);
    }


    private class GoalRunAwayStuff extends GoalRunAwayFromEntities {

        public GoalRunAwayStuff(AltoClef mod, double distance, boolean xz) {
            super(mod, distance, xz, _penalty);
        }

        @Override
        protected Optional<Entity> getEntities(AltoClef mod) {
            return Optional.ofNullable(_runAwaySupplier);
        }
    }
}
