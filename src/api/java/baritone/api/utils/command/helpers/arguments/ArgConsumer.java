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

import baritone.api.IBaritone;
import baritone.api.utils.Helper;
import baritone.api.utils.command.Command;
import baritone.api.utils.command.argparser.IArgParser;
import baritone.api.utils.command.argument.CommandArgument;
import baritone.api.utils.command.datatypes.IDatatype;
import baritone.api.utils.command.datatypes.IDatatypeContext;
import baritone.api.utils.command.datatypes.IDatatypeFor;
import baritone.api.utils.command.datatypes.IDatatypePost;
import baritone.api.utils.command.exception.CommandException;
import baritone.api.utils.command.exception.CommandInvalidTypeException;
import baritone.api.utils.command.exception.CommandNotEnoughArgumentsException;
import baritone.api.utils.command.exception.CommandTooManyArgumentsException;
import baritone.api.utils.command.manager.ICommandManager;
import net.minecraft.util.EnumFacing;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

/**
 * The {@link ArgConsumer} is how {@link Command}s read the arguments passed to them. This class has many benefits:
 *
 * <ul>
 * <li>Mutability. The whole concept of the {@link ArgConsumer} is to let you gradually consume arguments in any way
 * you'd like. You can change your consumption based on earlier arguments, for subcommands for example.</li>
 * <li>You don't need to keep track of your consumption. The {@link ArgConsumer} keeps track of the arguments you
 * consume so that it can throw detailed exceptions whenever something is out of the ordinary. Additionally, if you
 * need to retrieve an argument after you've already consumed it - look no further than {@link #consumed()}!</li>
 * <li>Easy retrieval of many different types. If you need to retrieve an instance of an int or float for example,
 * look no further than {@link #getAs(Class)}. If you need a more powerful way of retrieving data, try out the many
 * {@code getDatatype...} methods.</li>
 * <li>It's very easy to throw detailed exceptions. The {@link ArgConsumer} has many different methods that can
 * enforce the number of arguments, the type of arguments, and more, throwing different types of
 * {@link CommandException}s if something seems off. You're recommended to do all validation and store all needed
 * data in variables BEFORE logging any data to chat via {@link Helper#logDirect(String)}, so that the error
 * handlers can do their job and log the error to chat.</li>
 * </ul>
 */
public class ArgConsumer {

    /**
     * The parent {@link ICommandManager} for this {@link ArgConsumer}. Used by {@link #context}.
     */
    private final ICommandManager manager;

    /**
     * The {@link IDatatypeContext} instance for this {@link ArgConsumer}, passed to
     * datatypes when an operation is performed upon them.
     *
     * @see IDatatype
     * @see IDatatypeContext
     */
    private final IDatatypeContext context;

    /**
     * The list of arguments in this ArgConsumer
     */
    public final LinkedList<CommandArgument> args;

    /**
     * The list of consumed arguments for this ArgConsumer. The most recently consumed argument is the last one
     */
    public final Deque<CommandArgument> consumed;

    private ArgConsumer(ICommandManager manager, Deque<CommandArgument> args, Deque<CommandArgument> consumed) {
        this.manager = manager;
        this.context = this.new Context();
        this.args = new LinkedList<>(args);
        this.consumed = new LinkedList<>(consumed);
    }

    public ArgConsumer(ICommandManager manager, List<CommandArgument> args) {
        this(manager, new LinkedList<>(args), new LinkedList<>());
    }

    /**
     * @param num The number of arguments to check for
     * @return {@code true} if there are <i>at least</i> {@code num} arguments left in this {@link ArgConsumer}
     * @see #hasAny()
     * @see #hasAtMost(int)
     * @see #hasExactly(int)
     */
    public boolean has(int num) {
        return args.size() >= num;
    }

    /**
     * @return {@code true} if there is <i>at least</i> 1 argument left in this {@link ArgConsumer}
     * @see #has(int)
     * @see #hasAtMostOne()
     * @see #hasExactlyOne()
     */
    public boolean hasAny() {
        return has(1);
    }

    /**
     * @param num The number of arguments to check for
     * @return {@code true} if there are <i>at most</i> {@code num} arguments left in this {@link ArgConsumer}
     * @see #has(int)
     * @see #hasAtMost(int)
     * @see #hasExactly(int)
     */
    public boolean hasAtMost(int num) {
        return args.size() <= num;
    }

