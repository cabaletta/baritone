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

package baritone.command.argument;

import baritone.api.IBaritone;
import baritone.api.command.argument.IArgConsumer;
import baritone.api.command.argument.ICommandArgument;
import baritone.api.command.datatypes.IDatatype;
import baritone.api.command.datatypes.IDatatypeContext;
import baritone.api.command.datatypes.IDatatypeFor;
import baritone.api.command.datatypes.IDatatypePost;
import baritone.api.command.exception.CommandException;
import baritone.api.command.exception.CommandInvalidTypeException;
import baritone.api.command.exception.CommandNotEnoughArgumentsException;
import baritone.api.command.exception.CommandTooManyArgumentsException;
import baritone.api.command.manager.ICommandManager;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

public class ArgConsumer implements IArgConsumer {

    /**
     * The parent {@link ICommandManager} for this {@link IArgConsumer}}. Used by {@link #context}.
     */
    private final ICommandManager manager;

    /**
     * The {@link IDatatypeContext} instance for this {@link IArgConsumer}}, passed to
     * datatypes when an operation is performed upon them.
     *
     * @see IDatatype
     * @see IDatatypeContext
     */
    private final IDatatypeContext context;

    /**
     * The list of arguments in this ArgConsumer
     */
    private final LinkedList<ICommandArgument> args;

    /**
     * The list of consumed arguments for this ArgConsumer. The most recently consumed argument is the last one
     */
    private final Deque<ICommandArgument> consumed;

    private ArgConsumer(ICommandManager manager, Deque<ICommandArgument> args, Deque<ICommandArgument> consumed) {
        this.manager = manager;
        this.context = this.new Context();
        this.args = new LinkedList<>(args);
        this.consumed = new LinkedList<>(consumed);
    }

    public ArgConsumer(ICommandManager manager, List<ICommandArgument> args) {
        this(manager, new LinkedList<>(args), new LinkedList<>());
    }

    @Override
    public LinkedList<ICommandArgument> getArgs() {
        return this.args;
    }

    @Override
    public Deque<ICommandArgument> getConsumed() {
        return this.consumed;
    }

    @Override
    public boolean has(int num) {
        return args.size() >= num;
    }

    @Override
    public boolean hasAny() {
        return has(1);
    }

    @Override
    public boolean hasAtMost(int num) {
        return args.size() <= num;
    }

    @Override
    public boolean hasAtMostOne() {
        return hasAtMost(1);
    }

    @Override
    public boolean hasExactly(int num) {
        return args.size() == num;
    }

    @Override
    public boolean hasExactlyOne() {
        return hasExactly(1);
    }

    @Override
    public ICommandArgument peek(int index) throws CommandNotEnoughArgumentsException {
        requireMin(index + 1);
        return args.get(index);
    }

    @Override
    public ICommandArgument peek() throws CommandNotEnoughArgumentsException {
        return peek(0);
    }

    @Override
    public boolean is(Class<?> type, int index) throws CommandNotEnoughArgumentsException {
        return peek(index).is(type);
    }

    @Override
    public boolean is(Class<?> type) throws CommandNotEnoughArgumentsException {
        return is(type, 0);
    }

    @Override
    public String peekString(int index) throws CommandNotEnoughArgumentsException {
        return peek(index).getValue();
    }

    @Override
    public String peekString() throws CommandNotEnoughArgumentsException {
        return peekString(0);
    }

    @Override
    public <E extends Enum<?>> E peekEnum(Class<E> enumClass, int index) throws CommandInvalidTypeException, CommandNotEnoughArgumentsException {
        return peek(index).getEnum(enumClass);
    }

    @Override
    public <E extends Enum<?>> E peekEnum(Class<E> enumClass) throws CommandInvalidTypeException, CommandNotEnoughArgumentsException {
        return peekEnum(enumClass, 0);
    }

    @Override
    public <E extends Enum<?>> E peekEnumOrNull(Class<E> enumClass, int index) throws CommandNotEnoughArgumentsException {
        try {
            return peekEnum(enumClass, index);
        } catch (CommandInvalidTypeException e) {
            return null;
        }
    }

    @Override
    public <E extends Enum<?>> E peekEnumOrNull(Class<E> enumClass) throws CommandNotEnoughArgumentsException {
        return peekEnumOrNull(enumClass, 0);
    }

    @Override
    public <T> T peekAs(Class<T> type, int index) throws CommandInvalidTypeException, CommandNotEnoughArgumentsException {
        return peek(index).getAs(type);
    }

