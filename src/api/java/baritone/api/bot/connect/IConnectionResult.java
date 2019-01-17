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

package baritone.api.bot.connect;

import baritone.api.bot.IBaritoneUser;

import java.util.Optional;

/**
 * @author Brady
 * @since 1/17/2019
 */
public interface IConnectionResult {

    /**
     * @return The actual status of the connection attempt.
     * @see ConnectionStatus
     */
    ConnectionStatus getStatus();

    /**
     * Returns the user that was created in this connection this result reflects, if
     * {@link #getStatus()} is {@link ConnectionStatus#SUCCESS}, otherwise it will
     * return {@link Optional#empty()}.
     *
     * @return The user created in the connection
     */
    Optional<IBaritoneUser> getUser();
}
