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

package cabaletta.comms.upward;

import cabaletta.comms.IMessageListener;
import cabaletta.comms.iMessage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MessageStatus implements iMessage {

    public final String playerUUID;
    public final String serverIP;
    public final double x;
    public final double y;
    public final double z;
    public final float yaw;
    public final float pitch;
    public final boolean onGround;
    public final float health;
    public final float saturation;
    public final int foodLevel;
    public final int dimension;
    public final int pathStartX;
    public final int pathStartY;
    public final int pathStartZ;
    public final boolean hasCurrentSegment;
    public final boolean hasNextSegment;
    public final boolean calcInProgress;
    public final double ticksRemainingInCurrent;
    public final boolean calcFailedLastTick;
    public final boolean safeToCancel;
    public final String currentGoal;
    public final String currentProcess;
    public final List<String> mainInventory;
    public final List<String> armor;
    public final String offHand;

    public MessageStatus(DataInputStream in) throws IOException {
        this.playerUUID = in.readUTF();
        this.serverIP = in.readUTF();
        this.x = in.readDouble();
        this.y = in.readDouble();
        this.z = in.readDouble();
        this.yaw = in.readFloat();
        this.pitch = in.readFloat();
        this.onGround = in.readBoolean();
        this.health = in.readFloat();
        this.saturation = in.readFloat();
        this.foodLevel = in.readInt();
        this.dimension = in.readInt();
        this.pathStartX = in.readInt();
        this.pathStartY = in.readInt();
        this.pathStartZ = in.readInt();
        this.hasCurrentSegment = in.readBoolean();
        this.hasNextSegment = in.readBoolean();
        this.calcInProgress = in.readBoolean();
        this.ticksRemainingInCurrent = in.readDouble();
        this.calcFailedLastTick = in.readBoolean();
        this.safeToCancel = in.readBoolean();
        this.currentGoal = in.readUTF();
        this.currentProcess = in.readUTF();
        this.mainInventory = readList(36, in);
        this.armor = readList(4, in);
        this.offHand = in.readUTF();
    }

    public MessageStatus(String playerUUID, String serverIP, double x, double y, double z, float yaw, float pitch, boolean onGround, float health, float saturation, int foodLevel, int dimension, int pathStartX, int pathStartY, int pathStartZ, boolean hasCurrentSegment, boolean hasNextSegment, boolean calcInProgress, double ticksRemainingInCurrent, boolean calcFailedLastTick, boolean safeToCancel, String currentGoal, String currentProcess, List<String> mainInventory, List<String> armor, String offHand) {
        this.playerUUID = playerUUID;
        this.serverIP = serverIP;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.onGround = onGround;
        this.health = health;
        this.saturation = saturation;
        this.foodLevel = foodLevel;
        this.dimension = dimension;
        this.pathStartX = pathStartX;
        this.pathStartY = pathStartY;
        this.pathStartZ = pathStartZ;
        this.hasCurrentSegment = hasCurrentSegment;
        this.hasNextSegment = hasNextSegment;
        this.calcInProgress = calcInProgress;
        this.ticksRemainingInCurrent = ticksRemainingInCurrent;
        this.calcFailedLastTick = calcFailedLastTick;
        this.safeToCancel = safeToCancel;
        this.currentGoal = currentGoal;
        this.currentProcess = currentProcess;
        this.mainInventory = mainInventory;
        this.armor = armor;
        this.offHand = offHand;
        if (mainInventory.size() != 36 || armor.size() != 4) {
            throw new IllegalStateException();
        }
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeUTF(playerUUID);
        out.writeUTF(serverIP);
        out.writeDouble(x);
        out.writeDouble(y);
        out.writeDouble(z);
        out.writeFloat(yaw);
        out.writeFloat(pitch);
        out.writeBoolean(onGround);
        out.writeFloat(health);
        out.writeFloat(saturation);
        out.writeInt(foodLevel);
        out.writeInt(dimension);
        out.writeInt(pathStartX);
        out.writeInt(pathStartY);
        out.writeInt(pathStartZ);
        out.writeBoolean(hasCurrentSegment);
        out.writeBoolean(hasNextSegment);
        out.writeBoolean(calcInProgress);
        out.writeDouble(ticksRemainingInCurrent);
        out.writeBoolean(calcFailedLastTick);
        out.writeBoolean(safeToCancel);
        out.writeUTF(currentGoal);
        out.writeUTF(currentProcess);
        write(mainInventory, out);
        write(armor, out);
        out.writeUTF(offHand);
    }

    private static List<String> readList(int length, DataInputStream in) throws IOException {
        ArrayList<String> result = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            result.add(in.readUTF());
        }
        return result;
    }

    private static void write(List<String> list, DataOutputStream out) throws IOException {
        for (String str : list) {
            out.writeUTF(str);
        }
    }

    @Override
    public void handle(IMessageListener listener) {
        listener.handle(this);
    }
}