    /**
     * @return {@code true} if there is <i>at most</i> 1 argument left in this {@link ArgConsumer}
     * @see #hasAny()
     * @see #hasAtMostOne()
     * @see #hasExactlyOne()
     */
    public boolean hasAtMostOne() {
        return hasAtMost(1);
    }

    /**
     * @param num The number of arguments to check for
     * @return {@code true} if there are <i>exactly</i> {@code num} arguments left in this {@link ArgConsumer}
     * @see #has(int)
     * @see #hasAtMost(int)
     */
    public boolean hasExactly(int num) {
        return args.size() == num;
    }

    /**
     * @return {@code true} if there is <i>exactly</i> 1 argument left in this {@link ArgConsumer}
     * @see #hasAny()
     * @see #hasAtMostOne()
     */
    public boolean hasExactlyOne() {
        return hasExactly(1);
    }

    /**
     * @param index The index to peek
     * @return The argument at index {@code index} in this {@link ArgConsumer}, with 0 being the next one. This does not
     * mutate the {@link ArgConsumer}
     * @throws CommandNotEnoughArgumentsException If there is less than {@code index + 1} arguments left
     * @see #peek()
     * @see #peekString(int)
     * @see #peekAs(Class, int)
     * @see #get()
     */
    public CommandArgument peek(int index) throws CommandNotEnoughArgumentsException {
        requireMin(index + 1);
        return args.get(index);
    }

    /**
     * @return The next argument in this {@link ArgConsumer}. This does not mutate the {@link ArgConsumer}
     * @throws CommandNotEnoughArgumentsException If there is less than one argument left
     * @see #peek(int)
     * @see #peekString()
     * @see #peekAs(Class)
     * @see #get()
     */
    public CommandArgument peek() throws CommandNotEnoughArgumentsException {
        return peek(0);
    }

    /**
     * @param index The index to peek
     * @param type  The type to check for
     * @return If an ArgParser.Stateless for the specified {@code type} would succeed in parsing the next
     * argument
     * @throws CommandNotEnoughArgumentsException If there is less than {@code index + 1} arguments left
     * @see #peek()
     * @see #getAs(Class)
     */
    public boolean is(Class<?> type, int index) throws CommandNotEnoughArgumentsException {
        return peek(index).is(type);
    }

    /**
     * @param type The type to check for
     * @return If an ArgParser.Stateless for the specified {@code type} would succeed in parsing the next
     * argument
     * @throws CommandNotEnoughArgumentsException If there is less than one argument left
     * @see #peek()
     * @see #getAs(Class)
     */
    public boolean is(Class<?> type) throws CommandNotEnoughArgumentsException {
        return is(type, 0);
    }

    /**
     * @param index The index to peek
     * @return The value of the argument at index {@code index} in this {@link ArgConsumer}, with 0 being the next one
     * This does not mutate the {@link ArgConsumer}
     * @throws CommandNotEnoughArgumentsException If there is less than {@code index + 1} arguments left
     * @see #peek()
     * @see #peekString()
     */
    public String peekString(int index) throws CommandNotEnoughArgumentsException {
        return peek(index).value;
    }

    /**
     * @return The value of the next argument in this {@link ArgConsumer}. This does not mutate the {@link ArgConsumer}
     * @throws CommandNotEnoughArgumentsException If there is less than one argument left
     * @see #peekString(int)
     * @see #getString()
     */
    public String peekString() throws CommandNotEnoughArgumentsException {
        return peekString(0);
    }

    /**
     * @param index     The index to peek
     * @param enumClass The class to search
     * @return From the specified enum class, an enum constant of that class. The enum constant's name will match the
     * next argument's value
     * @throws java.util.NoSuchElementException If the constant couldn't be found
     * @see #peekEnumOrNull(Class)
     * @see #getEnum(Class)
     * @see CommandArgument#getEnum(Class)
     */
    public <E extends Enum<?>> E peekEnum(Class<E> enumClass, int index) throws CommandInvalidTypeException, CommandNotEnoughArgumentsException {
        return peek(index).getEnum(enumClass);
    }

    /**
     * @param enumClass The class to search
     * @return From the specified enum class, an enum constant of that class. The enum constant's name will match the
     * next argument's value
     * @throws CommandInvalidTypeException If the constant couldn't be found
     * @see #peekEnumOrNull(Class)
     * @see #getEnum(Class)
     * @see CommandArgument#getEnum(Class)
     */
    public <E extends Enum<?>> E peekEnum(Class<E> enumClass) throws CommandInvalidTypeException, CommandNotEnoughArgumentsException {
        return peekEnum(enumClass, 0);
    }

