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

package baritone.api.utils.command.helpers.arguments;

import baritone.api.utils.command.argument.CommandArgument;
import baritone.api.utils.command.datatypes.IDatatype;
import baritone.api.utils.command.datatypes.IDatatypeFor;
import baritone.api.utils.command.datatypes.IDatatypePost;
import baritone.api.utils.command.exception.CommandException;
import baritone.api.utils.command.exception.CommandInvalidTypeException;
import baritone.api.utils.command.exception.CommandNotEnoughArgumentsException;
import baritone.api.utils.command.exception.CommandTooManyArgumentsException;
import baritone.api.utils.command.exception.CommandUnhandledException;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

public class ArgConsumer {
    public final List<CommandArgument> args;
    public final Deque<CommandArgument> consumed;

    private ArgConsumer(List<CommandArgument> args, Deque<CommandArgument> consumed) {
        this.args = new ArrayList<>(args);
        this.consumed = new LinkedList<>(consumed);
    }

    public ArgConsumer(List<CommandArgument> args) {
        this(args, new LinkedList<>());
    }

    public boolean has(int num) {
        return args.size() >= num;
    }

    public boolean has() {
        return has(1);
    }

    public boolean hasAtMost(int num) {
        return args.size() <= num;
    }

    public boolean hasAtMostOne() {
        return hasAtMost(1);
    }

    public boolean hasExactly(int num) {
        return args.size() == num;
    }

    public boolean hasExactlyOne() {
        return hasExactly(1);
    }

    public CommandArgument peek(int index) {
        requireMin(1);
        return args.get(index);
    }

    public CommandArgument peek() {
        return peek(0);
    }

    public boolean is(Class<?> type) {
        return peek().is(type);
    }

    public String peekString(int index) {
        return peek(index).value;
    }

    public String peekString() {
        return peekString(0);
    }

    public <E extends Enum<?>> E peekEnum(Class<E> enumClass) {
        return peek().getEnum(enumClass);
    }

