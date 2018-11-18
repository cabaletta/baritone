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

import java.io.IOException;

/**
 * This is just a meme idk if this is actually useful
 *
 * @param <T>
 */
public class FilteredConnection<T extends iMessage> implements IConnection<T> {
    private final Class<T> klass;
    private final IConnection<? super iMessage> wrapped;

    public FilteredConnection(IConnection<? super iMessage> wrapped, Class<T> klass) {
        this.klass = klass;
        this.wrapped = wrapped;
    }

    @Override
    public void sendMessage(T message) throws IOException {
        wrapped.sendMessage(message);
    }

    @Override
    public T receiveMessage() throws IOException {
        while (true) {
            iMessage msg = wrapped.receiveMessage();
            if (klass.isInstance(msg)) {
                return klass.cast(msg);
            }
        }
    }

    @Override
    public void close() {
        wrapped.close();
    }
}