    /**
     * @param index     The index to peek
     * @param enumClass The class to search
     * @return From the specified enum class, an enum constant of that class. The enum constant's name will match the
     * next argument's value. If no constant could be found, null
     * @see #peekEnum(Class)
     * @see #getEnumOrNull(Class)
     * @see CommandArgument#getEnum(Class)
     */
    public <E extends Enum<?>> E peekEnumOrNull(Class<E> enumClass, int index) throws CommandNotEnoughArgumentsException {
        try {
            return peekEnum(enumClass, index);
        } catch (CommandInvalidTypeException e) {
            return null;
        }
    }

    /**
     * @param enumClass The class to search
     * @return From the specified enum class, an enum constant of that class. The enum constant's name will match the
     * next argument's value. If no constant could be found, null
     * @see #peekEnum(Class)
     * @see #getEnumOrNull(Class)
     * @see CommandArgument#getEnum(Class)
     */
    public <E extends Enum<?>> E peekEnumOrNull(Class<E> enumClass) throws CommandNotEnoughArgumentsException {
        return peekEnumOrNull(enumClass, 0);
    }

    /**
     * Tries to use a <b>stateless</b> {@link IArgParser} to parse the argument at the specified index into the specified
     * class
     * <p>
     * A critical difference between {@link IDatatype}s and {@link IArgParser}s is how many arguments they can take.
     * While {@link IArgParser}s always operate on a single argument's value, {@link IDatatype}s get access to the entire
     * {@link ArgConsumer}.
     *
     * @param type  The type to peek as
     * @param index The index to peek
     * @return An instance of the specified type
     * @throws CommandInvalidTypeException     If the parsing failed
     * @see IArgParser
     * @see #peekAs(Class)
     * @see #peekAsOrDefault(Class, Object, int)
     * @see #peekAsOrNull(Class, int)
     */
    public <T> T peekAs(Class<T> type, int index) throws CommandInvalidTypeException, CommandNotEnoughArgumentsException {
        return peek(index).getAs(type);
    }

    /**
     * Tries to use a <b>stateless</b> {@link IArgParser} to parse the next argument into the specified class
     * <p>
     * A critical difference between {@link IDatatype}s and {@link IArgParser}s is how many arguments they can take.
     * While {@link IArgParser}s always operate on a single argument's value, {@link IDatatype}s get access to the entire
     * {@link ArgConsumer}.
     *
     * @param type The type to peek as
     * @return An instance of the specified type
     * @throws CommandInvalidTypeException     If the parsing failed
     * @see IArgParser
     * @see #peekAs(Class, int)
     * @see #peekAsOrDefault(Class, Object)
     * @see #peekAsOrNull(Class)
     */
    public <T> T peekAs(Class<T> type) throws CommandInvalidTypeException, CommandNotEnoughArgumentsException {
        return peekAs(type, 0);
    }

    /**
     * Tries to use a <b>stateless</b> {@link IArgParser} to parse the argument at the specified index into the specified
     * class
     * <p>
     * A critical difference between {@link IDatatype}s and {@link IArgParser}s is how many arguments they can take.
     * While {@link IArgParser}s always operate on a single argument's value, {@link IDatatype}s get access to the entire
     * {@link ArgConsumer}.
     *
     * @param type  The type to peek as
     * @param def   The value to return if the argument can't be parsed
     * @param index The index to peek
     * @return An instance of the specified type, or {@code def} if it couldn't be parsed
     * @see IArgParser
     * @see #peekAsOrDefault(Class, Object)
     * @see #peekAs(Class, int)
     * @see #peekAsOrNull(Class, int)
     */
    public <T> T peekAsOrDefault(Class<T> type, T def, int index) throws CommandNotEnoughArgumentsException {
        try {
            return peekAs(type, index);
        } catch (CommandInvalidTypeException e) {
            return def;
        }
    }

