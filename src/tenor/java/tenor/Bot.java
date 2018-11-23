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

package tenor;

import comms.IConnection;
import comms.IMessageListener;
import comms.upward.MessageStatus;

public class Bot implements IMessageListener {
    public final BotTaskRegistry taskRegistry = new BotTaskRegistry(this);

    private final IConnection connectionToBot;
    private volatile MessageStatus mostRecentUpdate;

    public Bot(IConnection conn) {
        this.connectionToBot = conn;
        // TODO event loop to read messages non blockingly
    }

    public int getCurrentQuantityInInventory(String item) {
        // TODO get this information from the most recent update
        throw new UnsupportedOperationException("oppa");
    }

    @Override
    public void handle(MessageStatus msg) {
        mostRecentUpdate = msg;
    }
}
