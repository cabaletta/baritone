package baritone.bot.event.events;

import baritone.bot.event.events.type.EventState;

/**
 * @author Brady
 * @since 8/2/2018 12:32 AM
 */
public final class ChunkEvent {

    /**
     * The state of the event
     */
    private final EventState state;

    /**
     * The type of chunk event that occurred;
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
    public final EventState getState() {
        return this.state;
    }

    /**
     * @return The type of chunk event that occurred;
     */
    public final Type getType() {
        return this.type;
    }

    /**
     * @return The Chunk X position.
     */
    public final int getX() {
        return this.x;
    }

    /**
     * @return The Chunk Z position.
     */
    public final int getZ() {
        return this.z;
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
         */
        POPULATE
    }
}
