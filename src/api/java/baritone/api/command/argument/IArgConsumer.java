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

package baritone.api.command.argument;

import baritone.api.command.ICommand;
import baritone.api.command.argparser.IArgParser;
import baritone.api.command.datatypes.IDatatype;
import baritone.api.command.datatypes.IDatatypeFor;
import baritone.api.command.datatypes.IDatatypePost;
import baritone.api.command.exception.CommandException;
import baritone.api.command.exception.CommandInvalidTypeException;
import baritone.api.command.exception.CommandNotEnoughArgumentsException;
import baritone.api.command.exception.CommandTooManyArgumentsException;
import baritone.api.utils.Helper;
import net.minecraft.util.EnumFacing;

import java.util.Deque;
import java.util.LinkedList;
import java.util.stream.Stream;

/**
 * The {@link IArgConsumer} is how {@link ICommand}s read the arguments passed to them. This class has many benefits:
 *
 * <ul>
 * <li>Mutability. The whole concept of the {@link IArgConsumer}} is to let you gradually consume arguments in any way
 * you'd like. You can change your consumption based on earlier arguments, for subcommands for example.</li>
 * <li>You don't need to keep track of your consumption. The {@link IArgConsumer}} keeps track of the arguments you
 * consume so that it can throw detailed exceptions whenever something is out of the ordinary. Additionally, if you
 * need to retrieve an argument after you've already consumed it - look no further than {@link #consumed()}!</li>
 * <li>Easy retrieval of many different types. If you need to retrieve an instance of an int or float for example,
 * look no further than {@link #getAs(Class)}. If you need a more powerful way of retrieving data, try out the many
 * {@code getDatatype...} methods.</li>
 * <li>It's very easy to throw detailed exceptions. The {@link IArgConsumer}} has many different methods that can
 * enforce the number of arguments, the type of arguments, and more, throwing different types of
 * {@link CommandException}s if something seems off. You're recommended to do all validation and store all needed
 * data in variables BEFORE logging any data to chat via {@link Helper#logDirect(String)}, so that the error
 * handlers can do their job and log the error to chat.</li>
 * </ul>
 */
public interface IArgConsumer {

    LinkedList<ICommandArgument> getArgs();

    Deque<ICommandArgument> getConsumed();

    /**
     * @param num The number of arguments to check for
     * @return {@code true} if there are <i>at least</i> {@code num} arguments left in this {@link IArgConsumer}}
     * @see #hasAny()
     * @see #hasAtMost(int)
     * @see #hasExactly(int)
     */
    boolean has(int num);

    /**
     * @return {@code true} if there is <i>at least</i> 1 argument left in this {@link IArgConsumer}}
     * @see #has(int)
     * @see #hasAtMostOne()
     * @see #hasExactlyOne()
     */
    boolean hasAny();

    /**
     * @param num The number of arguments to check for
     * @return {@code true} if there are <i>at most</i> {@code num} arguments left in this {@link IArgConsumer}}
     * @see #has(int)
     * @see #hasAtMost(int)
     * @see #hasExactly(int)
     */
    boolean hasAtMost(int num);

    /**
     * @return {@code true} if there is <i>at most</i> 1 argument left in this {@link IArgConsumer}}
     * @see #hasAny()
     * @see #hasAtMostOne()
     * @see #hasExactlyOne()
     */
    boolean hasAtMostOne();

    /**
     * @param num The number of arguments to check for
     * @return {@code true} if there are <i>exactly</i> {@code num} arguments left in this {@link IArgConsumer}}
     * @see #has(int)
     * @see #hasAtMost(int)
     */
    boolean hasExactly(int num);

    /**
     * @return {@code true} if there is <i>exactly</i> 1 argument left in this {@link IArgConsumer}}
     * @see #hasAny()
     * @see #hasAtMostOne()
     */
    boolean hasExactlyOne();

    /**
     * @param index The index to peek
     * @return The argument at index {@code index} in this {@link IArgConsumer}}, with 0 being the next one. This does not
     * mutate the {@link IArgConsumer}}
     * @throws CommandNotEnoughArgumentsException If there is less than {@code index + 1} arguments left
     * @see #peek()
     * @see #peekString(int)
     * @see #peekAs(Class, int)
     * @see #get()
     */
    ICommandArgument peek(int index) throws CommandNotEnoughArgumentsException;

