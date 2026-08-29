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

package baritone.command.defaults;

import baritone.Baritone;
import baritone.api.IBaritone;
import baritone.api.command.Command;
import baritone.api.command.argument.IArgConsumer;
import baritone.api.command.exception.CommandException;
import baritone.api.command.exception.CommandInvalidStateException;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class ResumeLastCommand extends Command {

    public ResumeLastCommand(IBaritone baritone) {
        super(baritone, "resumelast", "rl");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        args.requireMax(0);
        String lastTaskCommand = ((Baritone) baritone).getResumeBehavior().getLastTaskCommand();
        if (lastTaskCommand == null) {
            throw new CommandInvalidStateException("No task command has been recorded yet");
        }
        logDirect(String.format("Re-running: %s", lastTaskCommand));
        baritone.getCommandManager().execute(lastTaskCommand);
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) {
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Re-runs the last task command";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "The resumelast command re-runs the most recent task command (tunnel, mine, goto, farm, ...)",
                "that Baritone recorded. This is useful to manually continue a task after a reconnect or after",
                "Baritone stopped on its own, for example when it ran into unloaded chunks.",
                "",
                "Note that direction and position dependent commands (like tunnel or thisway) are re-derived",
                "from where you are and which way you are looking when this runs.",
                "",
                "Usage:",
                "> resumelast"
        );
    }
}
