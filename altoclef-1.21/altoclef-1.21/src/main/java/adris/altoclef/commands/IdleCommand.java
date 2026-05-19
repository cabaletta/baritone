package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.tasks.movement.IdleTask;

public class IdleCommand extends Command {
    public IdleCommand() {
        super("idle", "Stand still");
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) {
        mod.runUserTask(new IdleTask(), this::finish);
    }
}