    /**
     * @return The next argument in this {@link IArgConsumer}}. This does not mutate the {@link IArgConsumer}}
     * @throws CommandNotEnoughArgumentsException If there is less than one argument left
     * @see #peek(int)
     * @see #peekString()
     * @see #peekAs(Class)
     * @see #get()
     */
    ICommandArgument peek() throws CommandNotEnoughArgumentsException;

    /**
     * @param index The index to peek
     * @param type  The type to check for
     * @return If an ArgParser.Stateless for the specified {@code type} would succeed in parsing the next
     * argument
     * @throws CommandNotEnoughArgumentsException If there is less than {@code index + 1} arguments left
     * @see #peek()
     * @see #getAs(Class)
     */
    boolean is(Class<?> type, int index) throws CommandNotEnoughArgumentsException;

    /**
     * @param type The type to check for
     * @return If an ArgParser.Stateless for the specified {@code type} would succeed in parsing the next
     * argument
     * @throws CommandNotEnoughArgumentsException If there is less than one argument left
     * @see #peek()
     * @see #getAs(Class)
     */
    boolean is(Class<?> type) throws CommandNotEnoughArgumentsException;

    /**
     * @param index The index to peek
     * @return The value of the argument at index {@code index} in this {@link IArgConsumer}}, with 0 being the next one
     * This does not mutate the {@link IArgConsumer}}
     * @throws CommandNotEnoughArgumentsException If there is less than {@code index + 1} arguments left
     * @see #peek()
     * @see #peekString()
     */
    String peekString(int index) throws CommandNotEnoughArgumentsException;

    /**
     * @return The value of the next argument in this {@link IArgConsumer}}. This does not mutate the {@link IArgConsumer}}
     * @throws CommandNotEnoughArgumentsException If there is less than one argument left
     * @see #peekString(int)
     * @see #getString()
     */
    String peekString() throws CommandNotEnoughArgumentsException;

    /**
     * @param index     The index to peek
     * @param enumClass The class to search
     * @return From the specified enum class, an enum constant of that class. The enum constant's name will match the
     * next argument's value
     * @throws java.util.NoSuchElementException If the constant couldn't be found
     * @see #peekEnumOrNull(Class)
     * @see #getEnum(Class)
     * @see ICommandArgument#getEnum(Class)
     */
    <E extends Enum<?>> E peekEnum(Class<E> enumClass, int index) throws CommandInvalidTypeException, CommandNotEnoughArgumentsException;

    /**
     * @param enumClass The class to search
     * @return From the specified enum class, an enum constant of that class. The enum constant's name will match the
     * next argument's value
     * @throws CommandInvalidTypeException If the constant couldn't be found
     * @see #peekEnumOrNull(Class)
     * @see #getEnum(Class)
     * @see ICommandArgument#getEnum(Class)
     */
    <E extends Enum<?>> E peekEnum(Class<E> enumClass) throws CommandInvalidTypeException, CommandNotEnoughArgumentsException;

    /**
     * @param index     The index to peek
     * @param enumClass The class to search
     * @return From the specified enum class, an enum constant of that class. The enum constant's name will match the
     * next argument's value. If no constant could be found, null
     * @see #peekEnum(Class)
     * @see #getEnumOrNull(Class)
     * @see ICommandArgument#getEnum(Class)
     */
    <E extends Enum<?>> E peekEnumOrNull(Class<E> enumClass, int index) throws CommandNotEnoughArgumentsException;

    /**
     * @param enumClass The class to search
     * @return From the specified enum class, an enum constant of that class. The enum constant's name will match the
     * next argument's value. If no constant could be found, null
     * @see #peekEnum(Class)
     * @see #getEnumOrNull(Class)
     * @see ICommandArgument#getEnum(Class)
     */
    <E extends Enum<?>> E peekEnumOrNull(Class<E> enumClass) throws CommandNotEnoughArgumentsException;

    /**
     * Tries to use a <b>stateless</b> {@link IArgParser} to parse the argument at the specified index into the specified
     * class
     * <p>
     * A critical difference between {@link IDatatype}s and {@link IArgParser}s is how many arguments they can take.
     * While {@link IArgParser}s always operate on a single argument's value, {@link IDatatype}s get access to the entire
     * {@link IArgConsumer}}.
     *
     * @param type  The type to peek as
     * @param index The index to peek
     * @return An instance of the specified type
     * @throws CommandInvalidTypeException If the parsing failed
     * @see IArgParser
     * @see #peekAs(Class)
     * @see #peekAsOrDefault(Class, Object, int)
     * @see #peekAsOrNull(Class, int)
     */
    <T> T peekAs(Class<T> type, int index) throws CommandInvalidTypeException, CommandNotEnoughArgumentsException;

