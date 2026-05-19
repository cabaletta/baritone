package adris.altoclef.eventbus.events;

public class SendChatEvent {
    public String message;
    private boolean _cancelled;

    public SendChatEvent(String message) {
        this.message = message;
    }

    public void cancel() {
        _cancelled = true;
    }

    public boolean isCancelled() {
        return _cancelled;
    }
}
