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

import baritone.api.bot.connect.IConnectionResult;
import com.mojang.authlib.GameProfile;
import net.minecraft.util.Session;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Brady
 * @since 1/17/2019
 */
public interface IUserManager {

    /**
     * Connects a new user with the specified {@link Session} to the current server.
     *
     * @param session The user session
     * @return The result of the attempted connection
     */
    IConnectionResult connect(Session session);

    /**
     * Disconnects the specified {@link IBaritoneUser} from its current server.
     *
     * @param user The user to disconnect
     */
    void disconnect(IBaritoneUser user);

    /**
     * Finds the {@link IBaritoneUser} associated with the specified {@link GameProfile}
     *
     * @param profile The game profile of the user
     * @return The user, {@link Optional#empty()} if no match or {@code profile} is {@code null}
     */
    default Optional<IBaritoneUser> getUserByProfile(GameProfile profile) {
        return profile == null ? Optional.empty() : users().stream().filter(user -> user.getProfile().equals(profile)).findFirst();
    }

    /**
     * Finds the {@link IBaritoneUser} associated with the specified {@link UUID}
     *
     * @param uuid The uuid of the user
     * @return The user, {@link Optional#empty()} if no match or {@code uuid} is {@code null}
     */
    default Optional<IBaritoneUser> getUserByUUID(UUID uuid) {
        return uuid == null ? Optional.empty() : users().stream().filter(user -> user.getProfile().getId().equals(uuid)).findFirst();
    }

    /**
     * @return All of the users held by this manager
     */
    List<IBaritoneUser> users();
}
