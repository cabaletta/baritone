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
import baritone.api.utils.command.registry.Registry;
import com.mojang.realmsclient.util.Pair;

import java.util.List;
import java.util.stream.Stream;

/**
 * @author Brady
 * @since 9/21/2019
 */
public interface ICommandManager {

    Registry<Command> getRegistry();

    /**
     * @param name The command name to search for.
     * @return The command, if found.
     */
    Command getCommand(String name);

    void execute(CommandExecution execution);

    boolean execute(String string);

    Stream<String> tabComplete(CommandExecution execution);

    Stream<String> tabComplete(Pair<String, List<CommandArgument>> pair);

    Stream<String> tabComplete(String prefix);
}
