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

import static java.util.Objects.isNull;

public class ArgParserManager {

    public static final Registry<ArgParser> REGISTRY = new Registry<>();

    static {
        DefaultArgParsers.ALL.forEach(REGISTRY::register);
    }

    /**
     * @param klass The class to search for.
     * @return A parser that can parse arguments into this class, if found.
     */
    public static <T> ArgParser.Stateless<T> getParserStateless(Class<T> klass) {
        //noinspection unchecked
        return REGISTRY.descendingStream()
                .filter(ArgParser.Stateless.class::isInstance)
                .map(ArgParser.Stateless.class::cast)
                .filter(parser -> parser.getKlass().isAssignableFrom(klass))
                .findFirst()
                .orElse(null);
    }

    /**
     * @param klass The class to search for.
     * @return A parser that can parse arguments into this class, if found.
     */
    public static <T, S> ArgParser.Stated<T, S> getParserStated(Class<T> klass, Class<S> stateKlass) {
        //noinspection unchecked
        return REGISTRY.descendingStream()
                .filter(ArgParser.Stated.class::isInstance)
                .map(ArgParser.Stated.class::cast)
                .filter(parser -> parser.getKlass().isAssignableFrom(klass))
                .filter(parser -> parser.getStateKlass().isAssignableFrom(stateKlass))
                .map(ArgParser.Stated.class::cast)
                .findFirst()
                .orElse(null);
    }

    /**
     * Attempt to parse the specified argument with a stateless {@link ArgParser} that outputs the specified class.
     *
     * @param klass The class to parse the argument into.
     * @param arg   The argument to parse.
     * @return An instance of the specified class.
     * @throws CommandNoParserForTypeException If no parser exists for that type
     * @throws CommandInvalidTypeException     If the parsing failed
     * @see ArgParser.Stateless
     */
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

    /**
     * Attempt to parse the specified argument with a stated {@link ArgParser} that outputs the specified class.
     *
     * @param klass The class to parse the argument into.
     * @param arg   The argument to parse.
     * @param state The state to pass to the {@link ArgParser.Stated}.
     * @return An instance of the specified class.
     * @throws CommandNoParserForTypeException If no parser exists for that type
     * @throws CommandInvalidTypeException     If the parsing failed
     * @see ArgParser.Stated
     */
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
