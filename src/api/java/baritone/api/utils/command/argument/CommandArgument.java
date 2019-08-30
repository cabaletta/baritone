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

import baritone.api.utils.command.argparser.ArgParserManager;
import baritone.api.utils.command.exception.CommandInvalidTypeException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("UnusedReturnValue")
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

    public <E extends Enum<?>> E getE(Class<E> enumClass) {
        //noinspection OptionalGetWithoutIsPresent
        return Arrays.stream(enumClass.getEnumConstants())
            .filter(e -> e.name().equalsIgnoreCase(value))
            .findFirst()
            .get();
    }

    public <T> T getAs(Class<T> type) {
        return ArgParserManager.parseStateless(type, this);
    }

    public <T> boolean is(Class<T> type) {
        try {
            getAs(type);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public <T, S> T getAs(Class<T> type, Class<S> stateType, S state) {
        return ArgParserManager.parseStated(type, stateType, this, state);
    }

    public <T, S> boolean is(Class<T> type, Class<S> stateType, S state) {
        try {
            getAs(type, stateType, state);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

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

    public static List<CommandArgument> from(String string) {
        return from(string, false);
    }

    public static CommandArgument unknown() {
        return new CommandArgument(-1, "<unknown>", "");
    }
}