    @Override
    public <T> T peekAs(Class<T> type) throws CommandInvalidTypeException, CommandNotEnoughArgumentsException {
        return peekAs(type, 0);
    }

    @Override
    public <T> T peekAsOrDefault(Class<T> type, T def, int index) throws CommandNotEnoughArgumentsException {
        try {
            return peekAs(type, index);
        } catch (CommandInvalidTypeException e) {
            return def;
        }
    }

    @Override
    public <T> T peekAsOrDefault(Class<T> type, T def) throws CommandNotEnoughArgumentsException {
        return peekAsOrDefault(type, def, 0);
    }

    @Override
    public <T> T peekAsOrNull(Class<T> type, int index) throws CommandNotEnoughArgumentsException {
        return peekAsOrDefault(type, null, index);
    }

    @Override
    public <T> T peekAsOrNull(Class<T> type) throws CommandNotEnoughArgumentsException {
        return peekAsOrNull(type, 0);
    }

    @Override
    public <T> T peekDatatype(IDatatypeFor<T> datatype) throws CommandInvalidTypeException, CommandNotEnoughArgumentsException {
        return copy().getDatatypeFor(datatype);
    }

    @Override
    public <T, O> T peekDatatype(IDatatypePost<T, O> datatype) throws CommandInvalidTypeException, CommandNotEnoughArgumentsException {
        return this.peekDatatype(datatype, null);
    }

    @Override
    public <T, O> T peekDatatype(IDatatypePost<T, O> datatype, O original) throws CommandInvalidTypeException, CommandNotEnoughArgumentsException {
        return copy().getDatatypePost(datatype, original);
    }

    @Override
    public <T> T peekDatatypeOrNull(IDatatypeFor<T> datatype) {
        return copy().getDatatypeForOrNull(datatype);
    }

    @Override
    public <T, O> T peekDatatypeOrNull(IDatatypePost<T, O> datatype) {
        return copy().getDatatypePostOrNull(datatype, null);
    }

    @Override
    public <T, O, D extends IDatatypePost<T, O>> T peekDatatypePost(D datatype, O original) throws CommandInvalidTypeException, CommandNotEnoughArgumentsException {
        return copy().getDatatypePost(datatype, original);
    }

    @Override
    public <T, O, D extends IDatatypePost<T, O>> T peekDatatypePostOrDefault(D datatype, O original, T def) {
        return copy().getDatatypePostOrDefault(datatype, original, def);
    }

    @Override
    public <T, O, D extends IDatatypePost<T, O>> T peekDatatypePostOrNull(D datatype, O original) {
        return peekDatatypePostOrDefault(datatype, original, null);
    }

    @Override
    public <T, D extends IDatatypeFor<T>> T peekDatatypeFor(Class<D> datatype) {
        return copy().peekDatatypeFor(datatype);
    }

    @Override
    public <T, D extends IDatatypeFor<T>> T peekDatatypeForOrDefault(Class<D> datatype, T def) {
        return copy().peekDatatypeForOrDefault(datatype, def);
    }

    @Override
    public <T, D extends IDatatypeFor<T>> T peekDatatypeForOrNull(Class<D> datatype) {
        return peekDatatypeForOrDefault(datatype, null);
    }

    @Override
    public ICommandArgument get() throws CommandNotEnoughArgumentsException {
        requireMin(1);
        ICommandArgument arg = args.removeFirst();
        consumed.add(arg);
        return arg;
    }

    @Override
    public String getString() throws CommandNotEnoughArgumentsException {
        return get().getValue();
    }

    @Override
    public <E extends Enum<?>> E getEnum(Class<E> enumClass) throws CommandInvalidTypeException, CommandNotEnoughArgumentsException {
        return get().getEnum(enumClass);
    }

    @Override
    public <E extends Enum<?>> E getEnumOrDefault(Class<E> enumClass, E def) throws CommandNotEnoughArgumentsException {
        try {
            peekEnum(enumClass);
            return getEnum(enumClass);
        } catch (CommandInvalidTypeException e) {
            return def;
        }
    }

    @Override
    public <E extends Enum<?>> E getEnumOrNull(Class<E> enumClass) throws CommandNotEnoughArgumentsException {
        return getEnumOrDefault(enumClass, null);
    }

    @Override
    public <T> T getAs(Class<T> type) throws CommandInvalidTypeException, CommandNotEnoughArgumentsException {
        return get().getAs(type);
    }

