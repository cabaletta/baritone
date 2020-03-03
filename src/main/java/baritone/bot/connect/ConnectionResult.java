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

package baritone.bot.connect;

import baritone.api.bot.IBaritoneUser;
import baritone.api.bot.connect.ConnectionStatus;
import baritone.api.bot.connect.IConnectionResult;

import java.util.Objects;
import java.util.Optional;

import static baritone.api.bot.connect.ConnectionStatus.SUCCESS;

/**
 * @author Brady
 * @since 11/6/2018
 */
public final class ConnectionResult implements IConnectionResult {

    /**
     * The result status
     */
    private final ConnectionStatus status;

    /**
     * The user created, if the status is {@link ConnectionStatus#SUCCESS}
     */
    private final IBaritoneUser user;

    private ConnectionResult(ConnectionStatus status, IBaritoneUser user) {
        this.status = status;
        this.user = user;
    }

    @Override
    public ConnectionStatus getStatus() {
        return this.status;
    }

    @Override
    public Optional<IBaritoneUser> getUser() {
        return Optional.ofNullable(user);
    }

    /**
     * Creates a new failed {@link ConnectionResult}.
     *
     * @param status The failed connection status
     * @return The connection result
     * @throws IllegalArgumentException if {@code status} is {@link ConnectionStatus#SUCCESS}
     */
    public static ConnectionResult failed(ConnectionStatus status) {
        if (status == SUCCESS) {
            throw new IllegalArgumentException("Status must be a failure type");
        }

        return new ConnectionResult(status, null);
    }

    /**
     * Creates a new success {@link ConnectionResult}.
     *
     * @param user The user created
     * @return The connection result
     * @throws IllegalArgumentException if {@code user} is {@code null}
     */
    public static ConnectionResult success(IBaritoneUser user) {
        Objects.requireNonNull(user);

        return new ConnectionResult(SUCCESS, user);
    }

    @Override
    public String toString() {
        return "ConnectionResult{" +
                "status=" + status +
                ", user=" + user +
                '}';
    }
}
