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
import baritone.bot.impl.BotMinecraft;
import baritone.bot.impl.BotWorld;
import baritone.bot.impl.BotEntity;
import baritone.utils.player.WrappedPlayerController;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.NetworkManager;
import net.minecraft.util.Session;

/**
 * Implementation of {@link IBaritoneUser}
 *
 * @author Brady
 * @since 11/6/2018
 */
public final class BaritoneUser implements IBaritoneUser {

    private final BotMinecraft mc;
    private final UserManager manager;
    private final NetworkManager networkManager;
    private final Session session;
    private final Baritone baritone;

    private GameProfile profile;
    private NetHandlerPlayClient netHandlerPlayClient;
    private BotWorld world;
    private BotEntity player;
    private IPlayerController playerController;

    BaritoneUser(UserManager manager, NetworkManager networkManager, Session session, ServerData serverData) {
        this.mc = BotMinecraft.allocate(this);
        this.mc.setServerData(serverData);
        this.manager = manager;
        this.networkManager = networkManager;
        this.session = session;
        this.profile = session.getProfile();
        this.baritone = new Baritone(new BotPlayerContext(this));
    }

    public void onLoginSuccess(GameProfile profile, NetHandlerPlayClient netHandlerPlayClient) {
        this.profile = profile;
        this.netHandlerPlayClient = netHandlerPlayClient;
    }

    public void onWorldLoad(BotWorld world, BotEntity player, PlayerControllerMP controller) {
        this.baritone.getGameEventHandler().onWorldEvent(new WorldEvent(world, EventState.PRE));

        this.mc.player = this.player = player;
        this.mc.world = this.world = world;
        this.mc.playerController = controller;
        this.playerController = new WrappedPlayerController(controller);

        this.baritone.getGameEventHandler().onWorldEvent(new WorldEvent(world, EventState.POST));
    }

    @Override
    public NetworkManager getNetworkManager() {
        return this.networkManager;
    }

    @Override
    public NetHandlerPlayClient getConnection() {
        return this.netHandlerPlayClient;
    }

    @Override
    public BotEntity getPlayer() {
        return this.player;
    }

    @Override
    public WorldClient getWorld() {
        return this.world;
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
        return this.baritone;
    }

    public BotMinecraft getMinecraft() {
        return this.mc;
    }
}
