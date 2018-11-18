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

import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Do you not like having a blocking "receiveMessage" thingy?
 * <p>
 * Do you prefer just being able to get a list of all messages received since the last tick?
 * <p>
 * If so, this class is for you!
 *
 * @author leijurv
 */
public class BufferedConnection<T extends iMessage> implements IConnection<T> {
    private final IConnection wrapped;
    private final LinkedBlockingQueue<T> queue;
    private volatile transient IOException thrownOnRead;

    public BufferedConnection(IConnection<T> wrapped) {
        this(wrapped, Integer.MAX_VALUE); // LinkedBlockingQueue accepts this as "no limit"
    }

    public BufferedConnection(IConnection<T> wrapped, int maxInternalQueueSize) {
        this.wrapped = wrapped;
        this.queue = new LinkedBlockingQueue<>();
        this.thrownOnRead = null;
        new Thread(() -> {
            try {
                while (thrownOnRead == null) {
                    queue.put(wrapped.receiveMessage());
                }
            } catch (IOException e) {
                thrownOnRead = e;
            } catch (InterruptedException e) {
                thrownOnRead = new IOException("Interrupted while enqueueing", e);
            }
        }).start();
    }

    @Override
    public void sendMessage(T message) throws IOException {
        wrapped.sendMessage(message);
    }

    @Override
    public T receiveMessage() {
        throw new UnsupportedOperationException("BufferedConnection can only be read from non-blockingly");
    }

    @Override
    public void close() {
        wrapped.close();
        thrownOnRead = new EOFException("Closed");
    }

    public List<T> receiveMessagesNonBlocking() throws IOException {
        ArrayList<T> msgs = new ArrayList<>();
        queue.drainTo(msgs); // preserves order -- first message received will be first in this arraylist
        if (msgs.isEmpty() && thrownOnRead != null) {
            IOException up = new IOException("BufferedConnection wrapped", thrownOnRead);
            throw up;
        }
        return msgs;
    }
}
