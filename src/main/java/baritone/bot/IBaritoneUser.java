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

package baritone.bot;

import baritone.bot.entity.EntityBot;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.network.NetworkManager;
import net.minecraft.util.Session;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * @author Brady
 * @since 10/23/2018
 */
public interface IBaritoneUser {

    /**
     * @return The network manager that is responsible for the current connection.
     */
    NetworkManager getNetworkManager();

    /**
     * @return The locally managed entity for this bot.
     */
    EntityBot getLocalEntity();

    /**
     * Returns the remote entity reported by the server that represents this bot connection. This is only
     * provided when this bot goes into the range of another connection that is being managed.
     *
     * @return The remote entity for this bot
     */
    EntityOtherPlayerMP getRemoteEntity();

    /**
     * Returns the user login session. Should never be {@code null}, as this should be set when the
     * user is constructed.
     *
     * @return The bot's login session
     */
    Session getSession();
}
