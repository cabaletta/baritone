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

package comms.upward;

import comms.IMessageListener;
import comms.iMessage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class MessageComputationResponse implements iMessage {
    public final long computationID;
    public final int pathLength;
    public final double pathCost;
    public final boolean endsInGoal;
    public final int endX;
    public final int endY;
    public final int endZ;

    public MessageComputationResponse(DataInputStream in) throws IOException {
        this.computationID = in.readLong();
        this.pathLength = in.readInt();
        this.pathCost = in.readDouble();
        this.endsInGoal = in.readBoolean();
        this.endX = in.readInt();
        this.endY = in.readInt();
        this.endZ = in.readInt();
    }

    public MessageComputationResponse(long computationID, int pathLength, double pathCost, boolean endsInGoal, int endX, int endY, int endZ) {
        this.computationID = computationID;
        this.pathLength = pathLength;
        this.pathCost = pathCost;
        this.endsInGoal = endsInGoal;
        this.endX = endX;
        this.endY = endY;
        this.endZ = endZ;
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeLong(computationID);
        out.writeInt(pathLength);
        out.writeDouble(pathCost);
        out.writeBoolean(endsInGoal);
        out.writeInt(endX);
        out.writeInt(endY);
        out.writeInt(endZ);
    }

    @Override
    public void handle(IMessageListener listener) {
        listener.handle(this);
    }
}

