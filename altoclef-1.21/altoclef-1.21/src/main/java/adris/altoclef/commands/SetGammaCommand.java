package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.commandsystem.Arg;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.commandsystem.CommandException;
import net.minecraft.client.MinecraftClient;

public class SetGammaCommand extends Command {

    public SetGammaCommand() throws CommandException {
        super("gamma", "Sets the brightness to a value", new Arg<>(Double.class, "gamma", 1.0, 0));
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) throws CommandException {
        double gammaValue = parser.get(Double.class);
        Debug.logMessage("Gamma set to " + gammaValue);
        MinecraftClient.getInstance().options.getGamma().setValue(gammaValue);
    }
}
