package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.commandsystem.CommandException;
import adris.altoclef.tasks.entity.HeroTask;

public class HeroCommand extends Command {
    public HeroCommand() {
        super("hero", "Kill all hostile mobs");
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) throws CommandException {
        mod.runUserTask(new HeroTask(), this::finish);
    }
}