    /**
     * Tries to use a <b>stateless</b> {@link IArgParser} to parse the next argument into the specified class
     * <p>
     * A critical difference between {@link IDatatype}s and {@link IArgParser}s is how many arguments they can take.
     * While {@link IArgParser}s always operate on a single argument's value, {@link IDatatype}s get access to the entire
     * {@link IArgConsumer}}.
     *
     * @param type The type to peek as
     * @return An instance of the specified type
     * @throws CommandInvalidTypeException If the parsing failed
     * @see IArgParser
     * @see #peekAs(Class, int)
     * @see #peekAsOrDefault(Class, Object)
     * @see #peekAsOrNull(Class)
     */
    <T> T peekAs(Class<T> type) throws CommandInvalidTypeException, CommandNotEnoughArgumentsException;

    /**
     * Tries to use a <b>stateless</b> {@link IArgParser} to parse the argument at the specified index into the specified
     * class
     * <p>
     * A critical difference between {@link IDatatype}s and {@link IArgParser}s is how many arguments they can take.
     * While {@link IArgParser}s always operate on a single argument's value, {@link IDatatype}s get access to the entire
     * {@link IArgConsumer}}.
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
    <T> T peekAsOrDefault(Class<T> type, T def, int index) throws CommandNotEnoughArgumentsException;

    /**
     * Tries to use a <b>stateless</b> {@link IArgParser} to parse the next argument into the specified class
     * <p>
     * A critical difference between {@link IDatatype}s and {@link IArgParser}s is how many arguments they can take.
     * While {@link IArgParser}s always operate on a single argument's value, {@link IDatatype}s get access to the entire
     * {@link IArgConsumer}}.
     *
     * @param type The type to peek as
     * @param def  The value to return if the argument can't be parsed
     * @return An instance of the specified type, or {@code def} if it couldn't be parsed
     * @see IArgParser
     * @see #peekAsOrDefault(Class, Object, int)
     * @see #peekAs(Class)
     * @see #peekAsOrNull(Class)
     */
    <T> T peekAsOrDefault(Class<T> type, T def) throws CommandNotEnoughArgumentsException;

    /**
     * Tries to use a <b>stateless</b> {@link IArgParser} to parse the argument at the specified index into the specified
     * class
     * <p>
     * A critical difference between {@link IDatatype}s and {@link IArgParser}s is how many arguments they can take.
     * While {@link IArgParser}s always operate on a single argument's value, {@link IDatatype}s get access to the entire
     * {@link IArgConsumer}}.
     *
     * @param type  The type to peek as
     * @param index The index to peek
     * @return An instance of the specified type, or {@code null} if it couldn't be parsed
     * @see IArgParser
     * @see #peekAsOrNull(Class)
     * @see #peekAs(Class, int)
     * @see #peekAsOrDefault(Class, Object, int)
     */
    <T> T peekAsOrNull(Class<T> type, int index) throws CommandNotEnoughArgumentsException;

    /**
     * Tries to use a <b>stateless</b> {@link IArgParser} to parse the next argument into the specified class
     * <p>
     * A critical difference between {@link IDatatype}s and {@link IArgParser}s is how many arguments they can take.
     * While {@link IArgParser}s always operate on a single argument's value, {@link IDatatype}s get access to the entire
     * {@link IArgConsumer}}.
     *
     * @param type The type to peek as
     * @return An instance of the specified type, or {@code null} if it couldn't be parsed
     * @see IArgParser
     * @see #peekAsOrNull(Class, int)
     * @see #peekAs(Class)
     * @see #peekAsOrDefault(Class, Object)
     */
    <T> T peekAsOrNull(Class<T> type) throws CommandNotEnoughArgumentsException;

    <T> T peekDatatype(IDatatypeFor<T> datatype) throws CommandInvalidTypeException, CommandNotEnoughArgumentsException;

    <T, O> T peekDatatype(IDatatypePost<T, O> datatype) throws CommandInvalidTypeException, CommandNotEnoughArgumentsException;

    <T, O> T peekDatatype(IDatatypePost<T, O> datatype, O original) throws CommandInvalidTypeException, CommandNotEnoughArgumentsException;

