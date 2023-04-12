/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.command.defaults;

import baritone.api.IBaritone;
import baritone.api.command.Command;
import baritone.api.command.ICommand;
import baritone.api.command.argument.IArgConsumer;
import baritone.api.command.exception.CommandException;
import baritone.api.command.helpers.TabCompleteHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class QueueCommand extends Command {

    private List<ICommand> commandList;

    protected QueueCommand(IBaritone baritone, String... names) {
        super(baritone, "queue");
        //todo get a list from all commands excluding control commands like pause or cancel and the queue command
        commandList = new ArrayList<>();
        commandList.add(new GotoCommand(baritone));
        commandList.add(new WaitCommand(baritone));
        commandList.add(new MineCommand(baritone));
        commandList.add(new BuildCommand(baritone));
        commandList.add(new WaypointsCommand(baritone));
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        String name = args.getString();
        for (ICommand command : commandList) {
            if (command.getNames().contains(name)) {
                baritone.getCommandQueueProcess().addNewCommand(command, args);
                return; //we found the command dont need to check the rest.
            }
        }
        logDirect("Unknown command. Unable to add to queue.");
    }

    //todo tapcomplete should return command names for arg1. any further args should use the results of the tapComplete from the matching command at arg1.
    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        TabCompleteHelper helper = new TabCompleteHelper();
        String commandName = "noCommand";
        if (args.has(2)) {
            commandName = args.peekString();
        }
        for (ICommand command : commandList) { //first argument aka command name
            if (args.hasExactlyOne()) {
                helper.append(command.getNames().get(0));
            }
            if (command.getNames().contains(commandName)) {
                args.get(); //using the command name as it isnt used
                helper.append(command.tabComplete(command.getNames().get(0), args));
                return helper.filterPrefix(args.getString()).stream();
            }
        }
        return helper.filterPrefix(args.getString()).stream();
    }

    @Override
    public String getShortDesc() {
        return "Add a command to a waiting queue.";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Add a command to a waiting queue",
                "The command will be executed once all the previous commands have finished.",
                "",
                "> queue <Command> <command arguments> - adds a new command to the queue"
        );
    }
}
