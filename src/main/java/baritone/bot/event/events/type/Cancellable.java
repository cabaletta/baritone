package baritone.bot.event.events.type;

/**
 * @author Brady
 * @since 8/1/2018 6:41 PM
 */
public class Cancellable {

    /**
     * Whether or not this event has been cancelled
     */
    private boolean cancelled;

    /**
     * Cancels this event
     */
    public final void cancel() {
        this.cancelled = true;
    }

    /**
     * @return Whether or not this event has been cancelled
     */
    public final boolean isCancelled() {
        return this.cancelled;
    }
}