    public <E extends Enum<?>> E peekEnumOrNull(Class<E> enumClass) {
        try {
            return peekEnum(enumClass);
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    public <T> T peekAs(Class<T> type, int index) {
        return peek(index).getAs(type);
    }

    public <T> T peekAs(Class<T> type) {
        return peekAs(type, 0);
    }

    public <T> T peekAsOrDefault(Class<T> type, T def, int index) {
        try {
            return peekAs(type, index);
        } catch (CommandInvalidTypeException e) {
            return def;
        }
    }

    public <T> T peekAsOrDefault(Class<T> type, T def) {
        return peekAsOrDefault(type, def, 0);
    }

    public <T> T peekAsOrNull(Class<T> type, int index) {
        return peekAsOrDefault(type, null, 0);
    }

    public <T> T peekAsOrNull(Class<T> type) {
        return peekAsOrNull(type, 0);
    }

    public <T extends IDatatype> T peekDatatype(Class<T> datatype) {
        return clone().getDatatype(datatype);
    }

    public <T extends IDatatype> T peekDatatypeOrNull(Class<T> datatype) {
        return new ArgConsumer(args, consumed).getDatatypeOrNull(datatype);
    }

    public <T, O, D extends IDatatypePost<T, O>> T peekDatatypePost(Class<D> datatype, O original) {
        return new ArgConsumer(args, consumed).getDatatypePost(datatype, original);
    }

    public <T, O, D extends IDatatypePost<T, O>> T peekDatatypePostOrDefault(Class<D> datatype, O original, T def) {
        return new ArgConsumer(args, consumed).getDatatypePostOrDefault(datatype, original, def);
    }

    public <T, O, D extends IDatatypePost<T, O>> T peekDatatypePostOrNull(Class<D> datatype, O original) {
        return peekDatatypePostOrDefault(datatype, original, null);
    }

    public <T, D extends IDatatypeFor<T>> T peekDatatypeFor(Class<D> datatype) {
        return new ArgConsumer(args, consumed).peekDatatypeFor(datatype);
    }

    public <T, D extends IDatatypeFor<T>> T peekDatatypeForOrDefault(Class<D> datatype, T def) {
        return new ArgConsumer(args, consumed).peekDatatypeForOrDefault(datatype, def);
    }

    public <T, D extends IDatatypeFor<T>> T peekDatatypeForOrNull(Class<D> datatype, T def) {
        return peekDatatypeForOrDefault(datatype, null);
    }

    public CommandArgument get() {
        requireMin(1);
        CommandArgument arg = args.remove(0);
        consumed.add(arg);
        return arg;
    }

    public String getString() {
        return get().value;
    }

    public <E extends Enum<?>> E getEnum(Class<E> enumClass) {
        try {
            return get().getEnum(enumClass);
        } catch (NoSuchElementException e) {
            throw new CommandInvalidTypeException(consumed(), enumClass.getSimpleName());
        }
    }

    public <E extends Enum<?>> E getEnumOrNull(Class<E> enumClass) {
        try {
            peekEnum(enumClass);
            return getEnum(enumClass);
        } catch (CommandInvalidTypeException e) {
            return null;
        }
    }

    public <T> T getAs(Class<T> type) {
        return get().getAs(type);
    }

    public <T> T getAsOrDefault(Class<T> type, T def) {
        try {
            T val = peek().getAs(type);
            get();
            return val;
        } catch (CommandInvalidTypeException e) {
            return def;
        }
    }

    public <T> T getAsOrNull(Class<T> type) {
        return getAsOrDefault(type, null);
    }

    public <T extends IDatatype> T getDatatype(Class<T> datatype) {
        try {
            return datatype.getConstructor(ArgConsumer.class).newInstance(this);
        } catch (InvocationTargetException e) {
            throw new CommandInvalidTypeException(has() ? peek() : consumed(), datatype.getSimpleName());
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException e) {
            throw new CommandUnhandledException(e);
        }
    }

    public <T extends IDatatype> T getDatatypeOrNull(Class<T> datatype) {
        try {
            return getDatatype(datatype);
        } catch (CommandInvalidTypeException e) {
            return null;
        }
    }

    public <T, O, D extends IDatatypePost<T, O>> T getDatatypePost(Class<D> datatype, O original) {
        return getDatatype(datatype).apply(original);
    }

    public <T, O, D extends IDatatypePost<T, O>> T getDatatypePostOrDefault(Class<D> datatype, O original, T def) {
        try {
            return getDatatypePost(datatype, original);
        } catch (CommandException e) {
            return def;
        }
    }

    public <T, O, D extends IDatatypePost<T, O>> T getDatatypePostOrNull(Class<D> datatype, O original) {
        return getDatatypePostOrDefault(datatype, original, null);
    }

    public <T, D extends IDatatypeFor<T>> T getDatatypeFor(Class<D> datatype) {
        return getDatatype(datatype).get();
    }

    public <T, D extends IDatatypeFor<T>> T getDatatypeForOrDefault(Class<D> datatype, T def) {
        try {
            return getDatatypeFor(datatype);
        } catch (CommandInvalidTypeException e) {
            return def;
        }
    }

    public <T, D extends IDatatypeFor<T>> T getDatatypeForOrNull(Class<D> datatype) {
        return getDatatypeForOrDefault(datatype, null);
    }

    public <T extends IDatatype> Stream<String> tabCompleteDatatype(Class<T> datatype) {
        try {
            return datatype.getConstructor().newInstance().tabComplete(this);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        } catch (CommandException ignored) {}

        return Stream.empty();
    }

    public String rawRest() {
        return args.size() > 0 ? args.get(0).rawRest : "";
    }

    public void requireMin(int min) {
        if (args.size() < min) {
            throw new CommandNotEnoughArgumentsException(min + consumed.size());
        }
    }

    public void requireMax(int max) {
        if (args.size() > max) {
            throw new CommandTooManyArgumentsException(max + consumed.size());
        }
    }

    public void requireExactly(int args) {
        requireMin(args);
        requireMax(args);
    }

    public boolean hasConsumed() {
        return !consumed.isEmpty();
    }

    public CommandArgument consumed() {
        return consumed.size() > 0 ? consumed.getLast() : CommandArgument.unknown();
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public ArgConsumer clone() {
        return new ArgConsumer(args, consumed);
    }

    public static ArgConsumer from(String string) {
        return new ArgConsumer(CommandArgument.from(string));
    }
}
