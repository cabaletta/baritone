package adris.altoclef.tasks.slot;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.slots.Slot;

public class ThrowCursorTask extends Task {

    private final Task _throwTask = new ClickSlotTask(Slot.UNDEFINED);

    @Override
    protected void onStart(AltoClef mod) {
    }

    @Override
    protected Task onTick(AltoClef mod) {
        return _throwTask;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {

    }

    @Override
    protected boolean isEqual(Task obj) {
        return obj instanceof ThrowCursorTask;
    }

    @Override
    protected String toDebugString() {
        return "Throwing Cursor";
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return _throwTask.isFinished(mod);
    }
}
