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
import baritone.api.cache.IWaypoint;
import baritone.api.command.Command;
import baritone.api.command.argument.IArgConsumer;
import baritone.api.command.datatypes.ForWaypoints;
import baritone.api.command.exception.CommandException;
import baritone.api.command.exception.CommandInvalidStateException;
import baritone.api.utils.BetterBlockPos;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class FarmCommand extends Command {

    public FarmCommand(IBaritone baritone) {
        super(baritone, "farm");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        args.requireMax(2);
        int range = 0;
        BetterBlockPos origin = null;
        //range
        if (args.has(1)) {
            range = args.getAs(Integer.class);
        }
        //waypoint
        if (args.has(1)) {
            IWaypoint[] waypoints = args.getDatatypeFor(ForWaypoints.INSTANCE);
            IWaypoint waypoint = null;
            switch (waypoints.length) {
                case 0:
                    throw new CommandInvalidStateException("No waypoints found");
                case 1:
                    waypoint = waypoints[0];
                    break;
                default:
                    throw new CommandInvalidStateException("Multiple waypoints were found");
            }
            origin = waypoint.getLocation();
        }

        baritone.getFarmProcess().farm(range, origin);
        logDirect("Farming");
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) {
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Farm nearby crops";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "The farm command starts farming nearby plants. It harvests mature crops and plants new ones.",
                "",
                "Usage:",
                "> farm - farms every crop it can find.",
                "> farm <range> - farm crops within range from the starting position.",
                "> farm <range> <waypoint> - farm crops within range from waypoint."
        );
    }
}
