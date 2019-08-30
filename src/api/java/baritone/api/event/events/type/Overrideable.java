package baritone.api.event.events.type;

/**
 * @author LoganDark
 */
public class Overrideable<T> {
    private T value;
    private boolean modified;

    public Overrideable(T current) {
        value = current;
    }

    public T get() {
        return value;
    }

    public void set(T newValue) {
        value = newValue;
        modified = true;
    }

    public boolean wasModified() {
        return modified;
    }

    @Override
    public String toString() {
        return String.format(
            "Overrideable{modified=%s,value=%s}",
            Boolean.toString(modified),
            value.toString()
        );
    }
}
