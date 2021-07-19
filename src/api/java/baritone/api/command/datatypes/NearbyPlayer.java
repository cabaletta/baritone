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

package baritone.api.command.datatypes;

import baritone.api.IBaritone;
import baritone.api.command.exception.CommandException;
import baritone.api.command.helpers.TabCompleteHelper;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * An {@link IDatatype} used to resolve nearby players, those within
 * render distance of the target {@link IBaritone} instance.
 */
public enum NearbyPlayer implements IDatatypeFor<Player> {
    INSTANCE;

    @Override
    public Player get(IDatatypeContext ctx) throws CommandException {
        final String username = ctx.getConsumer().getString();
        return getPlayers(ctx).stream()
                .filter(s -> s.getName().getString().equalsIgnoreCase(username))
                .findFirst().orElse(null);
    }

    @Override
    public Stream<String> tabComplete(IDatatypeContext ctx) throws CommandException {
        return new TabCompleteHelper()
                .append(getPlayers(ctx).stream().map(Player::getName).map(Component::getString))
                .filterPrefix(ctx.getConsumer().getString())
                .sortAlphabetically()
                .stream();
    }

    private static List<? extends Player> getPlayers(IDatatypeContext ctx) {
        return ctx.getBaritone().getPlayerContext().world().players();
    }
}
