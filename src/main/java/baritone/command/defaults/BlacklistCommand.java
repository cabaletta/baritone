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

import baritone.api.IBaritone;
import baritone.api.command.Command;
import baritone.api.command.argument.IArgConsumer;
import baritone.api.command.exception.CommandException;
import baritone.api.command.exception.CommandInvalidStateException;
import baritone.api.process.IGetToBlockProcess;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class BlacklistCommand extends Command {

    public BlacklistCommand(IBaritone baritone) {
        super(baritone, "blacklist");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        args.requireMax(0);
        IGetToBlockProcess proc = baritone.getGetToBlockProcess();
        if (!proc.isActive()) {
            throw new CommandInvalidStateException("GetToBlockProcess is not currently active");
        }
        if (proc.blacklistClosest()) {
            logDirect("Blacklisted closest instances");
        } else {
            throw new CommandInvalidStateException("No known locations, unable to blacklist");
        }
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) {
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Blacklist closest block";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "While going to a block this command blacklists the closest block so that Baritone won't attempt to get to it.",
                "",
                "Usage:",
                "> blacklist"
        );
    }
}
