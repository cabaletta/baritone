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
import baritone.api.command.registry.Registry;
import baritone.command.manager.CommandManager;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class QueueCommand extends Command {

    private List<ICommand> commandList;

    protected QueueCommand(IBaritone baritone, String... names) {
        super(baritone, "queue");
        commandList = new ArrayList<>();
        commandList.add(new GotoCommand(baritone));
        commandList.add(new WaitCommand(baritone));
        commandList.add(new MineCommand(baritone));
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        ICommand com;
        String name = args.getString();
        for (ICommand command : commandList) {
            if (command.getNames().contains(name)) {
                baritone.getCommandQueueProcess().addNewCommand(command, args);
            }
        }
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        return null;
    }

    @Override
    public String getShortDesc() {
        return null;
    }

    @Override
    public List<String> getLongDesc() {
        return null;
    }
}
