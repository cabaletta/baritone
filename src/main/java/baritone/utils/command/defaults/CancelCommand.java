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
import baritone.api.Settings;
import baritone.api.utils.command.Command;
import baritone.api.utils.command.helpers.arguments.ArgConsumer;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class CancelCommand extends Command {

    public CancelCommand(IBaritone baritone) {
        super(baritone, Arrays.asList("cancel", "stop"));
    }

    @Override
    protected void executed(String label, ArgConsumer args, Settings settings) {
        args.requireMax(0);
        baritone.getPathingBehavior().cancelEverything();
        logDirect("ok canceled");
    }

    @Override
    protected Stream<String> tabCompleted(String label, ArgConsumer args, Settings settings) {
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Cancel what Baritone is currently doing";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "The cancel command tells Baritons to stop whatever it's currently doing.",
                "",
                "Usage:",
                "> cancel"
        );
    }
}
