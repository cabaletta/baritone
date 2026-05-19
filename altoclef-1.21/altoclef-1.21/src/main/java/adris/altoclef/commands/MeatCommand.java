package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.commandsystem.Arg;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.commandsystem.CommandException;
import adris.altoclef.tasks.resources.CollectMeatTask;

public class MeatCommand extends Command {
    public MeatCommand() throws CommandException {
        super("meat", "Collects a certain amount of meat", new Arg<>(Integer.class, "count"));
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) throws CommandException {
        mod.runUserTask(new CollectMeatTask(parser.get(Integer.class)), this::finish);
    }
}