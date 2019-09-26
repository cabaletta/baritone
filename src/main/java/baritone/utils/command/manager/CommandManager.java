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

package baritone.utils.command.manager;

import baritone.Baritone;
import baritone.api.IBaritone;
import baritone.api.utils.command.Command;
import baritone.api.utils.command.argument.CommandArgument;
import baritone.api.utils.command.execution.CommandExecution;
import baritone.api.utils.command.helpers.tabcomplete.TabCompleteHelper;
import baritone.api.utils.command.manager.ICommandManager;
import baritone.api.utils.command.registry.Registry;
import baritone.utils.command.defaults.DefaultCommands;
import com.mojang.realmsclient.util.Pair;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * @author Brady
 * @since 9/21/2019
 */
public class CommandManager implements ICommandManager {

    private final Registry<Command> registry = new Registry<>();
    private final Baritone baritone;

    public CommandManager(Baritone baritone) {
        this.baritone = baritone;
        DefaultCommands.commands(baritone).forEach(this.registry::register);
    }

    @Override
    public IBaritone getBaritone() {
        return this.baritone;
    }

    @Override
    public Registry<Command> getRegistry() {
        return this.registry;
    }

    @Override
    public Command getCommand(String name) {
        for (Command command : this.registry.entries) {
            if (command.names.contains(name.toLowerCase(Locale.US))) {
                return command;
            }
        }
        return null;
    }

    @Override
    public void execute(CommandExecution execution) {
        execution.execute();
    }

    @Override
    public boolean execute(String string) {
        CommandExecution execution = CommandExecution.from(this, string);
        if (execution != null) {
            execution.execute();
        }
        return execution != null;
    }

    @Override
    public Stream<String> tabComplete(CommandExecution execution) {
        return execution.tabComplete();
    }

    @Override
    public Stream<String> tabComplete(Pair<String, List<CommandArgument>> pair) {
        CommandExecution execution = CommandExecution.from(this, pair);
        return execution == null ? Stream.empty() : tabComplete(execution);
    }

    @Override
    public Stream<String> tabComplete(String prefix) {
        Pair<String, List<CommandArgument>> pair = CommandExecution.expand(prefix, true);
        String label = pair.first();
        List<CommandArgument> args = pair.second();
        if (args.isEmpty()) {
            return new TabCompleteHelper()
                    .addCommands(this.baritone.getCommandManager())
                    .filterPrefix(label)
                    .stream();
        } else {
            return tabComplete(pair);
        }
    }
}
