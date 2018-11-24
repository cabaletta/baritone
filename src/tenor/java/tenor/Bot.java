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

import cabaletta.comms.BufferedConnection;
import cabaletta.comms.IConnection;
import cabaletta.comms.IMessageListener;
import cabaletta.comms.iMessage;
import cabaletta.comms.upward.MessageComputationResponse;
import cabaletta.comms.upward.MessageStatus;

import java.io.IOException;

public class Bot implements IMessageListener {
    public final BotTaskRegistry taskRegistry = new BotTaskRegistry(this);

    private final BufferedConnection connectionToBot;
    private volatile MessageStatus mostRecentUpdate;

    public Bot(IConnection conn) {
        this.connectionToBot = BufferedConnection.makeBuffered(conn);
        // TODO event loop calling tick?
    }

    public int getCurrentQuantityInInventory(String item) {
        // TODO get this information from the most recent update
        throw new UnsupportedOperationException("oppa");
    }

    public void tick() {
        // TODO i have no idea what tenor's threading model will be
        // probably single threaded idk
        ComputationRequestManager.INSTANCE.dispatchAll();
        receiveMessages();
        TaskLeaf<?> curr = decideWhatToDoNow();
        iMessage command = doTheTask(curr);
        try {
            connectionToBot.sendMessage(command);
        } catch (IOException e) {
            e.printStackTrace();
            disconnect();
        }
    }

    public void receiveMessages() {
        try {
            connectionToBot.handleAllPendingMessages(this);
        } catch (IOException e) {
            e.printStackTrace();
            disconnect();
        }
    }

    public void disconnect() {
        // TODO i have no idea how to handle this
        // this destroys the task tree
        // wew lad?
        connectionToBot.close();
    }

    public TaskLeaf<?> decideWhatToDoNow() {
        // TODO idk lol
        throw new UnsupportedOperationException();
    }

    public iMessage doTheTask(TaskLeaf<?> task) {
        // TODO idk lol
        throw new UnsupportedOperationException();
    }

    public void send(iMessage msg) {
        try {
            connectionToBot.sendMessage(msg);
        } catch (IOException e) {
            e.printStackTrace();
            disconnect();
        }
    }

    public MessageStatus getMostRecentUpdate() {
        return mostRecentUpdate;
    }

    public String getHostIdentifier() {
        // return ((SocketConnection) ((BufferedConnection) connectionToBot).getUnderlying()).getSocket().getHostname()
        return "localhost";
    }

    @Override
    public void handle(MessageStatus msg) {
        mostRecentUpdate = msg;
    }

    @Override
    public void handle(MessageComputationResponse msg) {
        ComputationRequestManager.INSTANCE.onRecv(this, msg);
    }
}
