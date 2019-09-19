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
import net.minecraft.util.text.TextFormatting;

import java.util.List;

import static baritone.api.utils.Helper.HELPER;

public abstract class CommandErrorMessageException extends CommandException {

    protected CommandErrorMessageException(String reason) {
        super(reason);
    }

    @Override
    public void handle(Command command, List<CommandArgument> args) {
        HELPER.logDirect(getMessage(), TextFormatting.RED);
    }
}
