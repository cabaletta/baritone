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

public class MessageStatus implements iMessage {

    public final double x;
    public final double y;
    public final double z;
    public final float yaw;
    public final float pitch;
    public final boolean onGround;
    public final float health;
    public final float saturation;
    public final int foodLevel;
    public final boolean hasCurrentSegment;
    public final boolean hasNextSegment;
    public final boolean calcInProgress;
    public final double ticksRemainingInCurrent;
    public final boolean calcFailedLastTick;
    public final boolean safeToCancel;
    public final String currentGoal;
    public final String currentProcess;

    public MessageStatus(DataInputStream in) throws IOException {
        this.x = in.readDouble();
        this.y = in.readDouble();
        this.z = in.readDouble();
        this.yaw = in.readFloat();
        this.pitch = in.readFloat();
        this.onGround = in.readBoolean();
        this.health = in.readFloat();
        this.saturation = in.readFloat();
        this.foodLevel = in.readInt();
        this.hasCurrentSegment = in.readBoolean();
        this.hasNextSegment = in.readBoolean();
        this.calcInProgress = in.readBoolean();
        this.ticksRemainingInCurrent = in.readDouble();
        this.calcFailedLastTick = in.readBoolean();
        this.safeToCancel = in.readBoolean();
        this.currentGoal = in.readUTF();
        this.currentProcess = in.readUTF();
    }

    public MessageStatus(double x, double y, double z, float yaw, float pitch, boolean onGround, float health, float saturation, int foodLevel, boolean hasCurrentSegment, boolean hasNextSegment, boolean calcInProgress, double ticksRemainingInCurrent, boolean calcFailedLastTick, boolean safeToCancel, String currentGoal, String currentProcess) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.onGround = onGround;
        this.health = health;
        this.saturation = saturation;
        this.foodLevel = foodLevel;
        this.hasCurrentSegment = hasCurrentSegment;
        this.hasNextSegment = hasNextSegment;
        this.calcInProgress = calcInProgress;
        this.ticksRemainingInCurrent = ticksRemainingInCurrent;
        this.calcFailedLastTick = calcFailedLastTick;
        this.safeToCancel = safeToCancel;
        this.currentGoal = currentGoal;
        this.currentProcess = currentProcess;
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeDouble(x);
        out.writeDouble(y);
        out.writeDouble(z);
        out.writeFloat(yaw);
        out.writeFloat(pitch);
        out.writeBoolean(onGround);
        out.writeFloat(health);
        out.writeFloat(saturation);
        out.writeInt(foodLevel);
        out.writeBoolean(hasCurrentSegment);
        out.writeBoolean(hasNextSegment);
        out.writeBoolean(calcInProgress);
        out.writeDouble(ticksRemainingInCurrent);
        out.writeBoolean(calcFailedLastTick);
        out.writeBoolean(safeToCancel);
        out.writeUTF(currentGoal);
        out.writeUTF(currentProcess);
    }

    @Override
    public void handle(IMessageListener listener) {
        listener.handle(this);
    }
}
