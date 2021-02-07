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

import baritone.api.command.argument.ICommandArgument;
import baritone.api.command.exception.CommandInvalidArgumentException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author LoganDark
 */
public final class CommandArguments {

    private CommandArguments() {}

    private static final Pattern ARG_PATTERN = Pattern.compile("\\S+");

    /**
     * Turn a string into a list of {@link ICommandArgument}s. This is needed because of {@link ICommandArgument#getRawRest()}
     *
     * @param string            The string to convert
     * @param preserveEmptyLast If the string ends with whitespace, add an empty {@link ICommandArgument} to the end This
     *                          is useful for tab completion
     * @return A list of {@link ICommandArgument}s
     */
    public static List<ICommandArgument> from(String string, boolean preserveEmptyLast) {
        List<ICommandArgument> args = new ArrayList<>();
        Matcher argMatcher = ARG_PATTERN.matcher(string);
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
    public static List<ICommandArgument> from(String string) {
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
