package adris.altoclef.eventbus;

import java.util.function.Consumer;

// A wrapper object for event subscription
public class Subscription<T> {
    private final Consumer<T> _callback;
    private boolean _shouldDelete;

    public Subscription(Consumer<T> callback) {
        _callback = callback;
    }

    public void accept(T event) {
        _callback.accept(event);
    }

    public void delete() {
        _shouldDelete = true;
    }

    public boolean shouldDelete() {
        return _shouldDelete;
    }
}