    <T> T peekDatatypeOrNull(IDatatypeFor<T> datatype);

    <T, O> T peekDatatypeOrNull(IDatatypePost<T, O> datatype);

    <T, O, D extends IDatatypePost<T, O>> T peekDatatypePost(D datatype, O original) throws CommandInvalidTypeException, CommandNotEnoughArgumentsException;

    <T, O, D extends IDatatypePost<T, O>> T peekDatatypePostOrDefault(D datatype, O original, T def);

    <T, O, D extends IDatatypePost<T, O>> T peekDatatypePostOrNull(D datatype, O original);

    /**
     * Attempts to get the specified {@link IDatatypeFor} from this ArgConsumer
     * <p>
     * A critical difference between {@link IDatatype}s and {@link IArgParser}s is how many arguments they can take.
     * While {@link IArgParser}s always operate on a single argument's value, {@link IDatatype}s get access to the entire
     * {@link IArgConsumer}}.
     * <p>
     * Since this is a peek operation, this ArgConsumer will not be mutated by any call to this method.
     *
     * @param datatype The datatype to get
     * @return The datatype instance
     * @see IDatatype
     * @see IDatatypeFor
     */
    <T, D extends IDatatypeFor<T>> T peekDatatypeFor(Class<D> datatype);

    /**
     * Attempts to get the specified {@link IDatatypeFor} from this ArgConsumer
     * <p>
     * A critical difference between {@link IDatatype}s and {@link IArgParser}s is how many arguments they can take.
     * While {@link IArgParser}s always operate on a single argument's value, {@link IDatatype}s get access to the entire
     * {@link IArgConsumer}}.
     * <p>
     * Since this is a peek operation, this ArgConsumer will not be mutated by any call to this method.
     *
     * @param datatype The datatype to get
     * @param def      The default value
     * @return The datatype instance, or {@code def} if it throws an exception
     * @see IDatatype
     * @see IDatatypeFor
     */
    <T, D extends IDatatypeFor<T>> T peekDatatypeForOrDefault(Class<D> datatype, T def);

    /**
     * Attempts to get the specified {@link IDatatypeFor} from this ArgConsumer
     * <p>
     * A critical difference between {@link IDatatype}s and {@link IArgParser}s is how many arguments they can take.
     * While {@link IArgParser}s always operate on a single argument's value, {@link IDatatype}s get access to the entire
     * {@link IArgConsumer}}.
     * <p>
     * Since this is a peek operation, this ArgConsumer will not be mutated by any call to this method.
     *
     * @param datatype The datatype to get
     * @return The datatype instance, or {@code null} if it throws an exception
     * @see IDatatype
     * @see IDatatypeFor
     */
    <T, D extends IDatatypeFor<T>> T peekDatatypeForOrNull(Class<D> datatype);

    /**
     * Gets the next argument and returns it. This consumes the first argument so that subsequent calls will return
     * later arguments
     *
     * @return The next argument
     * @throws CommandNotEnoughArgumentsException If there's less than one argument left
     */
    ICommandArgument get() throws CommandNotEnoughArgumentsException;

    /**
     * Gets the value of the next argument and returns it. This consumes the first argument so that subsequent calls
     * will return later arguments
     *
     * @return The value of the next argument
     * @throws CommandNotEnoughArgumentsException If there's less than one argument left
     */
    String getString() throws CommandNotEnoughArgumentsException;

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
     * @see ICommandArgument#getEnum(Class)
     */
    <E extends Enum<?>> E getEnum(Class<E> enumClass) throws CommandInvalidTypeException, CommandNotEnoughArgumentsException;

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
     * @see ICommandArgument#getEnum(Class)
     */
    <E extends Enum<?>> E getEnumOrDefault(Class<E> enumClass, E def) throws CommandNotEnoughArgumentsException;

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
     * @see ICommandArgument#getEnum(Class)
     */
    <E extends Enum<?>> E getEnumOrNull(Class<E> enumClass) throws CommandNotEnoughArgumentsException;

    /**
     * Tries to use a <b>stateless</b> {@link IArgParser} to parse the next argument into the specified class
     * <p>
     * A critical difference between {@link IDatatype}s and {@link IArgParser}s is how many arguments they can take.
     * While {@link IArgParser}s always operate on a single argument's value, {@link IDatatype}s get access to the entire
     * {@link IArgConsumer}}.
     *
     * @param type The type to peek as
     * @return An instance of the specified type
     * @throws CommandInvalidTypeException If the parsing failed
     * @see IArgParser
     * @see #get()
     * @see #getAsOrDefault(Class, Object)
     * @see #getAsOrNull(Class)
     * @see #peekAs(Class)
     * @see #peekAsOrDefault(Class, Object, int)
     * @see #peekAsOrNull(Class, int)
     */
    <T> T getAs(Class<T> type) throws CommandInvalidTypeException, CommandNotEnoughArgumentsException;

