package adris.altoclef.tasks.movement;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.baritone.GoalChunk;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import baritone.api.pathing.goals.Goal;
import net.minecraft.util.math.ChunkPos;

public class GetToChunkTask extends CustomBaritoneGoalTask {

    private final ChunkPos _pos;

    public GetToChunkTask(ChunkPos pos) {
        // Override checker to be more lenient, as we are traversing entire chunks here.
        _checker = new MovementProgressChecker();
        _pos = pos;
    }

    @Override
    protected Goal newGoal(AltoClef mod) {
        return new GoalChunk(_pos);
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof GetToChunkTask task) {
            return task._pos.equals(_pos);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Get to chunk: " + _pos.toString();
    }
}
