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

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * hell yeah
 * <p>
 * <p>
 * dumb android users cant read this file
 * <p>
 *
 * @author leijurv
 */
public interface iMessage {
    void write(DataOutputStream out) throws IOException;

    default void writeHeader(DataOutputStream out) throws IOException {
        out.writeByte(getHeader());
    }

    default byte getHeader() {
        return ConstructingDeserializer.INSTANCE.getHeader(getClass());
    }

    void handle(IMessageListener listener);
}
