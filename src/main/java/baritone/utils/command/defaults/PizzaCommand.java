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
import baritone.api.accessor.IGuiScreen;
import baritone.api.utils.command.Command;
import baritone.api.utils.command.exception.CommandException;
import baritone.api.utils.command.helpers.arguments.ArgConsumer;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class PizzaCommand extends Command {

    public PizzaCommand(IBaritone baritone) {
        super(baritone, "orderpizza");
    }

    @Override
    protected void executed(String label, ArgConsumer args) throws CommandException {
        try {
            ((IGuiScreen) mc.currentScreen).openLink(new URI("https://www.dominos.com/en/pages/order/"));
        } catch (NullPointerException | URISyntaxException ignored) {}
    }


    @Override
    protected Stream<String> tabCompleted(String label, ArgConsumer args) {
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Order Pizza";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Order Pizza"
        );
    }
}
