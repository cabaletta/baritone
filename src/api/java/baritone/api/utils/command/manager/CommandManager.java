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

package baritone.api.utils.command.manager;

import baritone.api.utils.command.Command;
import baritone.api.utils.command.argument.CommandArgument;
import baritone.api.utils.command.execution.CommandExecution;
import baritone.api.utils.command.helpers.tabcomplete.TabCompleteHelper;
import baritone.api.utils.command.registry.Registry;
import com.mojang.realmsclient.util.Pair;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class CommandManager {
    public static final Registry<Command> REGISTRY = new Registry<>();

    /**
     * @param name The command name to search for.
     * @return The command, if found.
     */
    public static Command getCommand(String name) {
        for (Command command : REGISTRY.entries) {
            if (command.names.contains(name.toLowerCase(Locale.US))) {
                return command;
            }
        }

        return null;
    }

    public static void execute(CommandExecution execution) {
        execution.execute();
    }

    public static boolean execute(String string) {
        CommandExecution execution = CommandExecution.from(string);

        if (nonNull(execution)) {
            execution.execute();
        }

        return nonNull(execution);
    }

    public static Stream<String> tabComplete(CommandExecution execution) {
        return execution.tabComplete();
    }

    public static Stream<String> tabComplete(Pair<String, List<CommandArgument>> pair) {
        CommandExecution execution = CommandExecution.from(pair);
        return isNull(execution) ? Stream.empty() : tabComplete(execution);
    }

    public static Stream<String> tabComplete(String prefix) {
        Pair<String, List<CommandArgument>> pair = CommandExecution.expand(prefix, true);
        String label = pair.first();
        List<CommandArgument> args = pair.second();

        if (args.isEmpty()) {
            return new TabCompleteHelper()
                .addCommands()
                .filterPrefix(label)
                .stream();
        } else {
            return tabComplete(pair);
        }
    }
}
