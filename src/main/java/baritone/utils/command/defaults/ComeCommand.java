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

import baritone.api.Settings;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.utils.command.Command;
import baritone.api.utils.command.exception.CommandInvalidStateException;
import baritone.api.utils.command.helpers.arguments.ArgConsumer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Objects.isNull;

public class ComeCommand extends Command {
    public ComeCommand() {
        super("come", "Start heading towards your camera");
    }

    @Override
    protected void executed(String label, ArgConsumer args, Settings settings) {
        args.requireMax(0);
        Entity entity = MC.getRenderViewEntity();

        if (isNull(entity)) {
            throw new CommandInvalidStateException("render view entity is null");
        }

        baritone.getCustomGoalProcess().setGoalAndPath(new GoalBlock(new BlockPos(entity)));
        logDirect("Coming");
    }

    @Override
    protected Stream<String> tabCompleted(String label, ArgConsumer args, Settings settings) {
        return Stream.empty();
    }

    @Override
    public List<String> getLongDesc() {
        return asList(
            "The come command tells Baritone to head towards your camera.",
            "",
            "I'm... not actually sure how useful this is, to be honest.",
            "",
            "Usage:",
            "> come"
        );
    }
}
