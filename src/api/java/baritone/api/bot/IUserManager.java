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

package baritone.api.bot;

import baritone.api.bot.connect.ConnectionStatus;
import baritone.api.bot.connect.IConnectionResult;
import baritone.api.event.events.TickEvent;
import com.mojang.authlib.GameProfile;
import net.minecraft.util.Session;
import net.minecraft.util.text.ITextComponent;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Brady
 * @since 1/17/2019
 */
public interface IUserManager {

    /**
     * Connects a new user with the specified {@link Session} to the current server. Returns
     * a {@link IConnectionResult} describing the result of the attempted connection as well
     * as a {@link IBaritoneUser} instance if it was {@link ConnectionStatus#SUCCESS}.
     *
     * @param session The user session
     * @return The result of the attempted connection
     */
    IConnectionResult connect(Session session);

    /**
     * Disconnects the specified {@link IBaritoneUser} from its current server. All valid users
     * are automatically disconnected when the current game state becomes {@link TickEvent.Type#OUT}.
     * A reason may be specified, but is more widely used in server-initiated disconnects.
     *
     * @param user The user to disconnect
     * @param reason The reason for the disconnect, may be {@code null}
     */
    void disconnect(IBaritoneUser user, ITextComponent reason);

    /**
     * Finds the {@link IBaritoneUser} associated with the specified {@link GameProfile}
     *
     * @param profile The game profile of the user
     * @return The user, {@link Optional#empty()} if no match or {@code profile} is {@code null}
     */
    default Optional<IBaritoneUser> getUserByProfile(GameProfile profile) {
        return profile == null
            ? Optional.empty()
            : this.getUsers().stream().filter(user -> user.getProfile().equals(profile)).findFirst();
    }

    /**
     * Finds the {@link IBaritoneUser} associated with the specified {@link UUID}
     *
     * @param uuid The uuid of the user
     * @return The user, {@link Optional#empty()} if no match or {@code uuid} is {@code null}
     */
    default Optional<IBaritoneUser> getUserByUUID(UUID uuid) {
        return uuid == null
                ? Optional.empty()
                : this.getUsers().stream().filter(user -> user.getProfile().getId().equals(uuid)).findFirst();
    }

    /**
     * Finds the {@link IBaritoneUser} associated with the specified username
     *
     * @param username The username of the user
     * @return The user, {@link Optional#empty()} if no match or {@code uuid} is {@code null}
     */
    default Optional<IBaritoneUser> getUserByName(String username) {
        return username == null || username.isEmpty()
                ? Optional.empty()
                : this.getUsers().stream().filter(user -> user.getProfile().getName().equalsIgnoreCase(username)).findFirst();
    }

    /**
     * @return All of the users held by this manager
     */
    List<IBaritoneUser> getUsers();
}