    /**
     * Tries to use a <b>stateless</b> {@link IArgParser} to parse the next argument into the specified class
     * <p>
     * A critical difference between {@link IDatatype}s and {@link IArgParser}s is how many arguments they can take.
     * While {@link IArgParser}s always operate on a single argument's value, {@link IDatatype}s get access to the entire
     * {@link ArgConsumer}.
     *
     * @param type The type to peek as
     * @param def  The value to return if the argument can't be parsed
     * @return An instance of the specified type, or {@code def} if it couldn't be parsed
     * @see IArgParser
     * @see #peekAsOrDefault(Class, Object, int)
     * @see #peekAs(Class)
     * @see #peekAsOrNull(Class)
     */
    public <T> T peekAsOrDefault(Class<T> type, T def) throws CommandNotEnoughArgumentsException {
        return peekAsOrDefault(type, def, 0);
    }

    /**
     * Tries to use a <b>stateless</b> {@link IArgParser} to parse the argument at the specified index into the specified
     * class
     * <p>
     * A critical difference between {@link IDatatype}s and {@link IArgParser}s is how many arguments they can take.
     * While {@link IArgParser}s always operate on a single argument's value, {@link IDatatype}s get access to the entire
     * {@link ArgConsumer}.
     *
     * @param type  The type to peek as
     * @param index The index to peek
     * @return An instance of the specified type, or {@code null} if it couldn't be parsed
     * @see IArgParser
     * @see #peekAsOrNull(Class)
     * @see #peekAs(Class, int)
     * @see #peekAsOrDefault(Class, Object, int)
     */
    public <T> T peekAsOrNull(Class<T> type, int index) throws CommandNotEnoughArgumentsException {
        return peekAsOrDefault(type, null, index);
    }

    /**
     * Tries to use a <b>stateless</b> {@link IArgParser} to parse the next argument into the specified class
     * <p>
     * A critical difference between {@link IDatatype}s and {@link IArgParser}s is how many arguments they can take.
     * While {@link IArgParser}s always operate on a single argument's value, {@link IDatatype}s get access to the entire
     * {@link ArgConsumer}.
     *
     * @param type The type to peek as
     * @return An instance of the specified type, or {@code null} if it couldn't be parsed
     * @see IArgParser
     * @see #peekAsOrNull(Class, int)
     * @see #peekAs(Class)
     * @see #peekAsOrDefault(Class, Object)
     */
    public <T> T peekAsOrNull(Class<T> type) throws CommandNotEnoughArgumentsException {
        return peekAsOrNull(type, 0);
    }

    public <T> T peekDatatype(IDatatypeFor<T> datatype) throws CommandInvalidTypeException, CommandNotEnoughArgumentsException {
        return copy().getDatatypeFor(datatype);
    }

    public <T, O> T peekDatatype(IDatatypePost<T, O> datatype) throws CommandInvalidTypeException, CommandNotEnoughArgumentsException {
        return this.peekDatatype(datatype, null);
    }

    public <T, O> T peekDatatype(IDatatypePost<T, O> datatype, O original) throws CommandInvalidTypeException, CommandNotEnoughArgumentsException {
        return copy().getDatatypePost(datatype, original);
    }

    public <T> T peekDatatypeOrNull(IDatatypeFor<T> datatype) {
        return copy().getDatatypeForOrNull(datatype);
    }

    public <T, O> T peekDatatypeOrNull(IDatatypePost<T, O> datatype) {
        return copy().getDatatypePostOrNull(datatype, null);
    }

    public <T, O, D extends IDatatypePost<T, O>> T peekDatatypePost(D datatype, O original) throws CommandInvalidTypeException, CommandNotEnoughArgumentsException {
        return copy().getDatatypePost(datatype, original);
    }

    public <T, O, D extends IDatatypePost<T, O>> T peekDatatypePostOrDefault(D datatype, O original, T def) {
        return copy().getDatatypePostOrDefault(datatype, original, def);
    }

    public <T, O, D extends IDatatypePost<T, O>> T peekDatatypePostOrNull(D datatype, O original) {
        return peekDatatypePostOrDefault(datatype, original, null);
    }

    /**
     * Attempts to get the specified {@link IDatatypeFor} from this ArgConsumer
     * <p>
     * A critical difference between {@link IDatatype}s and {@link IArgParser}s is how many arguments they can take.
     * While {@link IArgParser}s always operate on a single argument's value, {@link IDatatype}s get access to the entire
     * {@link ArgConsumer}.
     * <p>
     * Since this is a peek operation, this ArgConsumer will not be mutated by any call to this method.
     *
     * @param datatype The datatype to get
     * @return The datatype instance
     * @see IDatatype
     * @see IDatatypeFor
     */
    public <T, D extends IDatatypeFor<T>> T peekDatatypeFor(Class<D> datatype) {
        return copy().peekDatatypeFor(datatype);
    }

