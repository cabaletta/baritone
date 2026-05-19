package adris.altoclef.tasks.movement;

import adris.altoclef.AltoClef;
import adris.altoclef.Playground;
import adris.altoclef.tasksystem.Task;

/**
 * Do nothing.
 */
public class IdleTask extends Task {
    @Override
    protected void onStart(AltoClef mod) {

    }

    @Override
    protected Task onTick(AltoClef mod) {
        // Do nothing except maybe test code
        Playground.IDLE_TEST_TICK_FUNCTION(mod);
        return null;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {

    }

    @Override
    public boolean isFinished(AltoClef mod) {
        // Never finish
        return false;
    }

    @Override
    protected boolean isEqual(Task other) {
        return other instanceof IdleTask;
    }

    @Override
    protected String toDebugString() {
        return "Idle";
    }
}
