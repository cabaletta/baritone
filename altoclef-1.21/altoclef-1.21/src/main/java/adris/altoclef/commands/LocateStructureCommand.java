package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.commandsystem.Arg;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.commandsystem.CommandException;
import adris.altoclef.tasks.movement.GoToStrongholdPortalTask;
import adris.altoclef.tasks.movement.LocateDesertTempleTask;

public class LocateStructureCommand extends Command {

    public LocateStructureCommand() throws CommandException {
        super("locate_structure", "Locate a world generated structure.", new Arg(Structure.class, "structure"));
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) throws CommandException {
        Structure structure = parser.get(Structure.class);
        switch (structure) {
            case STRONGHOLD:
                mod.runUserTask(new GoToStrongholdPortalTask(1), this::finish);
                break;
            case DESERT_TEMPLE:
                mod.runUserTask(new LocateDesertTempleTask(), this::finish);
                break;
        }
    }

    public enum Structure {
        DESERT_TEMPLE,
        STRONGHOLD
    }
}