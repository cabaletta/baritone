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

package baritone.bot.handler;

import baritone.bot.BaritoneUser;
import baritone.bot.impl.BotMinecraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerLoginClient;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.login.server.SPacketLoginSuccess;
import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nonnull;

/**
 * Handles the login stage when connecting to a server.
 *
 * @author Brady
 * @since 10/29/2018
 */
public final class BotNetHandlerLoginClient extends NetHandlerLoginClient {

    /**
     * The {@link NetworkManager} that is managing the connection with the server.
     */
    private final NetworkManager networkManager;

    /**
     * The {@link Minecraft} game instance
     */
    private final BotMinecraft mc;

    /**
     * The bot of this connection
     */
    private final BaritoneUser user;

    public BotNetHandlerLoginClient(NetworkManager networkManager, BaritoneUser user) {
        super(networkManager, user.getPlayerContext().minecraft(), null);
        this.networkManager = networkManager;
        this.mc = (BotMinecraft) user.getPlayerContext().minecraft();
        this.user = user;
    }

    @Override
    public void handleLoginSuccess(SPacketLoginSuccess packetIn) {
        this.networkManager.setConnectionState(EnumConnectionState.PLAY);
        this.networkManager.setNetHandler(new BotNetHandlerPlayClient(this.networkManager, this.user, this.mc, packetIn.getProfile()));
    }

    @Override
    public void onDisconnect(@Nonnull ITextComponent reason) {
        // It's important that we don't call the superclass method because that would mess up GUIs and make us upset
        this.user.getManager().disconnect(this.user, reason);
    }
}
