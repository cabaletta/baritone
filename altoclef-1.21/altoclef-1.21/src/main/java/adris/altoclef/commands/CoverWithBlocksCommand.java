package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.commandsystem.CommandException;
import adris.altoclef.tasks.construction.CoverWithBlocksTask;

public class CoverWithBlocksCommand extends Command {
    public CoverWithBlocksCommand() {
        super("coverwithblocks", "Cover nether lava with blocks");
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) throws CommandException {
        mod.runUserTask(new CoverWithBlocksTask(), this::finish);
    }
}