    /**
     * Tries to use a <b>stateless</b> {@link IArgParser} to parse the next argument into the specified class
     * <p>
     * A critical difference between {@link IDatatype}s and {@link IArgParser}s is how many arguments they can take.
     * While {@link IArgParser}s always operate on a single argument's value, {@link IDatatype}s get access to the entire
     * {@link IArgConsumer}}.
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
    <T> T getAsOrDefault(Class<T> type, T def) throws CommandNotEnoughArgumentsException;

    /**
     * Tries to use a <b>stateless</b> {@link IArgParser} to parse the next argument into the specified class
     * <p>
     * A critical difference between {@link IDatatype}s and {@link IArgParser}s is how many arguments they can take.
     * While {@link IArgParser}s always operate on a single argument's value, {@link IDatatype}s get access to the entire
     * {@link IArgConsumer}}.
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
    <T> T getAsOrNull(Class<T> type) throws CommandNotEnoughArgumentsException;

    <T, O, D extends IDatatypePost<T, O>> T getDatatypePost(D datatype, O original) throws CommandInvalidTypeException, CommandNotEnoughArgumentsException;

    <T, O, D extends IDatatypePost<T, O>> T getDatatypePostOrDefault(D datatype, O original, T _default);

    <T, O, D extends IDatatypePost<T, O>> T getDatatypePostOrNull(D datatype, O original);

    <T, D extends IDatatypeFor<T>> T getDatatypeFor(D datatype) throws CommandInvalidTypeException, CommandNotEnoughArgumentsException;

    <T, D extends IDatatypeFor<T>> T getDatatypeForOrDefault(D datatype, T def);

    <T, D extends IDatatypeFor<T>> T getDatatypeForOrNull(D datatype);

    <T extends IDatatype> Stream<String> tabCompleteDatatype(T datatype);

    /**
     * Returns the "raw rest" of the string. For example, from a string <code>arg1 arg2&nbsp;&nbsp;arg3</code>, split
     * into three {@link ICommandArgument}s {@code "arg1"}, {@code "arg2"}, and {@code "arg3"}:
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
    String rawRest();

    /**
     * @param min The minimum amount of arguments to require.
     * @throws CommandNotEnoughArgumentsException If there are less than {@code min} arguments left.
     * @see #requireMax(int)
     * @see #requireExactly(int)
     */
    void requireMin(int min) throws CommandNotEnoughArgumentsException;

    /**
     * @param max The maximum amount of arguments allowed.
     * @throws CommandTooManyArgumentsException If there are more than {@code max} arguments left.
     * @see #requireMin(int)
     * @see #requireExactly(int)
     */
    void requireMax(int max) throws CommandTooManyArgumentsException;

    /**
     * @param args The exact amount of arguments to require.
     * @throws CommandNotEnoughArgumentsException If there are less than {@code args} arguments left.
     * @throws CommandTooManyArgumentsException   If there are more than {@code args} arguments left.
     * @see #requireMin(int)
     * @see #requireMax(int)
     */
    void requireExactly(int args) throws CommandException;

    /**
     * @return If this {@link IArgConsumer}} has consumed at least one argument.
     * @see #consumed()
     * @see #consumedString()
     */
    boolean hasConsumed();

    /**
     * @return The last argument this {@link IArgConsumer}} has consumed, or an "unknown" argument, indicated by a
     * comamnd argument index that has a value of {@code -1}, if no arguments have been consumed yet.
     * @see #consumedString()
     * @see #hasConsumed()
     */
    ICommandArgument consumed();

    /**
     * @return The value of thelast argument this {@link IArgConsumer}} has consumed, or an empty string if no arguments
     * have been consumed yet
     * @see #consumed()
     * @see #hasConsumed()
     */
    String consumedString();

    /**
     * @return A copy of this {@link IArgConsumer}}. It has the same arguments (both consumed and not), but does not
     * affect or mutate this instance. Useful for the various {@code peek} functions
     */
    IArgConsumer copy();
}
