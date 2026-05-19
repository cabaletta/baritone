package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.tasksystem.Task;

import java.util.List;

public class StatusCommand extends Command {
    public StatusCommand() {
        super("status", "Get status of currently executing command");
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) {
        List<Task> tasks = mod.getUserTaskChain().getTasks();
        if (tasks.size() == 0) {
            mod.log("No tasks currently running.");
        } else {
            mod.log("CURRENT TASK: " + tasks.get(0).toString());
        }
        finish();
    }
}