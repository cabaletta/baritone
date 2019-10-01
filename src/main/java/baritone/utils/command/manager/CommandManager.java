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
import baritone.api.utils.command.execution.ICommandExecution;
import baritone.api.utils.command.helpers.arguments.ArgConsumer;
import baritone.api.utils.command.helpers.tabcomplete.TabCompleteHelper;
import baritone.api.utils.command.manager.ICommandManager;
import baritone.api.utils.command.registry.Registry;
import baritone.utils.command.defaults.DefaultCommands;
import baritone.utils.command.execution.CommandExecution;
import net.minecraft.util.Tuple;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * The default, internal implementation of {@link ICommandManager}
 *
 * @author Brady
 * @since 9/21/2019
 */
public class CommandManager implements ICommandManager {

    private final Registry<Command> registry = new Registry<>();
    private final Baritone baritone;

    public CommandManager(Baritone baritone) {
        this.baritone = baritone;
        DefaultCommands.createAll(baritone).forEach(this.registry::register);
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
    public boolean execute(String string) {
        return this.execute(ICommandExecution.expand(string));
    }

    @Override
    public boolean execute(Tuple<String, List<CommandArgument>> expanded) {
        ICommandExecution execution = this.from(expanded);
        if (execution != null) {
            execution.execute();
        }
        return execution != null;
    }

    @Override
    public Stream<String> tabComplete(Tuple<String, List<CommandArgument>> expanded) {
        ICommandExecution execution = this.from(expanded);
        return execution == null ? Stream.empty() : execution.tabComplete();
    }

    @Override
    public Stream<String> tabComplete(String prefix) {
        Tuple<String, List<CommandArgument>> pair = ICommandExecution.expand(prefix, true);
        String label = pair.getA();
        List<CommandArgument> args = pair.getB();
        if (args.isEmpty()) {
            return new TabCompleteHelper()
                    .addCommands(this.baritone.getCommandManager())
                    .filterPrefix(label)
                    .stream();
        } else {
            return tabComplete(pair);
        }
    }

    private ICommandExecution from(Tuple<String, List<CommandArgument>> expanded) {
        String label = expanded.getA();
        ArgConsumer args = new ArgConsumer(this, expanded.getB());

        Command command = this.getCommand(label);
        return command == null ? null : new CommandExecution(command, label, args);
    }
}
