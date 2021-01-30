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

import baritone.api.command.exception.CommandException;

import java.util.function.Supplier;

/**
 * An {@link IDatatype} which acts as a {@link Supplier}, in essence. The only difference
 * is that it requires an {@link IDatatypeContext} to be provided due to the expectation that
 * implementations of {@link IDatatype} are singletons.
 */
public interface IDatatypeFor<T> extends IDatatype {

    /**
     * Consumes the desired amount of arguments from the specified {@link IDatatypeContext}, and
     * then returns the parsed value. This method will more than likely return a {@link IllegalArgumentException}
     * if the expected input does not conform to a parseable value. As far as a {@link CommandException} being
     * thrown is concerned, see the note below for specifics.
     *
     * @param ctx The context
     * @return The parsed data-type
     * @throws CommandException If there was an issue parsing using another type or arguments could not be polled.
     * @see IDatatypeContext
     */
    T get(IDatatypeContext ctx) throws CommandException;
}
