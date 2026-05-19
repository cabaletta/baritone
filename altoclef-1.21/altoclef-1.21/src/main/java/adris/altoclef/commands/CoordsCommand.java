package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.util.helpers.WorldHelper;

public class CoordsCommand extends Command {
    public CoordsCommand() {
        super("coords", "Get the bot's current coordinates");
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) {
        mod.log("CURRENT COORDINATES: " + mod.getPlayer().getBlockPos().toShortString() + " (Current dimension: " + WorldHelper.getCurrentDimension() + ")");
        finish();
    }
}
