package baritone.bot.event.events;

import baritone.bot.event.events.type.Cancellable;

/**
 * @author Brady
 * @since 8/1/2018 6:39 PM
 */
public final class ChatEvent extends Cancellable {

    /**
     * The message being sent
     */
    private final String message;

    public ChatEvent(String message) {
        this.message = message;
    }

    /**
     * @return The message being sent
     */
    public final String getMessage() {
        return this.message;
    }
}
