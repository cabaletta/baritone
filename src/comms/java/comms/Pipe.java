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
import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Do you want a socket to localhost without actually making a gross real socket to localhost?
 */
public class Pipe<T extends iMessage> {
    private final LinkedBlockingQueue<Optional<T>> AtoB;
    private final LinkedBlockingQueue<Optional<T>> BtoA;
    private final PipedConnection A;
    private final PipedConnection B;
    private volatile boolean closed;

    public Pipe() {
        this.AtoB = new LinkedBlockingQueue<>();
        this.BtoA = new LinkedBlockingQueue<>();
        this.A = new PipedConnection(BtoA, AtoB);
        this.B = new PipedConnection(AtoB, BtoA);
    }

    public PipedConnection getA() {
        return A;
    }

    public PipedConnection getB() {
        return B;
    }

    public class PipedConnection implements IConnection<T> {
        private final LinkedBlockingQueue<Optional<T>> in;
        private final LinkedBlockingQueue<Optional<T>> out;

        private PipedConnection(LinkedBlockingQueue<Optional<T>> in, LinkedBlockingQueue<Optional<T>> out) {
            this.in = in;
            this.out = out;
        }

        @Override
        public void sendMessage(T message) throws IOException {
            if (closed) {
                throw new EOFException("Closed");
            }
            try {
                out.put(Optional.of(message));
            } catch (InterruptedException e) {
                // this can never happen since the LinkedBlockingQueues are not constructed with a maximum capacity, see above
            }
        }

        @Override
        public T receiveMessage() throws IOException {
            if (closed) {
                throw new EOFException("Closed");
            }
            try {
                Optional<T> t = in.take();
                if (!t.isPresent()) {
                    throw new EOFException("Closed");
                }
                return t.get();
            } catch (InterruptedException e) {
                // again, cannot happen
                // but we have to throw something
                throw new IllegalStateException(e);
            }
        }

        @Override
        public void close() {
            closed = true;
            try {
                AtoB.put(Optional.empty()); // unstick threads
                BtoA.put(Optional.empty());
            } catch (InterruptedException e) {
            }
        }
    }
}
