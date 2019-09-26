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

import baritone.api.utils.command.exception.CommandException;

public interface IDatatypePost<T, O> extends IDatatype {

    /**
     * Takes the expected input and transforms it based on the value held by {@code original}. If {@code original}
     * is null, it is expected that the implementation of this method has a case to handle it, such that a
     * {@link NullPointerException} will never be thrown as a result.
     *
     * @param ctx      The datatype context
     * @param original The transformable value
     * @return The transformed value
     */
    T apply(IDatatypeContext ctx, O original) throws CommandException;
}
