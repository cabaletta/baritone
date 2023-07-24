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
import baritone.api.command.helpers.TabCompleteHelper;
import baritone.api.pathing.goals.Goal;
import baritone.api.process.ICustomGoalProcess;
import baritone.api.process.IElytraProcess;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class ElytraCommand extends Command {

    public ElytraCommand(IBaritone baritone) {
        super(baritone, "elytra");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        final ICustomGoalProcess customGoalProcess = baritone.getCustomGoalProcess();
        final IElytraProcess elytra = baritone.getElytraProcess();
        if (args.hasExactlyOne() && args.peekString().equals("supported")) {
            logDirect(elytra.isLoaded() ? "yes" : unsupportedSystemMessage());
            return;
        }
        if (!elytra.isLoaded()) {
            throw new CommandInvalidStateException(unsupportedSystemMessage());
        }

        if (!args.hasAny()) {
            Goal iGoal = customGoalProcess.mostRecentGoal();
            if (iGoal == null) {
                throw new CommandInvalidStateException("No goal has been set");
            }
            if (ctx.player().dimension != -1) {
                throw new CommandInvalidStateException("Only works in the nether");
            }
            try {
                elytra.pathTo(iGoal);
            } catch (IllegalArgumentException ex) {
                throw new CommandInvalidStateException(ex.getMessage());
            }
            return;
        }

        final String action = args.getString();
        switch (action) {
            case "reset": {
                elytra.resetState();
                logDirect("Reset state but still flying to same goal");
                break;
            }
            case "repack": {
                elytra.repackChunks();
                logDirect("Queued all loaded chunks for repacking");
                break;
            }
            default: {
                throw new CommandInvalidStateException("Invalid action");
            }
        }
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        TabCompleteHelper helper = new TabCompleteHelper();
        if (args.hasExactlyOne()) {
            helper.append("reset", "repack", "supported");
        }
        return helper.filterPrefix(args.getString()).stream();
    }

    @Override
    public String getShortDesc() {
        return "elytra time";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "The elytra command tells baritone to, in the nether, automatically fly to the current goal.",
                "",
                "Usage:",
                "> elytra - fly to the current goal",
                "> elytra reset - Resets the state of the process, but will try to keep flying to the same goal.",
                "> elytra repack - Queues all of the chunks in render distance to be given to the native library.",
                "> elytra supported - Tells you if baritone ships a native library that is compatible with your PC."
        );
    }

    private static String unsupportedSystemMessage() {
        final String osArch = System.getProperty("os.arch");
        final String osName = System.getProperty("os.name");
        return String.format(
                "Legacy architectures are not supported. your CPU is %s and your operating system is %s. " +
                        "Supported architectures are 64 bit x86, and 64 bit arm, supported operating systems are Windows, " +
                        "Linux, and Mac",
                osArch, osName
        );
    }
}
