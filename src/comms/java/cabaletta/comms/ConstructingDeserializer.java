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

package cabaletta.comms;

import java.io.DataInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public enum ConstructingDeserializer implements MessageDeserializer {
    INSTANCE;
    private final List<Class<? extends iMessage>> MSGS;

    ConstructingDeserializer() {
        MSGS = new ArrayList<>();
        // imagine doing something in reflect but it's actually concise and you don't need to catch 42069 different exceptions. huh.
        for (Method m : IMessageListener.class.getDeclaredMethods()) {
            if (m.getName().equals("handle")) {
                MSGS.add(0, (Class<? extends iMessage>) m.getParameterTypes()[0]);
            }
        }
    }

    @Override
    public synchronized iMessage deserialize(DataInputStream in) throws IOException {
        int type = ((int) in.readByte()) & 0xff;
        try {
            return MSGS.get(type).getConstructor(DataInputStream.class).newInstance(in);
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException ex) {
            throw new IOException("Unknown message type " + type, ex);
        }
    }

    public byte getHeader(Class<? extends iMessage> klass) {
        return (byte) MSGS.indexOf(klass);
    }
}
