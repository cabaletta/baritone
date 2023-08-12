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

import baritone.api.BaritoneAPI;
import baritone.api.bot.IBaritoneUser;
import baritone.api.bot.IUserManager;
import baritone.api.bot.connect.IConnectionResult;
import baritone.api.event.events.TickEvent;
import baritone.api.event.events.type.EventState;
import baritone.api.event.listener.AbstractGameEventListener;
import baritone.api.utils.Helper;
import baritone.api.utils.IPlayerContext;
import baritone.bot.connect.ConnectionResult;
import baritone.bot.handler.BotNetHandlerLoginClient;
import baritone.bot.impl.BotPlayer;
import baritone.bot.impl.BotWorld;
import baritone.utils.accessor.IIntegratedServer;
import baritone.utils.accessor.IThreadLanServerPing;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerAddress;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.handshake.client.C00Handshake;
import net.minecraft.network.login.client.CPacketLoginStart;
import net.minecraft.util.Session;
import net.minecraft.util.text.ITextComponent;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static baritone.api.bot.connect.ConnectionStatus.*;

/**
 * @author Brady
 * @since 11/6/2018
 */
public enum UserManager implements IUserManager, AbstractGameEventListener {
    INSTANCE;

    private static final Minecraft mc = Minecraft.getMinecraft();

    private final List<IBaritoneUser> users;
    private final BotWorldProvider worldProvider;

    UserManager() {
        BaritoneAPI.getProvider().getPrimaryBaritone().getGameEventHandler().registerEventListener(this);
        this.users = new CopyOnWriteArrayList<>();
        this.worldProvider = new BotWorldProvider();
    }

    @Override
    public void onTick(TickEvent event) {
        if (event.getState() != EventState.PRE) {
            return;
        }

        this.users.forEach(user -> {
            switch (event.getType()) {
                case IN: {
                    final IPlayerContext ctx = user.getPlayerContext();
                    if (ctx.player() != null) {
                        ctx.playerController().syncHeldItem();
                    }
                    if (user.getNetworkManager().isChannelOpen()) {
                        user.getNetworkManager().processReceivedPackets();
                    } else {
                        user.getNetworkManager().handleDisconnection();
                    }
                    break;
                }
                case OUT: {
                    this.disconnect(user, null);
                    break;
                }
            }
        });

        this.worldProvider.tick();
    }

    /**
     * Connects a new user with the specified {@link Session} to the current server.
     *
     * @param session The user session
     * @return The result of the attempted connection
     */
    @Override
    public final IConnectionResult connect(Session session) {
        if (mc.getIntegratedServer() != null && mc.getIntegratedServer().getPublic()) {
            try {
                IIntegratedServer integratedServer = (IIntegratedServer) mc.getIntegratedServer();
                IThreadLanServerPing lanServerPing = (IThreadLanServerPing) integratedServer.getLanServerPing();
                int port = Integer.parseInt(lanServerPing.getAddress());

                return connect0(session, new ServerData(lanServerPing.getMotd(), "localhost:" + port, true));
            } catch (Exception e) {
                e.printStackTrace();
                return ConnectionResult.failed(CANT_RESOLVE_LAN);
            }
        }

        ServerData data = mc.getCurrentServerData();
        if (data == null) {
            return ConnectionResult.failed(NO_CURRENT_CONNECTION);
        }

        // Connect to the server from the parsed server data
        return connect0(session, data);
    }

    /**
     * Connects a new user with the specified {@link Session} to the specified server.
     * <p>
     * Hi Mickey :)
     *
     * @param session The user session
     * @param data The address of the server to connect to
     * @return The result of the attempted connection
     */
    private IConnectionResult connect0(Session session, ServerData data) {
        ServerAddress address = ServerAddress.fromString(data.serverIP);
        InetAddress inetAddress;

        try {
            inetAddress = InetAddress.getByName(address.getIP());
        } catch (UnknownHostException e) {
            return ConnectionResult.failed(CANT_RESOLVE_HOST);
        }

        try {
            // Initialize Connection
            NetworkManager networkManager = NetworkManager.createNetworkManagerAndConnect(
                    inetAddress,
                    address.getPort(),
                    mc.gameSettings.isUsingNativeTransport()
            );

            // Create User
            BaritoneUser user = new BaritoneUser(this, networkManager, session, data);
            this.users.add(user);

            // Setup login handler and send connection packets
            networkManager.setNetHandler(new BotNetHandlerLoginClient(networkManager, user));
            networkManager.sendPacket(new C00Handshake(address.getIP(), address.getPort(), EnumConnectionState.LOGIN));
            networkManager.sendPacket(new CPacketLoginStart(session.getProfile()));

            return ConnectionResult.success(user);
        } catch (Exception e) {
            e.printStackTrace();
            return ConnectionResult.failed(CONNECTION_FAILED);
        }
    }

    /**
     * @return The bot world provider
     */
    public final BotWorldProvider getWorldProvider() {
        return this.worldProvider;
    }

    @Override
    public final void disconnect(IBaritoneUser user, ITextComponent reason) {
        if (this.users.contains(user)) {
            if (user.getNetworkManager().isChannelOpen()) {
                user.getNetworkManager().closeChannel(reason);
            }
            BaritoneAPI.getProvider().destroyBaritone(user.getBaritone());
            this.users.remove(user);
            Helper.HELPER.logDirect(user.getSession().getUsername() + " Disconnected: " +
                    (reason == null ? "Unknown" : reason.getUnformattedText()));

            final IPlayerContext ctx = user.getPlayerContext();
            if (ctx.player() != null && ctx.world() != null) {
                ((BotWorld) ctx.world()).handleWorldRemove((BotPlayer) ctx.player());
            }
        }
    }

    @Override
    public final List<IBaritoneUser> getUsers() {
        return Collections.unmodifiableList(this.users);
    }
}
