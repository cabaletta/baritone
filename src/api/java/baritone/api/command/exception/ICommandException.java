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

package baritone.api.command.exception;

import baritone.api.command.ICommand;
import baritone.api.command.argument.IArgConsumer;
import baritone.api.command.argument.ICommandArgument;
import net.minecraft.util.text.TextFormatting;

import java.util.List;

import static baritone.api.utils.Helper.HELPER;

/**
 * The base for a Baritone Command Exception, checked or unchecked. Provides a
 * {@link #handle(ICommand, List)} method that is used to provide useful output
 * to the user for diagnosing issues that may have occurred during execution.
 * <p>
 * Anything implementing this interface should be assignable to {@link Exception}.
 *
 * @author Brady
 * @since 9/20/2019
 */
public interface ICommandException {

    /**
     * @return The exception details
     * @see Exception#getMessage()
     */
    String getMessage();

    /**
     * Called when this exception is thrown, to handle the exception.
     *
     * @param command The command that threw it.
     * @param args    The arguments the command was called with.
     */
    default void handle(ICommand command, List<ICommandArgument> args) {
        HELPER.logDirect(this.getMessage(), TextFormatting.RED);
    }

    default void handle(ICommand command, IArgConsumer args) {
        handle(command, args.getArgs());
    }
}
