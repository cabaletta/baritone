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

package baritone.api.utils.command.argument;

import baritone.api.utils.command.argparser.ArgParser;
import baritone.api.utils.command.argparser.ArgParserManager;
import baritone.api.utils.command.exception.CommandInvalidArgumentException;
import baritone.api.utils.command.exception.CommandInvalidTypeException;
import baritone.api.utils.command.exception.CommandNoParserForTypeException;
import baritone.api.utils.command.helpers.arguments.ArgConsumer;
import net.minecraft.util.EnumFacing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A {@link CommandArgument} is an immutable object representing one command argument. It contains data on the index of
 * that argument, its value, and the rest of the string that argument was found in
 * <p>
 * You're recommended to use {@link ArgConsumer}s to handle these. Check out {@link ArgConsumer#from(String)}
 */
public class CommandArgument {

    public final int index;
    public final String value;
    public final String rawRest;
    public final static Pattern argPattern = Pattern.compile("\\S+");

    private CommandArgument(int index, String value, String rawRest) {
        this.index = index;
        this.value = value;
        this.rawRest = rawRest;
    }

    /**
     * Gets an enum value from the enum class with the same name as this argument's value
     * <p>
     * For example if you getEnum as an {@link EnumFacing}, and this argument's value is "up", it will return {@link
     * EnumFacing#UP}
     *
     * @param enumClass The enum class to search
     * @return An enum constant of that class with the same name as this argument's value
     * @throws CommandInvalidTypeException If the constant couldn't be found
     * @see ArgConsumer#peekEnum(Class)
     * @see ArgConsumer#peekEnum(Class, int)
     * @see ArgConsumer#peekEnumOrNull(Class)
     * @see ArgConsumer#peekEnumOrNull(Class, int)
     * @see ArgConsumer#getEnum(Class)
     * @see ArgConsumer#getEnumOrNull(Class)
     */
    public <E extends Enum<?>> E getEnum(Class<E> enumClass) throws CommandInvalidTypeException {
        return Arrays.stream(enumClass.getEnumConstants())
                .filter(e -> e.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new CommandInvalidTypeException(this, enumClass.getSimpleName()));
    }

    /**
     * Tries to use a <b>stateless</b> {@link ArgParser} to parse this argument into the specified class
     *
     * @param type The class to parse this argument into
     * @return An instance of the specified type
     * @throws CommandInvalidTypeException If the parsing failed
     */
    public <T> T getAs(Class<T> type) throws CommandInvalidTypeException {
        return ArgParserManager.parseStateless(type, this);
    }

    /**
     * Tries to use a <b>stateless</b> {@link ArgParser} to parse this argument into the specified class
     *
     * @param type The class to parse this argument into
     * @return If the parser succeeded
     */
    public <T> boolean is(Class<T> type) {
        try {
            getAs(type);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Tries to use a <b>stated</b> {@link ArgParser} to parse this argument into the specified class
     *
     * @param type The class to parse this argument into
     * @return An instance of the specified type
     * @throws CommandInvalidTypeException     If the parsing failed
     */
    @SuppressWarnings("UnusedReturnValue")
    public <T, S> T getAs(Class<T> type, Class<S> stateType, S state) throws CommandInvalidTypeException {
        return ArgParserManager.parseStated(type, stateType, this, state);
    }

    /**
     * Tries to use a <b>stated</b> {@link ArgParser} to parse this argument into the specified class
     *
     * @param type The class to parse this argument into
     * @return If the parser succeeded
     */
    public <T, S> boolean is(Class<T> type, Class<S> stateType, S state) {
        try {
            getAs(type, stateType, state);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Turn a string into a list of {@link CommandArgument}s. This is needed because of {@link CommandArgument#rawRest}
     *
     * @param string            The string to convert
     * @param preserveEmptyLast If the string ends with whitespace, add an empty {@link CommandArgument} to the end This
     *                          is useful for tab completion
     * @return A list of {@link CommandArgument}s
     */
    public static List<CommandArgument> from(String string, boolean preserveEmptyLast) {
        List<CommandArgument> args = new ArrayList<>();
        Matcher argMatcher = argPattern.matcher(string);
        int lastEnd = -1;
        while (argMatcher.find()) {
            args.add(new CommandArgument(
                    args.size(),
                    argMatcher.group(),
                    string.substring(argMatcher.start())
            ));
            lastEnd = argMatcher.end();
        }
        if (preserveEmptyLast && lastEnd < string.length()) {
            args.add(new CommandArgument(args.size(), "", ""));
        }
        return args;
    }

    /**
     * @see #from(String, boolean)
     */
    public static List<CommandArgument> from(String string) {
        return from(string, false);
    }

    /**
     * Returns an "unknown" {@link CommandArgument}. This shouldn't be used unless you absolutely have no information -
     * ESPECIALLY not with {@link CommandInvalidArgumentException}s
     *
     * @return The unknown {@link CommandArgument}
     */
    public static CommandArgument unknown() {
        return new CommandArgument(-1, "<unknown>", "");
    }
}
