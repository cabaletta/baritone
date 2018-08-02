package baritone.bot.event.events.type;

/**
 * @author Brady
 * @since 8/2/2018 12:34 AM
 */
public enum EventState {

    /**
     * Indicates that whatever action the event is being
     * dispatched as a result of is about to occur.
     */
    PRE,

    /**
     * Indicates that whatever action the event is being
     * dispatched as a result of has already occured.
     */
    POST
}
