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

import baritone.Baritone;
import baritone.api.IBaritone;
import baritone.api.bot.IBaritoneUser;
import baritone.api.bot.IUserManager;
import baritone.api.utils.IPlayerController;
import baritone.bot.spec.BotWorld;
import baritone.bot.spec.EntityBot;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.util.Session;

/**
 * Implementation of {@link IBaritoneUser}
 *
 * @author Brady
 * @since 11/6/2018
 */
public class BaritoneUser implements IBaritoneUser {

    private final UserManager manager;
    private final NetworkManager networkManager;
    private final Session session;

    private GameProfile profile;
    private INetHandlerPlayClient netHandlerPlayClient;

    private BotWorld world;
    private EntityBot player;
    private IPlayerController playerController;

    private final Baritone baritone;

    BaritoneUser(UserManager manager, NetworkManager networkManager, Session session) {
        this.manager = manager;
        this.networkManager = networkManager;
        this.session = session;
        this.baritone = new Baritone(new BotPlayerContext(this)); // OPPA GANGNAM STYLE
        this.baritone.init(); // actually massive iq
    }

    public void onLoginSuccess(GameProfile profile, INetHandlerPlayClient netHandlerPlayClient) {
        this.profile = profile;
        this.netHandlerPlayClient = netHandlerPlayClient;
    }

    public void onWorldLoad(BotWorld world, EntityBot player, IPlayerController playerController) {
        this.world = world;
        this.player = player;
        this.playerController = playerController;
    }

    @Override
    public NetworkManager getNetworkManager() {
        return this.networkManager;
    }

    @Override
    public INetHandlerPlayClient getConnection() {
        return this.netHandlerPlayClient;
    }

    @Override
    public EntityBot getEntity() {
        return this.player;
    }

    @Override
    public IPlayerController getPlayerController() {
        return this.playerController;
    }

    @Override
    public Session getSession() {
        return this.session;
    }

    @Override
    public GameProfile getProfile() {
        return this.profile;
    }

    @Override
    public UserManager getManager() {
        return this.manager;
    }

    @Override
    public IBaritone getBaritone() {
        return baritone;
    }
}
