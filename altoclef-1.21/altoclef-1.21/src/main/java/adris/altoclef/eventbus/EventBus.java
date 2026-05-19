package adris.altoclef.eventbus;

import net.minecraft.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

/**
 * A static class to solve dependency issues. Lets us send and receive events globally, decoupling our codebase.
 * <p>
 * Technically `ConfigHelper` does something like this, but here is a more general case.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class EventBus {

    private static final HashMap<Class, List<Subscription>> _topics = new HashMap<>();
    private static final List<Pair<Class, Subscription>> _toAdd = new ArrayList<>();
    private static boolean _lock;

    public static <T> void publish(T event) {
        Class type = event.getClass();

        // Add all subscriptions we need to add
        for (Pair<Class, Subscription> toAdd : _toAdd) {
            subscribeInternal(toAdd.getLeft(), toAdd.getRight());
        }
        _toAdd.clear();

        if (_topics.containsKey(type)) {
            List<Subscription> subscribers = _topics.get(type);

            // Subscriptions can be deleted while they're called
            List<Subscription> toDelete = new ArrayList<>();

            // Go through our subscription list. We shouldn't modify the list while we're iterating it.
            _lock = true;
            for (Subscription subRaw : subscribers) {
                Subscription<T> sub;
                try {
                    sub = (Subscription<T>) subRaw;
                    if (sub.shouldDelete()) {
                        toDelete.add(sub);
                    } else {
                        sub.accept(event);
                    }
                } catch (ClassCastException e) {
                    System.err.println("TRIED PUBLISHING MISMAPPED EVENT: " + event);
                    e.printStackTrace();
                }
            }
            // Delete all subscriptions
            _lock = false;
        }
    }

    private static <T> void subscribeInternal(Class<T> type, Subscription<T> sub) {
        if (!_topics.containsKey(type)) {
            _topics.put(type, new ArrayList<>());
        }
        _topics.get(type).add(sub);
    }

    public static <T> Subscription<T> subscribe(Class<T> type, Consumer<T> consumeEvent) {
        Subscription<T> sub = new Subscription<>(consumeEvent);
        if (_lock) {
            _toAdd.add(new Pair<>(type, sub));
        } else {
            subscribeInternal(type, sub);
        }
        return sub;
    }

    public static <T> void unsubscribe(Subscription<T> subscription) {
        if (subscription != null)
            subscription.delete();
    }
}
