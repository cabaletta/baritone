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

public class MessageEchestConfirmed implements iMessage {
    public final int slot;
    public final String item;

    public MessageEchestConfirmed(DataInputStream in) throws IOException {
        this.slot = in.readInt();
        this.item = in.readUTF();
    }

    public MessageEchestConfirmed(int slot, String item) {
        this.slot = slot;
        this.item = item;
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeInt(slot);
        out.writeUTF(item);
    }

    @Override
    public void handle(IMessageListener listener) {
        listener.handle(this);
    }
}
