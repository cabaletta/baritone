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

package baritone.api.utils.command.datatypes;

import baritone.api.BaritoneAPI;
import baritone.api.utils.command.exception.CommandException;
import baritone.api.utils.command.helpers.arguments.ArgConsumer;
import baritone.api.utils.command.helpers.tabcomplete.TabCompleteHelper;
import net.minecraft.entity.player.EntityPlayer;

import java.util.List;
import java.util.stream.Stream;

public class PlayerByUsername implements IDatatypeFor<EntityPlayer> {

    private final List<EntityPlayer> players =
            BaritoneAPI.getProvider().getPrimaryBaritone().getPlayerContext().world().playerEntities;
    public final EntityPlayer player;

    public PlayerByUsername() {
        player = null;
    }

    public PlayerByUsername(ArgConsumer consumer) throws CommandException {
        String username = consumer.getString();
        player = players
                .stream()
                .filter(s -> s.getName().equalsIgnoreCase(username))
                .findFirst()
                .orElse(null);
        if (player == null) {
            throw new IllegalArgumentException("no player found by that username");
        }
    }

    @Override
    public EntityPlayer get() {
        return player;
    }

    @Override
    public Stream<String> tabComplete(ArgConsumer consumer) throws CommandException {
        return new TabCompleteHelper()
                .append(
                        players
                                .stream()
                                .map(EntityPlayer::getName)
                )
                .filterPrefix(consumer.getString())
                .sortAlphabetically()
                .stream();
    }
}
