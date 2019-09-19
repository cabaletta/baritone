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

package baritone.api.utils.command.exception;

import baritone.api.utils.command.Command;
import baritone.api.utils.command.argument.CommandArgument;

import java.util.List;

public abstract class CommandException extends RuntimeException {
    protected CommandException(String reason) {
        super(reason);
    }

    /**
     * Called when this exception is thrown, to handle the exception.
     *
     * @param command The command that threw it.
     * @param args    The arguments the command was called with.
     */
    public abstract void handle(Command command, List<CommandArgument> args);
}
