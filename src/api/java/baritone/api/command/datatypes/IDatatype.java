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

package baritone.api.command.datatypes;

import baritone.api.command.argparser.IArgParser;
import baritone.api.command.argument.IArgConsumer;
import baritone.api.command.exception.CommandException;

import java.util.stream.Stream;

/**
 * An {@link IDatatype} is similar to an {@link IArgParser} in the sense that it is capable of consuming an argument
 * to transform it into a usable form as the code desires.
 * <p>
 * A fundamental difference is that an {@link IDatatype} is capable of consuming multiple arguments. For example,
 * {@link RelativeBlockPos} is an {@link IDatatypePost} which requires at least 3 {@link RelativeCoordinate} arguments
 * to be specified.
 * <p>
 * Another difference is that an {@link IDatatype} can be tab-completed, providing comprehensive auto completion
 * that can substitute based on existing input or provide possibilities for the next piece of input.
 *
 * @see IDatatypeContext
 * @see IDatatypeFor
 * @see IDatatypePost
 */
public interface IDatatype {

    /**
     * Attempts to complete missing or partial input provided through the {@link IArgConsumer}} provided by
     * {@link IDatatypeContext#getConsumer()} in order to aide the user in executing commands.
     * <p>
     * One benefit over datatypes over {@link IArgParser}s is that instead of each command trying to guess what values
     * the datatype will accept, or simply not tab completing at all, datatypes that support tab completion can provide
     * accurate information using the same methods used to parse arguments in the first place.
     *
     * @param ctx The argument consumer to tab complete
     * @return A stream representing the strings that can be tab completed. DO NOT INCLUDE SPACES IN ANY STRINGS.
     * @see IArgConsumer#tabCompleteDatatype(IDatatype)
     */
    Stream<String> tabComplete(IDatatypeContext ctx) throws CommandException;
}
