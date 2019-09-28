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

package baritone.utils.command.execution;

import baritone.api.utils.command.Command;
import baritone.api.utils.command.exception.CommandUnhandledException;
import baritone.api.utils.command.exception.ICommandException;
import baritone.api.utils.command.execution.ICommandExecution;
import baritone.api.utils.command.helpers.arguments.ArgConsumer;
import baritone.utils.command.manager.CommandManager;

import java.util.stream.Stream;

/**
 * The default, internal implementation of {@link ICommandExecution}, which is used by {@link CommandManager}
 *
 * @author LoganDark, Brady
 */
public class CommandExecution implements ICommandExecution {

    /**
     * The command itself
     */
    private final Command command;

    /**
     * The name this command was called with
     */
    private final String label;

    /**
     * The arg consumer
     */
    private final ArgConsumer args;

    public CommandExecution(Command command, String label, ArgConsumer args) {
        this.command = command;
        this.label = label;
        this.args = args;
    }

    @Override
    public String getLabel() {
        return this.label;
    }

    @Override
    public ArgConsumer getArguments() {
        return this.args;
    }

    @Override
    public void execute() {
        try {
            command.execute(this);
        } catch (Throwable t) {
            // Create a handleable exception, wrap if needed
            ICommandException exception = t instanceof ICommandException
                    ? (ICommandException) t
                    : new CommandUnhandledException(t);

            exception.handle(command, args.args);
        }
    }

    @Override
    public Stream<String> tabComplete() {
        return command.tabComplete(this);
    }
}
