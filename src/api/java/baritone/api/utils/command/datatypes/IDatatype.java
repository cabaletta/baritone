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

package baritone.api.utils.command.datatypes;

import baritone.api.utils.command.argparser.IArgParser;
import baritone.api.utils.command.exception.CommandException;
import baritone.api.utils.command.helpers.arguments.ArgConsumer;

import java.util.stream.Stream;

/**
 * @see IDatatypeContext
 * @see IDatatypeFor
 * @see IDatatypePost
 */
public interface IDatatype {

    /**
     * One benefit over datatypes over {@link IArgParser}s is that instead of each command trying to guess what values
     * the datatype will accept, or simply not tab completing at all, datatypes that support tab completion can provide
     * accurate information using the same methods used to parse arguments in the first place.
     * <p>
     * See {@link RelativeFile} for a very advanced example of tab completion. You wouldn't want this pasted into every
     * command that uses files - right? Right?
     *
     * @param ctx The argument consumer to tab complete
     * @return A stream representing the strings that can be tab completed. DO NOT INCLUDE SPACES IN ANY STRINGS.
     * @see ArgConsumer#tabCompleteDatatype(IDatatype)
     */
    Stream<String> tabComplete(IDatatypeContext ctx) throws CommandException;
}
