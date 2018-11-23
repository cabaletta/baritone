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

import comms.downward.MessageChat;
import comms.downward.MessageComputationRequest;
import comms.upward.MessageComputationResponse;
import comms.upward.MessageStatus;

public interface IMessageListener {
    default void handle(MessageStatus message) {
        unhandled(message);
    }

    default void handle(MessageChat message) {
        unhandled(message);
    }

    default void handle(MessageComputationRequest message) {
        unhandled(message);
    }

    default void handle(MessageComputationResponse message) {
        unhandled(message);
    }

    default void unhandled(iMessage msg) {
        // can override this to throw UnsupportedOperationException, if you want to make sure you're handling everything
        // default is to silently ignore messages without handlers
    }
}