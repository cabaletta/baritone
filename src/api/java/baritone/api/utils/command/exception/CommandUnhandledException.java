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

package baritone.api.utils.command.exception;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

public class CommandUnhandledException extends CommandErrorMessageException {
    public static String getStackTrace(Throwable throwable) {
        StringWriter sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    public static String getBaritoneStackTrace(String stackTrace) {
        List<String> lines = Arrays.stream(stackTrace.split("\n"))
            .collect(Collectors.toList());

        int lastBaritoneLine = 0;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith("\tat baritone.") && lines.get(i).contains("BaritoneChatControl")) {
                lastBaritoneLine = i;
            }
        }

        return String.join("\n", lines.subList(0, lastBaritoneLine + 1));
    }

    public static String getBaritoneStackTrace(Throwable throwable) {
        return getBaritoneStackTrace(getStackTrace(throwable));
    }

    public static String getFriendlierStackTrace(String stackTrace) {
        List<String> lines = asList(stackTrace.split("\n"));

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.startsWith("\tat ")) {
                if (line.startsWith("\tat baritone.")) {
                    line = line.replaceFirst("^\tat [a-z.]+?([A-Z])", "\tat $1");
                }

                // line = line.replaceFirst("\\(([^)]+)\\)$", "\n\t  . $1");
                line = line.replaceFirst("\\([^:]+:(\\d+)\\)$", ":$1");
                line = line.replaceFirst("\\(Unknown Source\\)$", "");
                lines.set(i, line);
            }
        }

        return String.join("\n", lines);
    }

    public static String getFriendlierStackTrace(Throwable throwable) {
        return getFriendlierStackTrace(getBaritoneStackTrace(throwable));
    }

    public CommandUnhandledException(Throwable cause) {
        super(String.format(
            "An unhandled exception has occurred:\n\n%s",
            getFriendlierStackTrace(cause)
        ));
    }
}
