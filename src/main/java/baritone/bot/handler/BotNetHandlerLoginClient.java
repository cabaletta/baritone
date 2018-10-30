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

import baritone.bot.IBaritoneUser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.exceptions.InvalidCredentialsException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerLoginClient;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.login.client.CPacketEncryptionResponse;
import net.minecraft.network.login.server.SPacketEncryptionRequest;
import net.minecraft.network.login.server.SPacketLoginSuccess;
import net.minecraft.util.CryptManager;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

import javax.annotation.Nonnull;
import javax.crypto.SecretKey;
import java.math.BigInteger;
import java.security.PublicKey;

/**
 * Handles the login stage when connecting to a server.
 *
 * @author Brady
 * @since 10/29/2018
 */
public class BotNetHandlerLoginClient extends NetHandlerLoginClient {

    /**
     * The {@link NetworkManager} that is managing the connection with the server.
     */
    private final NetworkManager networkManager;

    /**
     * The {@link Minecraft} game instance
     */
    private final Minecraft mc;

    /**
     * The bot of this connection
     */
    private final IBaritoneUser user;

    public BotNetHandlerLoginClient(NetworkManager networkManager, IBaritoneUser user) {
        super(networkManager, Minecraft.getMinecraft(), null);
        this.networkManager = networkManager;
        this.mc = Minecraft.getMinecraft();
        this.user = user;
    }

    @Override
    public void handleEncryptionRequest(SPacketEncryptionRequest packetIn) {
        SecretKey secretkey = CryptManager.createNewSharedKey();
        PublicKey publicKey = packetIn.getPublicKey();

        // Setup joinServer payload info
        GameProfile profile = this.user.getSession().getProfile();
        String authenticationToken = this.user.getSession().getToken();
        String serverId = new BigInteger(CryptManager.getServerIdHash(packetIn.getServerId(), publicKey, secretkey)).toString(16);

        if (this.mc.getCurrentServerData() != null && this.mc.getCurrentServerData().isOnLAN()) {
            try {
                this.mc.getSessionService().joinServer(profile, authenticationToken, serverId);
            } catch (AuthenticationException e) {
                // Couldn't connect to auth servers but will continue to join LAN
            }
        } else {
            try {
                this.mc.getSessionService().joinServer(profile, authenticationToken, serverId);
            } catch (AuthenticationUnavailableException e) {
                this.networkManager.closeChannel(new TextComponentTranslation("disconnect.loginFailedInfo", new TextComponentTranslation("disconnect.loginFailedInfo.serversUnavailable")));
                return;
            } catch (InvalidCredentialsException e) {
                this.networkManager.closeChannel(new TextComponentTranslation("disconnect.loginFailedInfo", new TextComponentTranslation("disconnect.loginFailedInfo.invalidSession")));
                return;
            } catch (AuthenticationException e) {
                this.networkManager.closeChannel(new TextComponentTranslation("disconnect.loginFailedInfo", e.getMessage()));
                return;
            }
        }

        // noinspection unchecked
        this.networkManager.sendPacket(new CPacketEncryptionResponse(secretkey, publicKey, packetIn.getVerifyToken()),
                future -> BotNetHandlerLoginClient.this.networkManager.enableEncryption(secretkey));
    }

    @Override
    public void handleLoginSuccess(SPacketLoginSuccess packetIn) {
        this.networkManager.setConnectionState(EnumConnectionState.PLAY);
        this.networkManager.setNetHandler(new BotNetHandlerPlayClient(this.networkManager, this.user, Minecraft.getMinecraft(), packetIn.getProfile()));
    }

    @Override
    public void onDisconnect(@Nonnull ITextComponent reason) {
        // TODO Notify the bot manager that we are no longer connected
        // It's important that we don't call the superclass method because that would mess up GUIs and make us upset
    }
}
