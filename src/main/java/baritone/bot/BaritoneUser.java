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
import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.bot.IBaritoneUser;
import baritone.api.event.events.WorldEvent;
import baritone.api.event.events.type.EventState;
import baritone.api.utils.IPlayerContext;
import baritone.bot.impl.BotMinecraft;
import baritone.bot.impl.BotWorld;
import baritone.bot.impl.BotPlayer;
import baritone.command.ExampleBaritoneControl;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.multiplayer.ServerData;
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

    BaritoneUser(UserManager manager, NetworkManager networkManager, Session session, ServerData serverData) {
        this.mc = BotMinecraft.allocate(this);
        this.mc.setServerData(serverData);
        this.manager = manager;
        this.networkManager = networkManager;
        this.session = session;
        this.profile = session.getProfile();
        this.baritone = (Baritone) BaritoneAPI.getProvider().createBaritone(this.mc);
        this.baritone.registerBehavior(ExampleBaritoneControl::new);
    }

    public void onLoginSuccess(GameProfile profile) {
        this.profile = profile;
    }

    public void onWorldLoad(BotWorld world, BotPlayer player, PlayerControllerMP playerController) {
        this.baritone.getGameEventHandler().onWorldEvent(new WorldEvent(world, EventState.PRE));

        this.mc.player = player;
        this.mc.world = world;
        this.mc.playerController = playerController;

        this.baritone.getGameEventHandler().onWorldEvent(new WorldEvent(world, EventState.POST));
    }

    @Override
    public IBaritone getBaritone() {
        return this.baritone;
    }

    @Override
    public IPlayerContext getPlayerContext() {
        return this.baritone.getPlayerContext();
    }

    @Override
    public NetworkManager getNetworkManager() {
        return this.networkManager;
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
}
