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

package baritone.api.utils.command.argparser;

import baritone.api.utils.command.argument.CommandArgument;
import baritone.api.utils.command.exception.CommandInvalidTypeException;
import baritone.api.utils.command.exception.CommandNoParserForTypeException;
import baritone.api.utils.command.registry.Registry;

import java.util.Iterator;

import static java.util.Objects.isNull;

public class ArgParserManager {
    public static final Registry<ArgParser> REGISTRY = new Registry<>();

    static {
        DefaultArgParsers.all.forEach(REGISTRY::register);
    }

    /**
     * @param klass The class to search for.
     * @return A parser that can parse arguments into this class, if found.
     */
    public static <T> ArgParser.Stateless<T> getParserStateless(Class<T> klass) {
        for (Iterator<ArgParser> it = REGISTRY.descendingIterator(); it.hasNext(); ) {
            ArgParser<?> parser = it.next();

            if (parser instanceof ArgParser.Stateless && parser.getKlass().isAssignableFrom(klass)) {
                //noinspection unchecked
                return (ArgParser.Stateless<T>) parser;
            }
        }

        return null;
    }

    /**
     * @param klass The class to search for.
     * @return A parser that can parse arguments into this class, if found.
     */
    public static <T, S> ArgParser.Stated<T, S> getParserStated(Class<T> klass, Class<S> stateKlass) {
        for (Iterator<ArgParser> it = REGISTRY.descendingIterator(); it.hasNext(); ) {
            ArgParser<?> parser = it.next();

            //noinspection unchecked
            if (parser instanceof ArgParser.Stated
                && parser.getKlass().isAssignableFrom(klass)
                && ((ArgParser.Stated) parser).getStateKlass().isAssignableFrom(stateKlass)) {
                //noinspection unchecked
                return (ArgParser.Stated<T, S>) parser;
            }
        }

        return null;
    }

    public static <T> T parseStateless(Class<T> klass, CommandArgument arg) {
        ArgParser.Stateless<T> parser = getParserStateless(klass);

        if (isNull(parser)) {
            throw new CommandNoParserForTypeException(klass);
        }

        try {
            return parser.parseArg(arg);
        } catch (RuntimeException exc) {
            throw new CommandInvalidTypeException(arg, klass.getSimpleName());
        }
    }

    public static <T, S> T parseStated(Class<T> klass, Class<S> stateKlass, CommandArgument arg, S state) {
        ArgParser.Stated<T, S> parser = getParserStated(klass, stateKlass);

        if (isNull(parser)) {
            throw new CommandNoParserForTypeException(klass);
        }

        try {
            return parser.parseArg(arg, state);
        } catch (RuntimeException exc) {
            throw new CommandInvalidTypeException(arg, klass.getSimpleName());
        }
    }
}
