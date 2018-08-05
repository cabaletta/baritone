package baritone.bot.event.events;

import baritone.bot.event.events.type.EventState;
import javafx.event.EventType;

public final class TickEvent {

    private final EventState state;
    private final Type type;

    public TickEvent(EventState state, Type type) {
        this.state = state;
        this.type = type;
    }


    public enum Type {
        /**
         * When guarantees can be made about
         * the game state and in-game variables.
         */
        IN,
        /**
         * No guarantees can be made about the game state.
         * This probably means we are at the main menu.
         */
        OUT,
    }
}
