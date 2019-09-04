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

import baritone.api.utils.command.exception.CommandInvalidArgumentException;
import baritone.api.utils.command.helpers.arguments.ArgConsumer;

import java.util.stream.Stream;

/**
 * Since interfaces cannot enforce the presence of a constructor, it's on you to make sure there is a constructor that
 * accepts a single {@link ArgConsumer} argument. The constructor will perform all needed validation, and
 * {@link ArgConsumer#getDatatype(Class)} will handle RuntimeExceptions and translate them into
 * {@link CommandInvalidArgumentException}s. There must always be a constructor with no arguments so that
 * {@link ArgConsumer} can create an instance for tab completion.
 */
public interface IDatatype {
    Stream<String> tabComplete(ArgConsumer consumer);
}
