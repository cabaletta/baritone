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

import baritone.api.IBaritone;
import baritone.bot.spec.BotPlayerController;
import baritone.bot.spec.BotWorld;
import baritone.bot.spec.EntityBot;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.INetHandler;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.util.Session;

/**
 * @author Brady
 * @since 10/23/2018
 */
public interface IBaritoneUser {

    /**
     * Called when the user successfully logs into a server.
     *
     * @param profile              The game profile returned by the server on login
     * @param netHandlerPlayClient The client play network handler
     */
    void onLoginSuccess(GameProfile profile, INetHandlerPlayClient netHandlerPlayClient);

    /**
     * Called when the user loads into a world.
     *
     * @param world The world object
     * @param player The player object
     * @param playerController The player controller
     */
    void onWorldLoad(BotWorld world, EntityBot player, BotPlayerController playerController);

    /**
     * @return The network manager that is responsible for the current connection.
     */
    NetworkManager getNetworkManager();

    /**
     * Returns the current play network handler. Can also be acquired via
     * {@link NetworkManager#getNetHandler()} from {@link #getNetworkManager()},
     * and checking if the {@link INetHandler} is an instance of {@link INetHandlerPlayClient}.
     *
     * @return The current play network handler
     */
    INetHandlerPlayClient getConnection();

    /**
     * @return The locally managed entity for this user.
     */
    EntityBot getEntity();

    /**
     * @return The bot player controller
     */
    BotPlayerController getPlayerController();

    /**
     * Returns the user login session. Should never be {@code null}, as this should be set when the
     * user is constructed.
     *
     * @return This users's login session
     */
    Session getSession();

    /**
     * Returns the game profile for the account represented by this user.
     *
     * @return This users's profile.
     */
    GameProfile getProfile();

    /**
     * @return The manager that spawned this {@link IBaritoneUser}.
     */
    UserManager getManager();

    IBaritone getBaritone();
}
