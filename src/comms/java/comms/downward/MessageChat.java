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

package comms.downward;

import comms.IMessageListener;
import comms.iMessage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class MessageChat implements iMessage {

    public final String msg;

    public MessageChat(DataInputStream in) throws IOException {
        this.msg = in.readUTF();
    }

    public MessageChat(String msg) {
        this.msg = msg;
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeUTF(msg);
    }

    @Override
    public void handle(IMessageListener listener) {
        listener.handle(this);
    }
}
