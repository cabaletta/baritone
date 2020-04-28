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

/**
 * @author Brady
 * @since 11/6/2018
 */
public enum ConnectionStatus {

    /**
     * The local player is not connected to a server, therefore, there is no target server to connect to.
     */
    NO_CURRENT_CONNECTION,

    /**
     * The IP of the targetted address to connect to could not be resolved.
     */
    CANT_RESOLVE_HOST,

    /**
     * The port for the detected LAN server could not be resolved.
     */
    CANT_RESOLVE_LAN,

    /**
     * The connection initialization failed.
     */
    CONNECTION_FAILED,

    /**
     * The connection was a success
     */
    SUCCESS
}
