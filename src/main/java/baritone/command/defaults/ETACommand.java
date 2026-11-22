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
import baritone.api.behavior.IPathingBehavior;
import baritone.api.command.Command;
import baritone.api.command.argument.IArgConsumer;
import baritone.api.command.exception.CommandException;
import baritone.api.command.exception.CommandInvalidStateException;
import baritone.api.pathing.calc.IPathingControlManager;
import baritone.api.process.IBaritoneProcess;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class ETACommand extends Command {

    // we just assume tps is 20, it isn't worth the effort that is needed to calculate it exactly
    private static final String[] UNITS = {"seconds", "minutes", "hours", "days", "weeks"};
    private static final int[] TICKS_PER_UNIT = {20, 1200, 72000, 1728000, 12096000};

    public ETACommand(IBaritone baritone) {
        super(baritone, "eta");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        args.requireMax(0);
        IPathingControlManager pathingControlManager = baritone.getPathingControlManager();
        IBaritoneProcess process = pathingControlManager.mostRecentInControl().orElse(null);
        if (process == null) {
            throw new CommandInvalidStateException("No process in control");
        }
        IPathingBehavior pathingBehavior = baritone.getPathingBehavior();

        double ticksRemainingInSegment = pathingBehavior.ticksRemainingInSegment().orElse(Double.NaN);
        double ticksRemainingInGoal = pathingBehavior.estimatedTicksToGoal().orElse(Double.NaN);

        logDirect(String.format(
                "Next segment: %s\n" +
                "        Goal: %s",
                formatTime(ticksRemainingInSegment),
                formatTime(ticksRemainingInGoal)
        ));

        if (Double.isFinite(ticksRemainingInGoal) && ticksRemainingInGoal >= 24192000) {
            logDirect("Please don't do this to me.");
        }
    }

    private static String formatTime(double ticks) {
        if (!Double.isFinite(ticks)) {
            return "" + ticks; // NaN, +Infinity and -Infinity don't need units
        }
        for (int i = UNITS.length - 1; i >= 0; i--) {
            int value = (int) ticks / TICKS_PER_UNIT[i];
            if (value >= 2) {
                return String.format("%s %s (%.0f ticks)", value, UNITS[i], ticks);
            }
        }
        return String.format("%.0f ticks", ticks);
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) {
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "View the current ETA";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "The ETA command provides information about the estimated time until the next segment.",
                "and the goal",
                "",
                "Be aware that the ETA to your goal is really unprecise",
                "",
                "Usage:",
                "> eta - View ETA, if present"
        );
    }
}
