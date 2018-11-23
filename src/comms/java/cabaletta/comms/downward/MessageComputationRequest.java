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

package cabaletta.comms.downward;

import cabaletta.comms.IMessageListener;
import cabaletta.comms.iMessage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class MessageComputationRequest implements iMessage {
    public final long computationID;
    public final int startX;
    public final int startY;
    public final int startZ;
    public final String goal; // TODO find a better way to do this lol

    public MessageComputationRequest(DataInputStream in) throws IOException {
        this.computationID = in.readLong();
        this.startX = in.readInt();
        this.startY = in.readInt();
        this.startZ = in.readInt();
        this.goal = in.readUTF();
    }

    public MessageComputationRequest(long computationID, int startX, int startY, int startZ, String goal) {
        this.computationID = computationID;
        this.startX = startX;
        this.startY = startY;
        this.startZ = startZ;
        this.goal = goal;
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeLong(computationID);
        out.writeInt(startX);
        out.writeInt(startY);
        out.writeUTF(goal);
    }

    @Override
    public void handle(IMessageListener listener) {
        listener.handle(this);
    }
}
