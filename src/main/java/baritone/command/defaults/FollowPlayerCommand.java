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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * {@code #followplayer <playerName> [range]}
 *
 * <p>Activates {@link baritone.process.FollowPlayerProcess} to track a named
 * player using {@link baritone.api.pathing.goals.GoalFollow}.  The path is
 * revalidated every tick so Baritone adapts as the target moves.
 *
 * <p>Stop with {@code #cancel} or the configured cancel hotkey.
 */
public class FollowPlayerCommand extends Command {

    public FollowPlayerCommand(IBaritone baritone) {
        super(baritone, "followplayer");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        args.requireMin(1);
        String playerName = args.getString();
        int    range      = args.hasAny() ? args.getAs(Integer.class) : 3;

        // FollowPlayerProcess is registered by Baritone's constructor.
        // Cast is safe: Baritone is the only IBaritone implementation.
        ((Baritone) baritone).getFollowPlayerProcess().followPlayer(playerName, range);
        logDirect("Following player \"" + playerName + "\" (range " + range + " blocks).");
        logDirect("Use #cancel or the cancel hotkey (default K) to stop.");
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) {
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Follow a named player using GoalFollow";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Tracks a named player dynamically using GoalFollow + FollowPlayerProcess.",
                "The path is revalidated every tick as the target moves.",
                "",
                "Usage:",
                "> followplayer <name>         - follow with default 3-block approach range",
                "> followplayer <name> <range> - follow with custom approach range"
        );
    }
}
