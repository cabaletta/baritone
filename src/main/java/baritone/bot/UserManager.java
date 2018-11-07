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

import baritone.bot.connect.ConnectionResult;
import baritone.bot.handler.BotNetHandlerLoginClient;
import baritone.utils.Helper;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.ServerAddress;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.handshake.client.C00Handshake;
import net.minecraft.network.login.client.CPacketLoginStart;
import net.minecraft.util.Session;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static baritone.bot.connect.ConnectionStatus.*;

/**
 * @author Brady
 * @since 11/6/2018
 */
public final class UserManager implements Helper {

    public static final UserManager INSTANCE = new UserManager();

    private UserManager() {}

    private final List<IBaritoneUser> users = new ArrayList<>();

    /**
     * Connects a new user with the specified {@link Session} to the current server.
     *
     * @param session The user session
     * @return The result of the attempted connection
     */
    public final ConnectionResult connect(Session session) {
        ServerData data = mc.getCurrentServerData();
        if (data == null) {
            return ConnectionResult.failed(NO_CURRENT_CONNECTION);
        }

        // Connect to the server from the parsed server data
        return connect(session, ServerAddress.fromString(data.serverIP));
    }

    /**
     * Connects a new user with the specified {@link Session} to the specified server.
     *
     * @param session The user session
     * @param address The address of the server to connect to
     * @return The result of the attempted connection
     */
    private ConnectionResult connect(Session session, ServerAddress address) {
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
            IBaritoneUser user = new BaritoneUser(this, networkManager, session);
            this.users.add(user);

            // Setup login handler and send connection packets
            networkManager.setNetHandler(new BotNetHandlerLoginClient(networkManager, user));
            networkManager.sendPacket(new C00Handshake(address.getIP(), address.getPort(), EnumConnectionState.LOGIN));
            networkManager.sendPacket(new CPacketLoginStart(session.getProfile()));

            return ConnectionResult.success(user);
        } catch (Exception e) {
            return ConnectionResult.failed(CONNECTION_FAILED);
        }
    }

    public final void notifyDisconnect(IBaritoneUser user, EnumConnectionState state) {

    }

    public final Optional<IBaritoneUser> getUserByProfile(GameProfile profile) {
        return this.users.stream().filter(user -> user.getSession().getProfile().equals(profile)).findFirst();
    }

    public final Optional<IBaritoneUser> getUserByUUID(UUID uuid) {
        return this.users.stream().filter(user -> user.getSession().getProfile().getId().equals(uuid)).findFirst();
    }
}