    /**
     * Attempts to get the specified {@link IDatatypeFor} from this ArgConsumer
     * <p>
     * A critical difference between {@link IDatatype}s and {@link IArgParser}s is how many arguments they can take.
     * While {@link IArgParser}s always operate on a single argument's value, {@link IDatatype}s get access to the entire
     * {@link ArgConsumer}.
     * <p>
     * Since this is a peek operation, this ArgConsumer will not be mutated by any call to this method.
     *
     * @param datatype The datatype to get
     * @param def      The default value
     * @return The datatype instance, or {@code def} if it throws an exception
     * @see IDatatype
     * @see IDatatypeFor
     */
    public <T, D extends IDatatypeFor<T>> T peekDatatypeForOrDefault(Class<D> datatype, T def) {
        return copy().peekDatatypeForOrDefault(datatype, def);
    }

    /**
     * Attempts to get the specified {@link IDatatypeFor} from this ArgConsumer
     * <p>
     * A critical difference between {@link IDatatype}s and {@link IArgParser}s is how many arguments they can take.
     * While {@link IArgParser}s always operate on a single argument's value, {@link IDatatype}s get access to the entire
     * {@link ArgConsumer}.
     * <p>
     * Since this is a peek operation, this ArgConsumer will not be mutated by any call to this method.
     *
     * @param datatype The datatype to get
     * @return The datatype instance, or {@code null} if it throws an exception
     * @see IDatatype
     * @see IDatatypeFor
     */
    public <T, D extends IDatatypeFor<T>> T peekDatatypeForOrNull(Class<D> datatype) {
        return peekDatatypeForOrDefault(datatype, null);
    }

    /**
     * Gets the next argument and returns it. This consumes the first argument so that subsequent calls will return
     * later arguments
     *
     * @return The next argument
     * @throws CommandNotEnoughArgumentsException If there's less than one argument left
     */
    public CommandArgument get() throws CommandNotEnoughArgumentsException {
        requireMin(1);
        CommandArgument arg = args.removeFirst();
        consumed.add(arg);
        return arg;
    }

    /**
     * Gets the value of the next argument and returns it. This consumes the first argument so that subsequent calls
     * will return later arguments
     *
     * @return The value of the next argument
     * @throws CommandNotEnoughArgumentsException If there's less than one argument left
     */
    public String getString() throws CommandNotEnoughArgumentsException {
        return get().value;
    }

    /**
     * Gets an enum value from the enum class with the same name as the next argument's value
     * <p>
     * For example if you getEnum as an {@link EnumFacing}, and the next argument's value is "up", this will return
     * {@link EnumFacing#UP}
     *
     * @param enumClass The enum class to search
     * @return An enum constant of that class with the same name as the next argument's value
     * @throws CommandInvalidTypeException If the constant couldn't be found
     * @see #peekEnum(Class)
     * @see #getEnumOrNull(Class)
     * @see CommandArgument#getEnum(Class)
     */
    public <E extends Enum<?>> E getEnum(Class<E> enumClass) throws CommandInvalidTypeException, CommandNotEnoughArgumentsException {
        return get().getEnum(enumClass);
    }

    /**
     * Gets an enum value from the enum class with the same name as the next argument's value
     * <p>
     * For example if you getEnum as an {@link EnumFacing}, and the next argument's value is "up", this will return
     * {@link EnumFacing#UP}
     *
     * @param enumClass The enum class to search
     * @param def       The default value
     * @return An enum constant of that class with the same name as the next argument's value, or {@code def} if it
     * couldn't be found
     * @see #getEnum(Class)
     * @see #getEnumOrNull(Class)
     * @see #peekEnumOrNull(Class)
     * @see CommandArgument#getEnum(Class)
     */
    public <E extends Enum<?>> E getEnumOrDefault(Class<E> enumClass, E def) throws CommandNotEnoughArgumentsException {
        try {
            peekEnum(enumClass);
            return getEnum(enumClass);
        } catch (CommandInvalidTypeException e) {
            return def;
        }
    }

