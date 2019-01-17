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

public class MessageClickSlot implements iMessage {
    public final int windowId;
    public final int slotId;
    public final int mouseButton;
    public final int clickType; // index into ClickType.values()

    public MessageClickSlot(DataInputStream in) throws IOException {
        this.windowId = in.readInt();
        this.slotId = in.readInt();
        this.mouseButton = in.readInt();
        this.clickType = in.readInt();
    }

    public MessageClickSlot(int windowId, int slotId, int mouseButton, int clickType) {
        this.windowId = windowId;
        this.slotId = slotId;
        this.mouseButton = mouseButton;
        this.clickType = clickType;
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeInt(windowId);
        out.writeInt(slotId);
        out.writeInt(mouseButton);
        out.writeInt(clickType);
    }

    @Override
    public void handle(IMessageListener listener) {
        listener.handle(this);
    }
}
