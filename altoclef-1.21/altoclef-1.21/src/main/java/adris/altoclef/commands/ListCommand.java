package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.commandsystem.CommandException;
import adris.altoclef.ui.MessagePriority;

import java.util.Arrays;

public class ListCommand extends Command {
    public ListCommand() {
        super("list", "List all obtainable items");
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) throws CommandException {
        mod.log("#### LIST OF ALL OBTAINABLE ITEMS ####", MessagePriority.OPTIONAL);
        mod.log(Arrays.toString(TaskCatalogue.resourceNames().toArray()), MessagePriority.OPTIONAL);
        mod.log("############# END LIST ###############", MessagePriority.OPTIONAL);
    }
}