    @Override
    public <T> T getAsOrDefault(Class<T> type, T def) throws CommandNotEnoughArgumentsException {
        try {
            T val = peek().getAs(type);
            get();
            return val;
        } catch (CommandInvalidTypeException e) {
            return def;
        }
    }

    @Override
    public <T> T getAsOrNull(Class<T> type) throws CommandNotEnoughArgumentsException {
        return getAsOrDefault(type, null);
    }

    @Override
    public <T, O, D extends IDatatypePost<T, O>> T getDatatypePost(D datatype, O original) throws CommandInvalidTypeException, CommandNotEnoughArgumentsException {
        try {
            return datatype.apply(this.context, original);
        } catch (Exception e) {
            throw new CommandInvalidTypeException(hasAny() ? peek() : consumed(), datatype.getClass().getSimpleName(), e);
        }
    }

    @Override
    public <T, O, D extends IDatatypePost<T, O>> T getDatatypePostOrDefault(D datatype, O original, T _default) {
        final List<ICommandArgument> argsSnapshot = new ArrayList<>(this.args);
        final List<ICommandArgument> consumedSnapshot = new ArrayList<>(this.consumed);
        try {
            return this.getDatatypePost(datatype, original);
        } catch (Exception e) {
            this.args.clear();
            this.args.addAll(argsSnapshot);
            this.consumed.clear();
            this.consumed.addAll(consumedSnapshot);
            return _default;
        }
    }

    @Override
    public <T, O, D extends IDatatypePost<T, O>> T getDatatypePostOrNull(D datatype, O original) {
        return this.getDatatypePostOrDefault(datatype, original, null);
    }

    @Override
    public <T, D extends IDatatypeFor<T>> T getDatatypeFor(D datatype) throws CommandInvalidTypeException, CommandNotEnoughArgumentsException {
        try {
            return datatype.get(this.context);
        } catch (Exception e) {
            throw new CommandInvalidTypeException(hasAny() ? peek() : consumed(), datatype.getClass().getSimpleName(), e);
        }
    }

    @Override
    public <T, D extends IDatatypeFor<T>> T getDatatypeForOrDefault(D datatype, T def) {
        final List<ICommandArgument> argsSnapshot = new ArrayList<>(this.args);
        final List<ICommandArgument> consumedSnapshot = new ArrayList<>(this.consumed);
        try {
            return this.getDatatypeFor(datatype);
        } catch (Exception e) {
            this.args.clear();
            this.args.addAll(argsSnapshot);
            this.consumed.clear();
            this.consumed.addAll(consumedSnapshot);
            return def;
        }
    }

    @Override
    public <T, D extends IDatatypeFor<T>> T getDatatypeForOrNull(D datatype) {
        return this.getDatatypeForOrDefault(datatype, null);
    }

    @Override
    public <T extends IDatatype> Stream<String> tabCompleteDatatype(T datatype) {
        try {
            return datatype.tabComplete(this.context);
        } catch (CommandException ignored) {
            // NOP
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Stream.empty();
    }

    @Override
    public String rawRest() {
        return args.size() > 0 ? args.getFirst().getRawRest() : "";
    }

    @Override
    public void requireMin(int min) throws CommandNotEnoughArgumentsException {
        if (args.size() < min) {
            throw new CommandNotEnoughArgumentsException(min + consumed.size());
        }
    }

    @Override
    public void requireMax(int max) throws CommandTooManyArgumentsException {
        if (args.size() > max) {
            throw new CommandTooManyArgumentsException(max + consumed.size());
        }
    }

    @Override
    public void requireExactly(int args) throws CommandException {
        requireMin(args);
        requireMax(args);
    }

    @Override
    public boolean hasConsumed() {
        return !consumed.isEmpty();
    }

    @Override
    public ICommandArgument consumed() {
        return consumed.size() > 0 ? consumed.getLast() : CommandArguments.unknown();
    }

    @Override
    public String consumedString() {
        return consumed().getValue();
    }

    @Override
    public ArgConsumer copy() {
        return new ArgConsumer(manager, args, consumed);
    }

    /**
     * Implementation of {@link IDatatypeContext} which adapts to the parent {@link IArgConsumer}}
     */
    private final class Context implements IDatatypeContext {

        @Override
        public final IBaritone getBaritone() {
            return ArgConsumer.this.manager.getBaritone();
        }

        @Override
        public final ArgConsumer getConsumer() {
            return ArgConsumer.this;
        }
    }
}
