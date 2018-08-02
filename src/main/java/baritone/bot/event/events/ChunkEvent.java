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
     * The Chunk Y position.
     */
    private final int y;

    public ChunkEvent(EventState state, Type type, int x, int y) {
        this.state = state;
        this.type = type;
        this.x = x;
        this.y = y;
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
     * @return The Chunk Y position.
     */
    public final int getY() {
        return this.y;
    }

    public enum Type {
        LOAD,
        UNLOAD
    }
}
