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

import baritone.api.utils.Helper;
import baritone.api.utils.command.Command;
import baritone.api.utils.command.argparser.ArgParser;
import baritone.api.utils.command.argument.CommandArgument;
import baritone.api.utils.command.datatypes.IDatatype;
import baritone.api.utils.command.datatypes.IDatatypeFor;
import baritone.api.utils.command.datatypes.IDatatypePost;
import baritone.api.utils.command.datatypes.RelativeFile;
import baritone.api.utils.command.exception.CommandException;
import baritone.api.utils.command.exception.CommandInvalidTypeException;
import baritone.api.utils.command.exception.CommandNoParserForTypeException;
import baritone.api.utils.command.exception.CommandNotEnoughArgumentsException;
import baritone.api.utils.command.exception.CommandTooManyArgumentsException;
import baritone.api.utils.command.exception.CommandUnhandledException;
import net.minecraft.util.EnumFacing;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

/**
 * The {@link ArgConsumer} is how {@link Command}s read the arguments passed to them. This class has many benefits:
 *
 * <ul>
 *     <li>Mutability. The whole concept of the {@link ArgConsumer} is to let you gradually consume arguments in any way
 *     you'd like. You can change your consumption based on earlier arguments, for subcommands for example.</li>
 *     <li>You don't need to keep track of your consumption. The {@link ArgConsumer} keeps track of the arguments you
 *     consume so that it can throw detailed exceptions whenever something is out of the ordinary. Additionally, if you
 *     need to retrieve an argument after you've already consumed it - look no further than {@link #consumed()}!</li>
 *     <li>Easy retrieval of many different types. If you need to retrieve an instance of an int or float for example,
 *     look no further than {@link #getAs(Class)}. If you need a more powerful way of retrieving data, try out the many
 *     {@link #getDatatype(Class)} methods.</li>
 *     <li>It's very easy to throw detailed exceptions. The {@link ArgConsumer} has many different methods that can
 *     enforce the number of arguments, the type of arguments, and more, throwing different types of
 *     {@link CommandException}s if something seems off. You're recommended to do all validation and store all needed
 *     data in variables BEFORE logging any data to chat via {@link Helper#logDirect(String)}, so that the error
 *     handlers can do their job and log the error to chat.</li>
 * </ul>
 */
public class ArgConsumer implements Cloneable {
    /**
     * The list of arguments in this ArgConsumer
     */
    public final LinkedList<CommandArgument> args;

    /**
     * The list of consumed arguments for this ArgConsumer. The most recently consumed argument is the last one
     */
    public final Deque<CommandArgument> consumed;

    private ArgConsumer(Deque<CommandArgument> args, Deque<CommandArgument> consumed) {
        this.args = new LinkedList<>(args);
        this.consumed = new LinkedList<>(consumed);
    }

    public ArgConsumer(List<CommandArgument> args) {
        this(new LinkedList<>(args), new LinkedList<>());
    }

    /**
     * @param num The number of arguments to check for
     * @return {@code true} if there are <i>at least</i> {@code num} arguments left in this {@link ArgConsumer}
     * @see #has()
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
    public boolean has() {
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
     * @see #has()
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
     * @see #has()
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
    public CommandArgument peek(int index) {
        requireMin(index + 1);
        return args.get(index);
    }

    /**
     * @return The next argument in this {@link ArgConsumer}. This does not mutate the {@link ArgConsumer}
     * @throws CommandNotEnoughArgumentsException If there is less than one argument left
     * @see #peek(int)
     * @see #peekString()
     * @see #peekAs(Class)
     * @see #peekDatatype(Class)
     * @see #peekDatatypeFor(Class)
     * @see #peekDatatypePost(Class, Object)
     * @see #get()
     */
    public CommandArgument peek() {
        return peek(0);
    }

    /**
     * @param index The index to peek
     * @param type  The type to check for
     * @return If an {@link ArgParser.Stateless} for the specified {@code type} would succeed in parsing the next
     * argument
     * @throws CommandNotEnoughArgumentsException If there is less than {@code index + 1} arguments left
     * @see #peek()
     * @see #getAs(Class)
     */
    public boolean is(Class<?> type, int index) {
        return peek(index).is(type);
    }

