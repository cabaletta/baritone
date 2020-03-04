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
import baritone.api.event.events.WorldEvent;
import baritone.api.event.events.type.EventState;
import baritone.api.utils.IPlayerController;
import baritone.bot.spec.BotMinecraft;
import baritone.bot.spec.BotWorld;
import baritone.bot.spec.EntityBot;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.util.Session;

/**
 * Implementation of {@link IBaritoneUser}
 *
 * @author Brady
 * @since 11/6/2018
 */
public final class BaritoneUser implements IBaritoneUser {

    private final UserManager manager;
    private final NetworkManager networkManager;
    private final Session session;

    private GameProfile profile;
    private INetHandlerPlayClient netHandlerPlayClient;

    private BotMinecraft mc;
    private BotWorld world;
    private EntityBot player;
    private IPlayerController playerController;

    private final Baritone baritone;

    BaritoneUser(UserManager manager, NetworkManager networkManager, Session session, ServerData serverData) {
        this.mc = BotMinecraft.allocate(this);
        this.mc.setServerData(serverData);
        this.manager = manager;
        this.networkManager = networkManager;
        this.session = session;
        this.baritone = new Baritone(new BotPlayerContext(this));
    }

    public void onLoginSuccess(GameProfile profile, INetHandlerPlayClient netHandlerPlayClient) {
        this.profile = profile;
        this.netHandlerPlayClient = netHandlerPlayClient;
    }

    public void onWorldLoad(BotWorld world, EntityBot player, IPlayerController playerController) {
        this.baritone.getGameEventHandler().onWorldEvent(new WorldEvent(world, EventState.PRE));

        this.mc.player = this.player = player;
        this.player.world = this.world = world;
        this.playerController = playerController;

        this.baritone.getGameEventHandler().onWorldEvent(new WorldEvent(world, EventState.POST));
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

    public BotMinecraft getMinecraft() {
        return this.mc;
    }
}