    /**
     * Gets an enum value from the enum class with the same name as the next argument's value
     * <p>
     * For example if you getEnum as an {@link EnumFacing}, and the next argument's value is "up", this will return
     * {@link EnumFacing#UP}
     *
     * @param enumClass The enum class to search
     * @return An enum constant of that class with the same name as the next argument's value, or {@code null} if it
     * couldn't be found
     * @see #getEnum(Class)
     * @see #getEnumOrDefault(Class, Enum)
     * @see #peekEnumOrNull(Class)
     * @see CommandArgument#getEnum(Class)
     */
    public <E extends Enum<?>> E getEnumOrNull(Class<E> enumClass) throws CommandNotEnoughArgumentsException {
        return getEnumOrDefault(enumClass, null);
    }

    /**
     * Tries to use a <b>stateless</b> {@link IArgParser} to parse the next argument into the specified class
     * <p>
     * A critical difference between {@link IDatatype}s and {@link IArgParser}s is how many arguments they can take.
     * While {@link IArgParser}s always operate on a single argument's value, {@link IDatatype}s get access to the entire
     * {@link ArgConsumer}.
     *
     * @param type The type to peek as
     * @return An instance of the specified type
     * @throws CommandInvalidTypeException     If the parsing failed
     * @see IArgParser
     * @see #get()
     * @see #getAsOrDefault(Class, Object)
     * @see #getAsOrNull(Class)
     * @see #peekAs(Class)
     * @see #peekAsOrDefault(Class, Object, int)
     * @see #peekAsOrNull(Class, int)
     */
    public <T> T getAs(Class<T> type) throws CommandInvalidTypeException, CommandNotEnoughArgumentsException {
        return get().getAs(type);
    }

    /**
     * Tries to use a <b>stateless</b> {@link IArgParser} to parse the next argument into the specified class
     * <p>
     * A critical difference between {@link IDatatype}s and {@link IArgParser}s is how many arguments they can take.
     * While {@link IArgParser}s always operate on a single argument's value, {@link IDatatype}s get access to the entire
     * {@link ArgConsumer}.
     *
     * @param type The type to peek as
     * @param def  The default value
     * @return An instance of the specified type, or {@code def} if it couldn't be parsed
     * @see IArgParser
     * @see #get()
     * @see #getAs(Class)
     * @see #getAsOrNull(Class)
     * @see #peekAs(Class)
     * @see #peekAsOrDefault(Class, Object, int)
     * @see #peekAsOrNull(Class, int)
     */
    public <T> T getAsOrDefault(Class<T> type, T def) throws CommandNotEnoughArgumentsException {
        try {
            T val = peek().getAs(type);
            get();
            return val;
        } catch (CommandInvalidTypeException e) {
            return def;
        }
    }

    /**
     * Tries to use a <b>stateless</b> {@link IArgParser} to parse the next argument into the specified class
     * <p>
     * A critical difference between {@link IDatatype}s and {@link IArgParser}s is how many arguments they can take.
     * While {@link IArgParser}s always operate on a single argument's value, {@link IDatatype}s get access to the entire
     * {@link ArgConsumer}.
     *
     * @param type The type to peek as
     * @return An instance of the specified type, or {@code null} if it couldn't be parsed
     * @see IArgParser
     * @see #get()
     * @see #getAs(Class)
     * @see #getAsOrDefault(Class, Object)
     * @see #peekAs(Class)
     * @see #peekAsOrDefault(Class, Object, int)
     * @see #peekAsOrNull(Class, int)
     */
    public <T> T getAsOrNull(Class<T> type) throws CommandNotEnoughArgumentsException {
        return getAsOrDefault(type, null);
    }

    public <T, O, D extends IDatatypePost<T, O>> T getDatatypePost(D datatype, O original) throws CommandInvalidTypeException, CommandNotEnoughArgumentsException {
        try {
            return datatype.apply(this.context, original);
        } catch (Exception e) {
            e.printStackTrace();
            throw new CommandInvalidTypeException(hasAny() ? peek() : consumed(), datatype.getClass().getSimpleName());
        }
    }

