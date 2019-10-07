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

package baritone.command.argparser;

import baritone.api.command.argparser.IArgParser;
import baritone.api.command.argparser.IArgParserManager;
import baritone.api.command.argument.ICommandArgument;
import baritone.api.command.exception.CommandInvalidTypeException;
import baritone.api.command.exception.CommandNoParserForTypeException;
import baritone.api.command.registry.Registry;

public enum ArgParserManager implements IArgParserManager {
    INSTANCE;

    public final Registry<IArgParser> registry = new Registry<>();

    ArgParserManager() {
        DefaultArgParsers.ALL.forEach(this.registry::register);
    }

    @Override
    public <T> IArgParser.Stateless<T> getParserStateless(Class<T> type) {
        //noinspection unchecked
        return this.registry.descendingStream()
                .filter(IArgParser.Stateless.class::isInstance)
                .map(IArgParser.Stateless.class::cast)
                .filter(parser -> parser.getTarget().isAssignableFrom(type))
                .findFirst()
                .orElse(null);
    }

    @Override
    public <T, S> IArgParser.Stated<T, S> getParserStated(Class<T> type, Class<S> stateKlass) {
        //noinspection unchecked
        return this.registry.descendingStream()
                .filter(IArgParser.Stated.class::isInstance)
                .map(IArgParser.Stated.class::cast)
                .filter(parser -> parser.getTarget().isAssignableFrom(type))
                .filter(parser -> parser.getStateType().isAssignableFrom(stateKlass))
                .map(IArgParser.Stated.class::cast)
                .findFirst()
                .orElse(null);
    }

    @Override
    public <T> T parseStateless(Class<T> type, ICommandArgument arg) throws CommandInvalidTypeException {
        IArgParser.Stateless<T> parser = this.getParserStateless(type);
        if (parser == null) {
            throw new CommandNoParserForTypeException(type);
        }
        try {
            return parser.parseArg(arg);
        } catch (Exception exc) {
            throw new CommandInvalidTypeException(arg, type.getSimpleName());
        }
    }

    @Override
    public <T, S> T parseStated(Class<T> type, Class<S> stateKlass, ICommandArgument arg, S state) throws CommandInvalidTypeException {
        IArgParser.Stated<T, S> parser = this.getParserStated(type, stateKlass);
        if (parser == null) {
            throw new CommandNoParserForTypeException(type);
        }
        try {
            return parser.parseArg(arg, state);
        } catch (Exception exc) {
            throw new CommandInvalidTypeException(arg, type.getSimpleName());
        }
    }

    @Override
    public Registry<IArgParser> getRegistry() {
        return this.registry;
    }
}