    /**
     * @param type The type to check for
     * @return If an {@link ArgParser.Stateless} for the specified {@code type} would succeed in parsing the next
     * argument
     * @throws CommandNotEnoughArgumentsException If there is less than one argument left
     * @see #peek()
     * @see #getAs(Class)
     */
    public boolean is(Class<?> type) {
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
    public String peekString(int index) {
        return peek(index).value;
    }

    /**
     * @return The value of the next argument in this {@link ArgConsumer}. This does not mutate the {@link ArgConsumer}
     * @throws CommandNotEnoughArgumentsException If there is less than one argument left
     * @see #peekString(int)
     * @see #getString()
     */
    public String peekString() {
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
    public <E extends Enum<?>> E peekEnum(Class<E> enumClass, int index) {
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
    public <E extends Enum<?>> E peekEnum(Class<E> enumClass) {
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
    public <E extends Enum<?>> E peekEnumOrNull(Class<E> enumClass, int index) {
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
    public <E extends Enum<?>> E peekEnumOrNull(Class<E> enumClass) {
        try {
            return peekEnumOrNull(enumClass, 0);
        } catch (CommandInvalidTypeException e) {
            return null;
        }
    }

    /**
     * Tries to use a <b>stateless</b> {@link ArgParser} to parse the argument at the specified index into the specified
     * class
     * <p>
     * A critical difference between {@link IDatatype}s and {@link ArgParser}s is how many arguments they can take.
     * While {@link ArgParser}s always operate on a single argument's value, {@link IDatatype}s get access to the entire
     * {@link ArgConsumer}.
     *
     * @param type  The type to peek as
     * @param index The index to peek
     * @return An instance of the specified type
     * @throws CommandNoParserForTypeException If no parser exists for that type
     * @throws CommandInvalidTypeException     If the parsing failed
     * @see ArgParser
     * @see ArgParser.Stateless
     * @see #peekAs(Class)
     * @see #peekAsOrDefault(Class, Object, int)
     * @see #peekAsOrNull(Class, int)
     */
    public <T> T peekAs(Class<T> type, int index) {
        return peek(index).getAs(type);
    }

    /**
     * Tries to use a <b>stateless</b> {@link ArgParser} to parse the next argument into the specified class
     * <p>
     * A critical difference between {@link IDatatype}s and {@link ArgParser}s is how many arguments they can take.
     * While {@link ArgParser}s always operate on a single argument's value, {@link IDatatype}s get access to the entire
     * {@link ArgConsumer}.
     *
     * @param type The type to peek as
     * @return An instance of the specified type
     * @throws CommandNoParserForTypeException If no parser exists for that type
     * @throws CommandInvalidTypeException     If the parsing failed
     * @see ArgParser
     * @see ArgParser.Stateless
     * @see #peekAs(Class, int)
     * @see #peekAsOrDefault(Class, Object)
     * @see #peekAsOrNull(Class)
     */
    public <T> T peekAs(Class<T> type) {
        return peekAs(type, 0);
    }

    /**
     * Tries to use a <b>stateless</b> {@link ArgParser} to parse the argument at the specified index into the specified
     * class
     * <p>
     * A critical difference between {@link IDatatype}s and {@link ArgParser}s is how many arguments they can take.
     * While {@link ArgParser}s always operate on a single argument's value, {@link IDatatype}s get access to the entire
     * {@link ArgConsumer}.
     *
     * @param type  The type to peek as
     * @param def   The value to return if the argument can't be parsed
     * @param index The index to peek
     * @return An instance of the specified type, or {@code def} if it couldn't be parsed
     * @see ArgParser
     * @see ArgParser.Stateless
     * @see #peekAsOrDefault(Class, Object)
     * @see #peekAs(Class, int)
     * @see #peekAsOrNull(Class, int)
     */
    public <T> T peekAsOrDefault(Class<T> type, T def, int index) {
        try {
            return peekAs(type, index);
        } catch (CommandInvalidTypeException e) {
            return def;
        }
    }

    /**
     * Tries to use a <b>stateless</b> {@link ArgParser} to parse the next argument into the specified class
     * <p>
     * A critical difference between {@link IDatatype}s and {@link ArgParser}s is how many arguments they can take.
     * While {@link ArgParser}s always operate on a single argument's value, {@link IDatatype}s get access to the entire
     * {@link ArgConsumer}.
     *
     * @param type The type to peek as
     * @param def  The value to return if the argument can't be parsed
     * @return An instance of the specified type, or {@code def} if it couldn't be parsed
     * @see ArgParser
     * @see ArgParser.Stateless
     * @see #peekAsOrDefault(Class, Object, int)
     * @see #peekAs(Class)
     * @see #peekAsOrNull(Class)
     */
    public <T> T peekAsOrDefault(Class<T> type, T def) {
        return peekAsOrDefault(type, def, 0);
    }


    /**
     * Tries to use a <b>stateless</b> {@link ArgParser} to parse the argument at the specified index into the specified
     * class
     * <p>
     * A critical difference between {@link IDatatype}s and {@link ArgParser}s is how many arguments they can take.
     * While {@link ArgParser}s always operate on a single argument's value, {@link IDatatype}s get access to the entire
     * {@link ArgConsumer}.
     *
     * @param type  The type to peek as
     * @param index The index to peek
     * @return An instance of the specified type, or {@code null} if it couldn't be parsed
     * @see ArgParser
     * @see ArgParser.Stateless
     * @see #peekAsOrNull(Class)
     * @see #peekAs(Class, int)
     * @see #peekAsOrDefault(Class, Object, int)
     */
    public <T> T peekAsOrNull(Class<T> type, int index) {
        return peekAsOrDefault(type, null, index);
    }

    /**
     * Tries to use a <b>stateless</b> {@link ArgParser} to parse the next argument into the specified class
     * <p>
     * A critical difference between {@link IDatatype}s and {@link ArgParser}s is how many arguments they can take.
     * While {@link ArgParser}s always operate on a single argument's value, {@link IDatatype}s get access to the entire
     * {@link ArgConsumer}.
     *
     * @param type The type to peek as
     * @return An instance of the specified type, or {@code null} if it couldn't be parsed
     * @see ArgParser
     * @see ArgParser.Stateless
     * @see #peekAsOrNull(Class, int)
     * @see #peekAs(Class)
     * @see #peekAsOrDefault(Class, Object)
     */
    public <T> T peekAsOrNull(Class<T> type) {
        return peekAsOrNull(type, 0);
    }

    /**
     * Attempts to get the specified datatype from this ArgConsumer
     * <p>
     * A critical difference between {@link IDatatype}s and {@link ArgParser}s is how many arguments they can take.
     * While {@link ArgParser}s always operate on a single argument's value, {@link IDatatype}s get access to the entire
     * {@link ArgConsumer}.
     * <p>
     * Since this is a peek operation, this ArgConsumer will not be mutated by any call to this method.
     *
     * @param datatype The datatype to get
     * @return The datatype instance
     * @see IDatatype
     * @see #peekDatatype(Class)
     * @see #peekDatatypeOrNull(Class)
     * @see #peekDatatypePost(Class, Object)
     * @see #peekDatatypePostOrDefault(Class, Object, Object)
     * @see #peekDatatypePostOrNull(Class, Object)
     * @see #peekDatatypeFor(Class)
     * @see #peekDatatypeForOrDefault(Class, Object)
     * @see #peekDatatypeForOrNull(Class)
     * @see #getDatatype(Class)
     * @see #getDatatypeOrNull(Class)
     * @see #getDatatypePost(Class, Object)
     * @see #getDatatypePostOrDefault(Class, Object, Object)
     * @see #getDatatypePostOrNull(Class, Object)
     * @see #getDatatypeFor(Class)
     * @see #getDatatypeForOrDefault(Class, Object)
     * @see #getDatatypeForOrNull(Class)
     */
    public <T extends IDatatype> T peekDatatype(Class<T> datatype) {
        return clone().getDatatype(datatype);
    }

    /**
     * Attempts to get the specified datatype from this ArgConsumer
     * <p>
     * A critical difference between {@link IDatatype}s and {@link ArgParser}s is how many arguments they can take.
     * While {@link ArgParser}s always operate on a single argument's value, {@link IDatatype}s get access to the entire
     * {@link ArgConsumer}.
     * <p>
     * Since this is a peek operation, this ArgConsumer will not be mutated by any call to this method.
     *
     * @param datatype The datatype to get
     * @return The datatype instance, or {@code null} if it throws an exception
     * @see IDatatype
     * @see #peekDatatype(Class)
     * @see #peekDatatypeOrNull(Class)
     * @see #peekDatatypePost(Class, Object)
     * @see #peekDatatypePostOrDefault(Class, Object, Object)
     * @see #peekDatatypePostOrNull(Class, Object)
     * @see #peekDatatypeFor(Class)
     * @see #peekDatatypeForOrDefault(Class, Object)
     * @see #peekDatatypeForOrNull(Class)
     * @see #getDatatype(Class)
     * @see #getDatatypeOrNull(Class)
     * @see #getDatatypePost(Class, Object)
     * @see #getDatatypePostOrDefault(Class, Object, Object)
     * @see #getDatatypePostOrNull(Class, Object)
     * @see #getDatatypeFor(Class)
     * @see #getDatatypeForOrDefault(Class, Object)
     * @see #getDatatypeForOrNull(Class)
     */
    public <T extends IDatatype> T peekDatatypeOrNull(Class<T> datatype) {
        return clone().getDatatypeOrNull(datatype);
    }

    /**
     * Attempts to get the specified {@link IDatatypePost} from this ArgConsumer
     * <p>
     * A critical difference between {@link IDatatype}s and {@link ArgParser}s is how many arguments they can take.
     * While {@link ArgParser}s always operate on a single argument's value, {@link IDatatype}s get access to the entire
     * {@link ArgConsumer}.
     * <p>
     * Since this is a peek operation, this ArgConsumer will not be mutated by any call to this method.
     *
     * @param datatype The datatype to get
     * @return The datatype instance
     * @see IDatatype
     * @see IDatatypePost
     * @see #peekDatatype(Class)
     * @see #peekDatatypeOrNull(Class)
     * @see #peekDatatypePost(Class, Object)
     * @see #peekDatatypePostOrDefault(Class, Object, Object)
     * @see #peekDatatypePostOrNull(Class, Object)
     * @see #peekDatatypeFor(Class)
     * @see #peekDatatypeForOrDefault(Class, Object)
     * @see #peekDatatypeForOrNull(Class)
     * @see #getDatatype(Class)
     * @see #getDatatypeOrNull(Class)
     * @see #getDatatypePost(Class, Object)
     * @see #getDatatypePostOrDefault(Class, Object, Object)
     * @see #getDatatypePostOrNull(Class, Object)
     * @see #getDatatypeFor(Class)
     * @see #getDatatypeForOrDefault(Class, Object)
     * @see #getDatatypeForOrNull(Class)
     */
    public <T, O, D extends IDatatypePost<T, O>> T peekDatatypePost(Class<D> datatype, O original) {
        return clone().getDatatypePost(datatype, original);
    }

    /**
     * Attempts to get the specified {@link IDatatypePost} from this ArgConsumer
     * <p>
     * A critical difference between {@link IDatatype}s and {@link ArgParser}s is how many arguments they can take.
     * While {@link ArgParser}s always operate on a single argument's value, {@link IDatatype}s get access to the entire
     * {@link ArgConsumer}.
     * <p>
     * Since this is a peek operation, this ArgConsumer will not be mutated by any call to this method.
     *
     * @param datatype The datatype to get
     * @param def      The default value
     * @return The datatype instance, or {@code def} if it throws an exception
     * @see IDatatype
     * @see IDatatypePost
     * @see #peekDatatype(Class)
     * @see #peekDatatypeOrNull(Class)
     * @see #peekDatatypePost(Class, Object)
     * @see #peekDatatypePostOrDefault(Class, Object, Object)
     * @see #peekDatatypePostOrNull(Class, Object)
     * @see #peekDatatypeFor(Class)
     * @see #peekDatatypeForOrDefault(Class, Object)
     * @see #peekDatatypeForOrNull(Class)
     * @see #getDatatype(Class)
     * @see #getDatatypeOrNull(Class)
     * @see #getDatatypePost(Class, Object)
     * @see #getDatatypePostOrDefault(Class, Object, Object)
     * @see #getDatatypePostOrNull(Class, Object)
     * @see #getDatatypeFor(Class)
     * @see #getDatatypeForOrDefault(Class, Object)
     * @see #getDatatypeForOrNull(Class)
     */
    public <T, O, D extends IDatatypePost<T, O>> T peekDatatypePostOrDefault(Class<D> datatype, O original, T def) {
        return clone().getDatatypePostOrDefault(datatype, original, def);
    }

    /**
     * Attempts to get the specified {@link IDatatypePost} from this ArgConsumer
     * <p>
     * A critical difference between {@link IDatatype}s and {@link ArgParser}s is how many arguments they can take.
     * While {@link ArgParser}s always operate on a single argument's value, {@link IDatatype}s get access to the entire
     * {@link ArgConsumer}.
     * <p>
     * Since this is a peek operation, this ArgConsumer will not be mutated by any call to this method.
     *
     * @param datatype The datatype to get
     * @return The datatype instance, or {@code null} if it throws an exception
     * @see IDatatype
     * @see IDatatypePost
     * @see #peekDatatype(Class)
     * @see #peekDatatypeOrNull(Class)
     * @see #peekDatatypePost(Class, Object)
     * @see #peekDatatypePostOrDefault(Class, Object, Object)
     * @see #peekDatatypePostOrNull(Class, Object)
     * @see #peekDatatypeFor(Class)
     * @see #peekDatatypeForOrDefault(Class, Object)
     * @see #peekDatatypeForOrNull(Class)
     * @see #getDatatype(Class)
     * @see #getDatatypeOrNull(Class)
     * @see #getDatatypePost(Class, Object)
     * @see #getDatatypePostOrDefault(Class, Object, Object)
     * @see #getDatatypePostOrNull(Class, Object)
     * @see #getDatatypeFor(Class)
     * @see #getDatatypeForOrDefault(Class, Object)
     * @see #getDatatypeForOrNull(Class)
     */
    public <T, O, D extends IDatatypePost<T, O>> T peekDatatypePostOrNull(Class<D> datatype, O original) {
        return peekDatatypePostOrDefault(datatype, original, null);
    }

    /**
     * Attempts to get the specified {@link IDatatypeFor} from this ArgConsumer
     * <p>
     * A critical difference between {@link IDatatype}s and {@link ArgParser}s is how many arguments they can take.
     * While {@link ArgParser}s always operate on a single argument's value, {@link IDatatype}s get access to the entire
     * {@link ArgConsumer}.
     * <p>
     * Since this is a peek operation, this ArgConsumer will not be mutated by any call to this method.
     *
     * @param datatype The datatype to get
     * @return The datatype instance
     * @see IDatatype
     * @see IDatatypeFor
     * @see #peekDatatype(Class)
     * @see #peekDatatypeOrNull(Class)
     * @see #peekDatatypePost(Class, Object)
     * @see #peekDatatypePostOrDefault(Class, Object, Object)
     * @see #peekDatatypePostOrNull(Class, Object)
     * @see #peekDatatypeFor(Class)
     * @see #peekDatatypeForOrDefault(Class, Object)
     * @see #peekDatatypeForOrNull(Class)
     * @see #getDatatype(Class)
     * @see #getDatatypeOrNull(Class)
     * @see #getDatatypePost(Class, Object)
     * @see #getDatatypePostOrDefault(Class, Object, Object)
     * @see #getDatatypePostOrNull(Class, Object)
     * @see #getDatatypeFor(Class)
     * @see #getDatatypeForOrDefault(Class, Object)
     * @see #getDatatypeForOrNull(Class)
     */
    public <T, D extends IDatatypeFor<T>> T peekDatatypeFor(Class<D> datatype) {
        return clone().peekDatatypeFor(datatype);
    }

    /**
     * Attempts to get the specified {@link IDatatypeFor} from this ArgConsumer
     * <p>
     * A critical difference between {@link IDatatype}s and {@link ArgParser}s is how many arguments they can take.
     * While {@link ArgParser}s always operate on a single argument's value, {@link IDatatype}s get access to the entire
     * {@link ArgConsumer}.
     * <p>
     * Since this is a peek operation, this ArgConsumer will not be mutated by any call to this method.
     *
     * @param datatype The datatype to get
     * @param def      The default value
     * @return The datatype instance, or {@code def} if it throws an exception
     * @see IDatatype
     * @see IDatatypeFor
     * @see #peekDatatype(Class)
     * @see #peekDatatypeOrNull(Class)
     * @see #peekDatatypePost(Class, Object)
     * @see #peekDatatypePostOrDefault(Class, Object, Object)
     * @see #peekDatatypePostOrNull(Class, Object)
     * @see #peekDatatypeFor(Class)
     * @see #peekDatatypeForOrDefault(Class, Object)
     * @see #peekDatatypeForOrNull(Class)
     * @see #getDatatype(Class)
     * @see #getDatatypeOrNull(Class)
     * @see #getDatatypePost(Class, Object)
     * @see #getDatatypePostOrDefault(Class, Object, Object)
     * @see #getDatatypePostOrNull(Class, Object)
     * @see #getDatatypeFor(Class)
     * @see #getDatatypeForOrDefault(Class, Object)
     * @see #getDatatypeForOrNull(Class)
     */
    public <T, D extends IDatatypeFor<T>> T peekDatatypeForOrDefault(Class<D> datatype, T def) {
        return clone().peekDatatypeForOrDefault(datatype, def);
    }

    /**
     * Attempts to get the specified {@link IDatatypeFor} from this ArgConsumer
     * <p>
     * A critical difference between {@link IDatatype}s and {@link ArgParser}s is how many arguments they can take.
     * While {@link ArgParser}s always operate on a single argument's value, {@link IDatatype}s get access to the entire
     * {@link ArgConsumer}.
     * <p>
     * Since this is a peek operation, this ArgConsumer will not be mutated by any call to this method.
     *
     * @param datatype The datatype to get
     * @return The datatype instance, or {@code null} if it throws an exception
     * @see IDatatype
     * @see IDatatypeFor
     * @see #peekDatatype(Class)
     * @see #peekDatatypeOrNull(Class)
     * @see #peekDatatypePost(Class, Object)
     * @see #peekDatatypePostOrDefault(Class, Object, Object)
     * @see #peekDatatypePostOrNull(Class, Object)
     * @see #peekDatatypeFor(Class)
     * @see #peekDatatypeForOrDefault(Class, Object)
     * @see #peekDatatypeForOrNull(Class)
     * @see #getDatatype(Class)
     * @see #getDatatypeOrNull(Class)
     * @see #getDatatypePost(Class, Object)
     * @see #getDatatypePostOrDefault(Class, Object, Object)
     * @see #getDatatypePostOrNull(Class, Object)
     * @see #getDatatypeFor(Class)
     * @see #getDatatypeForOrDefault(Class, Object)
     * @see #getDatatypeForOrNull(Class)
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
    public CommandArgument get() {
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
    public String getString() {
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
    public <E extends Enum<?>> E getEnum(Class<E> enumClass) {
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
    public <E extends Enum<?>> E getEnumOrDefault(Class<E> enumClass, E def) {
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
    public <E extends Enum<?>> E getEnumOrNull(Class<E> enumClass) {
        return getEnumOrDefault(enumClass, null);
    }

    /**
     * Tries to use a <b>stateless</b> {@link ArgParser} to parse the next argument into the specified class
     * <p>
     * A critical difference between {@link IDatatype}s and {@link ArgParser}s is how many arguments they can take.
     * While {@link ArgParser}s always operate on a single argument's value, {@link IDatatype}s get access to the entire
     * {@link ArgConsumer}.
     *
     * @param type The type to peek as
     * @return An instance of the specified type
     * @throws CommandNoParserForTypeException If no parser exists for that type
     * @throws CommandInvalidTypeException     If the parsing failed
     * @see ArgParser
     * @see ArgParser.Stateless
     * @see #get()
     * @see #getAsOrDefault(Class, Object)
     * @see #getAsOrNull(Class)
     * @see #peekAs(Class)
     * @see #peekAsOrDefault(Class, Object, int)
     * @see #peekAsOrNull(Class, int)
     */
    public <T> T getAs(Class<T> type) {
        return get().getAs(type);
    }

    /**
     * Tries to use a <b>stateless</b> {@link ArgParser} to parse the next argument into the specified class
     * <p>
     * A critical difference between {@link IDatatype}s and {@link ArgParser}s is how many arguments they can take.
     * While {@link ArgParser}s always operate on a single argument's value, {@link IDatatype}s get access to the entire
     * {@link ArgConsumer}.
     *
     * @param type The type to peek as
     * @param def  The default value
     * @return An instance of the specified type, or {@code def} if it couldn't be parsed
     * @see ArgParser
     * @see ArgParser.Stateless
     * @see #get()
     * @see #getAs(Class)
     * @see #getAsOrNull(Class)
     * @see #peekAs(Class)
     * @see #peekAsOrDefault(Class, Object, int)
     * @see #peekAsOrNull(Class, int)
     */
    public <T> T getAsOrDefault(Class<T> type, T def) {
        try {
            T val = peek().getAs(type);
            get();
            return val;
        } catch (CommandInvalidTypeException e) {
            return def;
        }
    }

    /**
     * Tries to use a <b>stateless</b> {@link ArgParser} to parse the next argument into the specified class
     * <p>
     * A critical difference between {@link IDatatype}s and {@link ArgParser}s is how many arguments they can take.
     * While {@link ArgParser}s always operate on a single argument's value, {@link IDatatype}s get access to the entire
     * {@link ArgConsumer}.
     *
     * @param type The type to peek as
     * @return An instance of the specified type, or {@code null} if it couldn't be parsed
     * @see ArgParser
     * @see ArgParser.Stateless
     * @see #get()
     * @see #getAs(Class)
     * @see #getAsOrDefault(Class, Object)
     * @see #peekAs(Class)
     * @see #peekAsOrDefault(Class, Object, int)
     * @see #peekAsOrNull(Class, int)
     */
    public <T> T getAsOrNull(Class<T> type) {
        return getAsOrDefault(type, null);
    }

    /**
     * Attempts to get the specified datatype from this ArgConsumer
     * <p>
     * A critical difference between {@link IDatatype}s and {@link ArgParser}s is how many arguments they can take.
     * While {@link ArgParser}s always operate on a single argument's value, {@link IDatatype}s get access to the entire
     * {@link ArgConsumer}.
     *
     * @param datatype The datatype to get
     * @return The datatype instance
     * @see IDatatype
     * @see #getDatatype(Class)
     * @see #getDatatypeOrNull(Class)
     * @see #getDatatypePost(Class, Object)
     * @see #getDatatypePostOrDefault(Class, Object, Object)
     * @see #getDatatypePostOrNull(Class, Object)
     * @see #getDatatypeFor(Class)
     * @see #getDatatypeForOrDefault(Class, Object)
     * @see #getDatatypeForOrNull(Class)
     * @see #peekDatatype(Class)
     * @see #peekDatatypeOrNull(Class)
     * @see #peekDatatypePost(Class, Object)
     * @see #peekDatatypePostOrDefault(Class, Object, Object)
     * @see #peekDatatypePostOrNull(Class, Object)
     * @see #peekDatatypeFor(Class)
     * @see #peekDatatypeForOrDefault(Class, Object)
     * @see #peekDatatypeForOrNull(Class)
     */
    public <T extends IDatatype> T getDatatype(Class<T> datatype) {
        try {
            return datatype.getConstructor(ArgConsumer.class).newInstance(this);
        } catch (InvocationTargetException e) {
            throw new CommandInvalidTypeException(has() ? peek() : consumed(), datatype.getSimpleName());
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException e) {
            throw new CommandUnhandledException(e);
        }
    }

    /**
     * Attempts to get the specified datatype from this ArgConsumer
     * <p>
     * A critical difference between {@link IDatatype}s and {@link ArgParser}s is how many arguments they can take.
     * While {@link ArgParser}s always operate on a single argument's value, {@link IDatatype}s get access to the entire
     * {@link ArgConsumer}.
     * <p>
     * The state of this {@link ArgConsumer} is restored if the datatype could not be gotten.
     *
     * @param datatype The datatype to get
     * @return The datatype instance, or {@code null} if it throws an exception
     * @see IDatatype
     * @see #getDatatype(Class)
     * @see #getDatatypeOrNull(Class)
     * @see #getDatatypePost(Class, Object)
     * @see #getDatatypePostOrDefault(Class, Object, Object)
     * @see #getDatatypePostOrNull(Class, Object)
     * @see #getDatatypeFor(Class)
     * @see #getDatatypeForOrDefault(Class, Object)
     * @see #getDatatypeForOrNull(Class)
     * @see #peekDatatype(Class)
     * @see #peekDatatypeOrNull(Class)
     * @see #peekDatatypePost(Class, Object)
     * @see #peekDatatypePostOrDefault(Class, Object, Object)
     * @see #peekDatatypePostOrNull(Class, Object)
     * @see #peekDatatypeFor(Class)
     * @see #peekDatatypeForOrDefault(Class, Object)
     * @see #peekDatatypeForOrNull(Class)
     */
    public <T extends IDatatype> T getDatatypeOrNull(Class<T> datatype) {
        List<CommandArgument> argsSnapshot = new ArrayList<>(args);
        List<CommandArgument> consumedSnapshot = new ArrayList<>(consumed);

        try {
            return getDatatype(datatype);
        } catch (CommandInvalidTypeException e) {
            args.clear();
            args.addAll(argsSnapshot);
            consumed.clear();
            consumed.addAll(consumedSnapshot);

            return null;
        }
    }

    /**
     * Attempts to get the specified {@link IDatatypePost} from this ArgConsumer
     * <p>
     * A critical difference between {@link IDatatype}s and {@link ArgParser}s is how many arguments they can take.
     * While {@link ArgParser}s always operate on a single argument's value, {@link IDatatype}s get access to the entire
     * {@link ArgConsumer}.
     *
     * @param datatype The datatype to get
     * @return The datatype instance
     * @see IDatatype
     * @see IDatatypePost
     * @see #getDatatype(Class)
     * @see #getDatatypeOrNull(Class)
     * @see #getDatatypePost(Class, Object)
     * @see #getDatatypePostOrDefault(Class, Object, Object)
     * @see #getDatatypePostOrNull(Class, Object)
     * @see #getDatatypeFor(Class)
     * @see #getDatatypeForOrDefault(Class, Object)
     * @see #getDatatypeForOrNull(Class)
     * @see #peekDatatype(Class)
     * @see #peekDatatypeOrNull(Class)
     * @see #peekDatatypePost(Class, Object)
     * @see #peekDatatypePostOrDefault(Class, Object, Object)
     * @see #peekDatatypePostOrNull(Class, Object)
     * @see #peekDatatypeFor(Class)
     * @see #peekDatatypeForOrDefault(Class, Object)
     * @see #peekDatatypeForOrNull(Class)
     */
    public <T, O, D extends IDatatypePost<T, O>> T getDatatypePost(Class<D> datatype, O original) {
        return getDatatype(datatype).apply(original);
    }

    /**
     * Attempts to get the specified {@link IDatatypePost} from this ArgConsumer
     * <p>
     * A critical difference between {@link IDatatype}s and {@link ArgParser}s is how many arguments they can take.
     * While {@link ArgParser}s always operate on a single argument's value, {@link IDatatype}s get access to the entire
     * {@link ArgConsumer}.
     * <p>
     * The state of this {@link ArgConsumer} is restored if the datatype could not be gotten.
     *
     * @param datatype The datatype to get
     * @param def      The default value
     * @return The datatype instance, or {@code def} if it throws an exception
     * @see IDatatype
     * @see IDatatypePost
     * @see #getDatatype(Class)
     * @see #getDatatypeOrNull(Class)
     * @see #getDatatypePost(Class, Object)
     * @see #getDatatypePostOrDefault(Class, Object, Object)
     * @see #getDatatypePostOrNull(Class, Object)
     * @see #getDatatypeFor(Class)
     * @see #getDatatypeForOrDefault(Class, Object)
     * @see #getDatatypeForOrNull(Class)
     * @see #peekDatatype(Class)
     * @see #peekDatatypeOrNull(Class)
     * @see #peekDatatypePost(Class, Object)
     * @see #peekDatatypePostOrDefault(Class, Object, Object)
     * @see #peekDatatypePostOrNull(Class, Object)
     * @see #peekDatatypeFor(Class)
     * @see #peekDatatypeForOrDefault(Class, Object)
     * @see #peekDatatypeForOrNull(Class)
     */
    public <T, O, D extends IDatatypePost<T, O>> T getDatatypePostOrDefault(Class<D> datatype, O original, T def) {
        List<CommandArgument> argsSnapshot = new ArrayList<>(args);
        List<CommandArgument> consumedSnapshot = new ArrayList<>(consumed);

        try {
            return getDatatypePost(datatype, original);
        } catch (CommandException e) {
            args.clear();
            args.addAll(argsSnapshot);
            consumed.clear();
            consumed.addAll(consumedSnapshot);

            return def;
        }
    }

    /**
     * Attempts to get the specified {@link IDatatypePost} from this ArgConsumer
     * <p>
     * A critical difference between {@link IDatatype}s and {@link ArgParser}s is how many arguments they can take.
     * While {@link ArgParser}s always operate on a single argument's value, {@link IDatatype}s get access to the entire
     * {@link ArgConsumer}.
     * <p>
     * The state of this {@link ArgConsumer} is restored if the datatype could not be gotten.
     *
     * @param datatype The datatype to get
     * @return The datatype instance, or {@code null} if it throws an exception
     * @see IDatatype
     * @see IDatatypePost
     * @see #getDatatype(Class)
     * @see #getDatatypeOrNull(Class)
     * @see #getDatatypePost(Class, Object)
     * @see #getDatatypePostOrDefault(Class, Object, Object)
     * @see #getDatatypePostOrNull(Class, Object)
     * @see #getDatatypeFor(Class)
     * @see #getDatatypeForOrDefault(Class, Object)
     * @see #getDatatypeForOrNull(Class)
     * @see #peekDatatype(Class)
     * @see #peekDatatypeOrNull(Class)
     * @see #peekDatatypePost(Class, Object)
     * @see #peekDatatypePostOrDefault(Class, Object, Object)
     * @see #peekDatatypePostOrNull(Class, Object)
     * @see #peekDatatypeFor(Class)
     * @see #peekDatatypeForOrDefault(Class, Object)
     * @see #peekDatatypeForOrNull(Class)
     */
    public <T, O, D extends IDatatypePost<T, O>> T getDatatypePostOrNull(Class<D> datatype, O original) {
        return getDatatypePostOrDefault(datatype, original, null);
    }

    /**
     * Attempts to get the specified {@link IDatatypeFor} from this ArgConsumer
     * <p>
     * A critical difference between {@link IDatatype}s and {@link ArgParser}s is how many arguments they can take.
     * While {@link ArgParser}s always operate on a single argument's value, {@link IDatatype}s get access to the entire
     * {@link ArgConsumer}.
     *
     * @param datatype The datatype to get
     * @return The datatype instance
     * @see IDatatype
     * @see IDatatypeFor
     * @see #getDatatype(Class)
     * @see #getDatatypeOrNull(Class)
     * @see #getDatatypePost(Class, Object)
     * @see #getDatatypePostOrDefault(Class, Object, Object)
     * @see #getDatatypePostOrNull(Class, Object)
     * @see #getDatatypeFor(Class)
     * @see #getDatatypeForOrDefault(Class, Object)
     * @see #getDatatypeForOrNull(Class)
     * @see #peekDatatype(Class)
     * @see #peekDatatypeOrNull(Class)
     * @see #peekDatatypePost(Class, Object)
     * @see #peekDatatypePostOrDefault(Class, Object, Object)
     * @see #peekDatatypePostOrNull(Class, Object)
     * @see #peekDatatypeFor(Class)
     * @see #peekDatatypeForOrDefault(Class, Object)
     * @see #peekDatatypeForOrNull(Class)
     */
    public <T, D extends IDatatypeFor<T>> T getDatatypeFor(Class<D> datatype) {
        return getDatatype(datatype).get();
    }

    /**
     * Attempts to get the specified {@link IDatatypeFor} from this ArgConsumer
     * <p>
     * A critical difference between {@link IDatatype}s and {@link ArgParser}s is how many arguments they can take.
     * While {@link ArgParser}s always operate on a single argument's value, {@link IDatatype}s get access to the entire
     * {@link ArgConsumer}.
     * <p>
     * The state of this {@link ArgConsumer} is restored if the datatype could not be gotten.
     *
     * @param datatype The datatype to get
     * @param def      The default value
     * @return The datatype instance, or {@code def} if it throws an exception
     * @see IDatatype
     * @see IDatatypeFor
     * @see #getDatatype(Class)
     * @see #getDatatypeOrNull(Class)
     * @see #getDatatypePost(Class, Object)
     * @see #getDatatypePostOrDefault(Class, Object, Object)
     * @see #getDatatypePostOrNull(Class, Object)
     * @see #getDatatypeFor(Class)
     * @see #getDatatypeForOrDefault(Class, Object)
     * @see #getDatatypeForOrNull(Class)
     * @see #peekDatatype(Class)
     * @see #peekDatatypeOrNull(Class)
     * @see #peekDatatypePost(Class, Object)
     * @see #peekDatatypePostOrDefault(Class, Object, Object)
     * @see #peekDatatypePostOrNull(Class, Object)
     * @see #peekDatatypeFor(Class)
     * @see #peekDatatypeForOrDefault(Class, Object)
     * @see #peekDatatypeForOrNull(Class)
     */
    public <T, D extends IDatatypeFor<T>> T getDatatypeForOrDefault(Class<D> datatype, T def) {
        List<CommandArgument> argsSnapshot = new ArrayList<>(args);
        List<CommandArgument> consumedSnapshot = new ArrayList<>(consumed);

        try {
            return getDatatypeFor(datatype);
        } catch (CommandInvalidTypeException e) {
            args.clear();
            args.addAll(argsSnapshot);
            consumed.clear();
            consumed.addAll(consumedSnapshot);

            return def;
        }
    }

    /**
     * Attempts to get the specified {@link IDatatypeFor} from this ArgConsumer
     * <p>
     * A critical difference between {@link IDatatype}s and {@link ArgParser}s is how many arguments they can take.
     * While {@link ArgParser}s always operate on a single argument's value, {@link IDatatype}s get access to the entire
     * {@link ArgConsumer}.
     * <p>
     * The state of this {@link ArgConsumer} is restored if the datatype could not be gotten.
     *
     * @param datatype The datatype to get
     * @return The datatype instance, or {@code null} if it throws an exception
     * @see IDatatype
     * @see IDatatypeFor
     * @see #getDatatype(Class)
     * @see #getDatatypeOrNull(Class)
     * @see #getDatatypePost(Class, Object)
     * @see #getDatatypePostOrDefault(Class, Object, Object)
     * @see #getDatatypePostOrNull(Class, Object)
     * @see #getDatatypeFor(Class)
     * @see #getDatatypeForOrDefault(Class, Object)
     * @see #getDatatypeForOrNull(Class)
     * @see #peekDatatype(Class)
     * @see #peekDatatypeOrNull(Class)
     * @see #peekDatatypePost(Class, Object)
     * @see #peekDatatypePostOrDefault(Class, Object, Object)
     * @see #peekDatatypePostOrNull(Class, Object)
     * @see #peekDatatypeFor(Class)
     * @see #peekDatatypeForOrDefault(Class, Object)
     * @see #peekDatatypeForOrNull(Class)
     */
    public <T, D extends IDatatypeFor<T>> T getDatatypeForOrNull(Class<D> datatype) {
        return getDatatypeForOrDefault(datatype, null);
    }

    /**
     * One benefit over datatypes over {@link ArgParser}s is that instead of each command trying to guess what values
     * the datatype will accept, or simply not tab completing at all, datatypes that support tab completion can provide
     * accurate information using the same methods used to parse arguments in the first place.
     * <p>
     * See {@link RelativeFile} for a very advanced example of tab completion. You wouldn't want this pasted into every
     * command that uses files - right? Right?
     *
     * @param datatype The datatype to get tab completions from
     * @return A stream representing the strings that can be tab completed. DO NOT INCLUDE SPACES IN ANY STRINGS.
     * @see IDatatype#tabComplete(ArgConsumer)
     */
    public <T extends IDatatype> Stream<String> tabCompleteDatatype(Class<T> datatype) {
        try {
            return datatype.getConstructor().newInstance().tabComplete(this);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        } catch (CommandException ignored) {}

        return Stream.empty();
    }

    /**
     * Returns the "raw rest" of the string. For example, from a string <code>arg1 arg2&nbsp;&nbsp;arg3</code>, split
     * into three {@link CommandArgument}s {@code "arg1"}, {@code "arg2"}, and {@code "arg3"}:
     *
     * <ul>
     *     <li>{@code rawRest()} would return <code>arg1 arg2&nbsp;&nbsp;arg3</code></li>
     *     <li>After calling {@link #get()}, {@code rawRest()} would return <code>arg2&nbsp;&nbsp;arg3</code> (note the
     *     double space - it is preserved!)</li>
     *     <li>After calling {@link #get()} again, {@code rawRest()} would return {@code "arg3"}</li>
     *     <li>After calling {@link #get()} one last time, {@code rawRest()} would return {@code ""}</li>
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
    public void requireMin(int min) {
        if (args.size() < min) {
            throw new CommandNotEnoughArgumentsException(min + consumed.size());
        }
    }

    /**
     * @param max The maximum amount of arguments allowed.
     * @throws CommandNotEnoughArgumentsException If there are more than {@code max} arguments left.
     * @see #requireMin(int)
     * @see #requireExactly(int)
     */
    public void requireMax(int max) {
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
    public void requireExactly(int args) {
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
     * @return A clone of this {@link ArgConsumer}. It has the same arguments (both consumed and not), but does not
     * affect or mutate this instance. Useful for the various {@code peek} functions
     */
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public ArgConsumer clone() {
        return new ArgConsumer(args, consumed);
    }

    /**
     * @param string The string to split
     * @return A new {@link ArgConsumer} constructed from the specified string
     * @see CommandArgument#from(String)
     */
    public static ArgConsumer from(String string) {
        return new ArgConsumer(CommandArgument.from(string));
    }
}
