package adris.altoclef.eventbus.events;

import adris.altoclef.tasksystem.Task;

public class TaskFinishedEvent {
    public double durationSeconds;
    public Task lastTaskRan;

    public TaskFinishedEvent(double durationSeconds, Task lastTaskRan) {
        this.durationSeconds = durationSeconds;
        this.lastTaskRan = lastTaskRan;
    }
}
