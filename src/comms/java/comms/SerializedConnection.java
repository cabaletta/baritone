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

package comms;

import java.io.*;

public class SerializedConnection implements IConnection<SerializableMessage> {
    private final DataInputStream in;
    private final DataOutputStream out;
    private final MessageDeserializer deserializer;

    public SerializedConnection(InputStream in, OutputStream out) {
        this(ConstructingDeserializer.INSTANCE, in, out);
    }

    public SerializedConnection(MessageDeserializer d, InputStream in, OutputStream out) {
        this.in = new DataInputStream(in);
        this.out = new DataOutputStream(out);
        this.deserializer = d;
    }

    @Override
    public void sendMessage(SerializableMessage message) throws IOException {
        message.writeHeader(out);
        message.write(out);
    }

    @Override
    public SerializableMessage receiveMessage() throws IOException {
        return deserializer.deserialize(in);
    }

    @Override
    public void close() {
        try {
            in.close();
        } catch (IOException e) {
        }
        try {
            out.close();
        } catch (IOException e) {
        }
    }
}