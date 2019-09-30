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

package baritone.api.utils.command.execution;

import baritone.api.utils.command.Command;
import baritone.api.utils.command.argument.CommandArgument;
import baritone.api.utils.command.helpers.arguments.ArgConsumer;
import net.minecraft.util.Tuple;

import java.util.List;
import java.util.stream.Stream;

/**
 * @author Brady
 * @since 9/28/2019
 */
public interface ICommandExecution {

    /**
     * @return The label that was used to target the {@link Command}
     */
    String getLabel();

    /**
     * @return The arguments to be passed to the {@link Command}
     */
    ArgConsumer getArguments();

    /**
     * Executes the target command for this {@link ICommandExecution}. This method should never
     * {@code throw} any exception, anything that is thrown during the target command execution
     * should be safely handled.
     */
    void execute();

    /**
     * Forwards this {@link ICommandExecution} to the target {@link Command} to perform a tab-completion.
     * If the tab-completion operation is a failure, then {@link Stream#empty()} will be returned.
     *
     * @return The tab-completed arguments, if possible.
     */
    Stream<String> tabComplete();

    static Tuple<String, List<CommandArgument>> expand(String string, boolean preserveEmptyLast) {
        String label = string.split("\\s", 2)[0];
        List<CommandArgument> args = CommandArgument.from(string.substring(label.length()), preserveEmptyLast);
        return new Tuple<>(label, args);
    }

    static Tuple<String, List<CommandArgument>> expand(String string) {
        return expand(string, false);
    }
}
