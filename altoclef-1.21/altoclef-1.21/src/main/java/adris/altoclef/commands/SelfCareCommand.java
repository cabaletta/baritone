package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.tasks.entity.SelfCareTask;

public class SelfCareCommand extends Command {
    public SelfCareCommand() {
        super("selfcare", "Care for self (Not finished and tested yet)");
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) {
        mod.runUserTask(new SelfCareTask(), this::finish);
    }
}
