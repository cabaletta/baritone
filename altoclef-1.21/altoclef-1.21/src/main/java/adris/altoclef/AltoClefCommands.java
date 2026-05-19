package adris.altoclef;

import adris.altoclef.commands.*;
import adris.altoclef.commandsystem.CommandException;

/**
 * Initializes altoclef's built in commands.
 */
public class AltoClefCommands {

    public AltoClefCommands() throws CommandException {
        // List commands here
        AltoClef.getCommandExecutor().registerNewCommand(
                new HelpCommand(),
                new GetCommand(),
                new FollowCommand(),
                new GiveCommand(),
                new EquipCommand(),
                new DepositCommand(),
                new StashCommand(),
                new GotoCommand(),
                new IdleCommand(),
                new CoordsCommand(),
                new StatusCommand(),
                new InventoryCommand(),
                new LocateStructureCommand(),
                new StopCommand(),
                new TestCommand(),
                new FoodCommand(),
                new MeatCommand(),
                new ReloadSettingsCommand(),
                new GamerCommand(),
                new MarvionCommand(),
                new PunkCommand(),
                new HeroCommand(),
                new SetGammaCommand(),
                new ListCommand(),
                new CoverWithSandCommand(),
                new CoverWithBlocksCommand(),
                new SelfCareCommand()
                //new TestMoveInventoryCommand(),
                //    new TestSwapInventoryCommand()
        );
    }
}
