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

package baritone.utils.command.defaults;

import baritone.api.IBaritone;
import baritone.api.utils.command.Command;
import baritone.api.utils.command.exception.CommandException;
import baritone.api.utils.command.exception.CommandInvalidStateException;
import baritone.api.utils.command.helpers.arguments.ArgConsumer;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class VersionCommand extends Command {

    public VersionCommand(IBaritone baritone) {
        super(baritone, "version");
    }

    @Override
    protected void executed(String label, ArgConsumer args) throws CommandException {
        args.requireMax(0);
        String version = getClass().getPackage().getImplementationVersion();
        if (version == null) {
            throw new CommandInvalidStateException("Null version (this is normal in a dev environment)");
        } else {
            logDirect(String.format("You are running Baritone v%s", version));
        }
    }

    @Override
    protected Stream<String> tabCompleted(String label, ArgConsumer args) {
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "View the Baritone version";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "The version command prints the version of Baritone you're currently running.",
                "",
                "Usage:",
                "> version - View version information, if present"
        );
    }
}