    public <T, O, D extends IDatatypePost<T, O>> T getDatatypePostOrDefault(D datatype, O original, T _default) {
        final List<CommandArgument> argsSnapshot = new ArrayList<>(this.args);
        final List<CommandArgument> consumedSnapshot = new ArrayList<>(this.consumed);
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

    public <T, O, D extends IDatatypePost<T, O>> T getDatatypePostOrNull(D datatype, O original) {
        return this.getDatatypePostOrDefault(datatype, original, null);
    }

    public <T, D extends IDatatypeFor<T>> T getDatatypeFor(D datatype) throws CommandInvalidTypeException, CommandNotEnoughArgumentsException {
        try {
            return datatype.get(this.context);
        } catch (Exception e) {
            throw new CommandInvalidTypeException(hasAny() ? peek() : consumed(), datatype.getClass().getSimpleName());
        }
    }

    public <T, D extends IDatatypeFor<T>> T getDatatypeForOrDefault(D datatype, T def) {
        final List<CommandArgument> argsSnapshot = new ArrayList<>(this.args);
        final List<CommandArgument> consumedSnapshot = new ArrayList<>(this.consumed);
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

    public <T, D extends IDatatypeFor<T>> T getDatatypeForOrNull(D datatype) {
        return this.getDatatypeForOrDefault(datatype, null);
    }

    public <T extends IDatatype> Stream<String> tabCompleteDatatype(T datatype) {
        try {
            return datatype.tabComplete(this.context);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Stream.empty();
    }

    /**
     * Returns the "raw rest" of the string. For example, from a string <code>arg1 arg2&nbsp;&nbsp;arg3</code>, split
     * into three {@link CommandArgument}s {@code "arg1"}, {@code "arg2"}, and {@code "arg3"}:
     *
     * <ul>
     * <li>{@code rawRest()} would return <code>arg1 arg2&nbsp;&nbsp;arg3</code></li>
     * <li>After calling {@link #get()}, {@code rawRest()} would return <code>arg2&nbsp;&nbsp;arg3</code> (note the
     * double space - it is preserved!)</li>
     * <li>After calling {@link #get()} again, {@code rawRest()} would return {@code "arg3"}</li>
     * <li>After calling {@link #get()} one last time, {@code rawRest()} would return {@code ""}</li>
     * </ul>
     *
     * @return The "raw rest" of the string.
     */
    public String rawRest() {
        return args.size() > 0 ? args.getFirst().rawRest : "";
    }

    /**
     * @param min The minimum amount of arguments to require.
     * @throws CommandNotEnoughArgumentsException If there are less than {@code min} arguments left.
     * @see #requireMax(int)
     * @see #requireExactly(int)
     */
    public void requireMin(int min) throws CommandNotEnoughArgumentsException {
        if (args.size() < min) {
            throw new CommandNotEnoughArgumentsException(min + consumed.size());
        }
    }

    /**
     * @param max The maximum amount of arguments allowed.
     * @throws CommandTooManyArgumentsException If there are more than {@code max} arguments left.
     * @see #requireMin(int)
     * @see #requireExactly(int)
     */
    public void requireMax(int max) throws CommandTooManyArgumentsException {
        if (args.size() > max) {
            throw new CommandTooManyArgumentsException(max + consumed.size());
        }
    }

    /**
     * @param args The exact amount of arguments to require.
     * @throws CommandNotEnoughArgumentsException If there are less than {@code args} arguments left.
     * @throws CommandTooManyArgumentsException   If there are more than {@code args} arguments left.
     * @see #requireMin(int)
     * @see #requireMax(int)
     */
    public void requireExactly(int args) throws CommandException {
        requireMin(args);
        requireMax(args);
    }

    /**
     * @return If this {@link ArgConsumer} has consumed at least one argument.
     * @see #consumed()
     * @see #consumedString()
     */
    public boolean hasConsumed() {
        return !consumed.isEmpty();
    }

    /**
     * @return The last argument this {@link ArgConsumer} has consumed, or the {@link CommandArgument#unknown() unknown}
     * argument if no arguments have been consumed yet.
     * @see #consumedString()
     * @see #hasConsumed()
     */
    public CommandArgument consumed() {
        return consumed.size() > 0 ? consumed.getLast() : CommandArgument.unknown();
    }

    /**
     * @return The value of thelast argument this {@link ArgConsumer} has consumed, or an empty string if no arguments
     * have been consumed yet
     * @see #consumed()
     * @see #hasConsumed()
     */
    public String consumedString() {
        return consumed().value;
    }

    /**
     * @return A copy of this {@link ArgConsumer}. It has the same arguments (both consumed and not), but does not
     * affect or mutate this instance. Useful for the various {@code peek} functions
     */
    public ArgConsumer copy() {
        return new ArgConsumer(manager, args, consumed);
    }

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
