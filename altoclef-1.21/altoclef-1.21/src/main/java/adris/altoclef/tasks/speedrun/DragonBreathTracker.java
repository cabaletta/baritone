package adris.altoclef.tasks.speedrun;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.movement.CustomBaritoneGoalTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalRunAway;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

import java.util.HashSet;
import java.util.Optional;

public class DragonBreathTracker {
    private final HashSet<BlockPos> _breathBlocks = new HashSet<>();

    public void updateBreath(AltoClef mod) {
        _breathBlocks.clear();
        Optional<Entity> cloud = mod.getEntityTracker().getClosestEntity(AreaEffectCloudEntity.class);
        if (cloud.isPresent()) {
            for (BlockPos bad : WorldHelper.getBlocksTouchingBox(mod, cloud.get().getBoundingBox())) {
                _breathBlocks.add(bad);
            }
        }
    }

    public boolean isTouchingDragonBreath(BlockPos pos) {
        return _breathBlocks.contains(pos);
    }

    public Task getRunAwayTask() {
        return new RunAwayFromDragonsBreathTask();
    }

    private class RunAwayFromDragonsBreathTask extends CustomBaritoneGoalTask {

        @Override
        protected void onStart(AltoClef mod) {
            super.onStart(mod);
            mod.getBehaviour().push();
            mod.getBehaviour().setBlockPlacePenalty(Double.POSITIVE_INFINITY);
            // do NOT ever wander
            _checker = new MovementProgressChecker((int) Float.POSITIVE_INFINITY);
        }

        @Override
        protected void onStop(AltoClef mod, Task interruptTask) {
            super.onStop(mod, interruptTask);
            mod.getBehaviour().pop();
        }

        @Override
        protected Goal newGoal(AltoClef mod) {
            return new GoalRunAway(10, _breathBlocks.toArray(BlockPos[]::new));
        }

        @Override
        protected boolean isEqual(Task other) {
            return other instanceof RunAwayFromDragonsBreathTask;
        }

        @Override
        protected String toDebugString() {
            return "ESCAPE Dragons Breath";
        }
    }
}
