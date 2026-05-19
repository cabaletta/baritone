package adris.altoclef.tasks.movement;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.helpers.WorldHelper;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalYLevel;

public class GetToYTask extends CustomBaritoneGoalTask {

    private final int _yLevel;
    private final Dimension _dimension;

    public GetToYTask(int ylevel, Dimension dimension) {
        _yLevel = ylevel;
        _dimension = dimension;
    }

    public GetToYTask(int ylevel) {
        this(ylevel, null);
    }

    @Override
    protected Task onTick(AltoClef mod) {
        if (_dimension != null && WorldHelper.getCurrentDimension() != _dimension) {
            return new DefaultGoToDimensionTask(_dimension);
        }
        return super.onTick(mod);
    }

    @Override
    protected Goal newGoal(AltoClef mod) {
        return new GoalYLevel(_yLevel);
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof GetToYTask task) {
            return task._yLevel == _yLevel;
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Going to y=" + _yLevel + (_dimension != null ? ("in dimension" + _dimension) : "");
    }
}
