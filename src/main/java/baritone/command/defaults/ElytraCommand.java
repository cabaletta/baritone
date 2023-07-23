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
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.pathing.goals.GoalXZ;
import baritone.api.process.ICustomGoalProcess;
import baritone.api.process.IElytraProcess;
import net.minecraft.util.math.BlockPos;

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
        if (!elytra.isLoaded()) {
            final String osArch = System.getProperty("os.arch");
            final String osName = System.getProperty("os.name");
            throw new CommandInvalidStateException(String.format(
                    "legacy architectures are not supported. your CPU is %s and your operating system is %s. " +
                            "supported architectures are x86_64 or arm64, supported operating systems are windows, " +
                            "linux, and mac",
                    osArch, osName
            ));
        }

        if (!args.hasAny()) {
            Goal iGoal = customGoalProcess.mostRecentGoal();
            if (iGoal == null) {
                throw new CommandInvalidStateException("No goal has been set");
            }
            final int x;
            final int y;
            final int z;
            if (iGoal instanceof GoalXZ) {
                GoalXZ goal = (GoalXZ) iGoal;
                x = goal.getX();
                y = 64;
                z = goal.getZ();
            } else if (iGoal instanceof GoalBlock) {
                GoalBlock goal = (GoalBlock) iGoal;
                x = goal.x;
                y = goal.y;
                z = goal.z;
            } else {
                throw new CommandInvalidStateException("The goal must be a GoalXZ or GoalBlock");
            }
            if (y <= 0 || y >= 128) {
                throw new CommandInvalidStateException("The y of the goal is not between 0 and 128");
            }
            elytra.pathTo(new BlockPos(x, y, z));
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
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "elytra time";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList();
    }
}
