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

package baritone.api.event.events;

import baritone.api.event.events.type.EventState;

/**
 * @author Brady
 * @since 8/2/2018
 */
public final class ChunkEvent {

    /**
     * The state of the event
     */
    private final EventState state;

    /**
     * The type of chunk event that occurred
     *
     * @see Type
     */
    private final Type type;

    /**
     * The Chunk X position.
     */
    private final int x;

    /**
     * The Chunk Z position.
     */
    private final int z;

    public ChunkEvent(EventState state, Type type, int x, int z) {
        this.state = state;
        this.type = type;
        this.x = x;
        this.z = z;
    }

    /**
     * @return The state of the event
     */
    public EventState getState() {
        return this.state;
    }

    /**
     * @return The type of chunk event that occurred;
     */
    public Type getType() {
        return this.type;
    }

    /**
     * @return The Chunk X position.
     */
    public int getX() {
        return this.x;
    }

    /**
     * @return The Chunk Z position.
     */
    public int getZ() {
        return this.z;
    }

    /**
     * @return {@code true} if the event was fired after a chunk population
     */
    public boolean isPostPopulate() {
        return this.state == EventState.POST && this.type.isPopulate();
    }

    public enum Type {

        /**
         * When the chunk is constructed.
         */
        LOAD,

        /**
         * When the chunk is deconstructed.
         */
        UNLOAD,

        /**
         * When the chunk is being populated with blocks, tile entities, etc.
         * <p>
         * And it's a full chunk
         */
        POPULATE_FULL,

        /**
         * When the chunk is being populated with blocks, tile entities, etc.
         * <p>
         * And it's a partial chunk
         */
        POPULATE_PARTIAL;

        public final boolean isPopulate() {
            return this == POPULATE_FULL || this == POPULATE_PARTIAL;
        }
    }
}
