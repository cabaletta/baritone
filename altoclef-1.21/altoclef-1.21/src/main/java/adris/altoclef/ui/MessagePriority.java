package adris.altoclef.ui;

public enum MessagePriority {
    ASAP(3),
    TIMELY(2),
    OPTIONAL(1),
    UNAUTHORIZED(0);

    private final int _importance;

    MessagePriority(int importance) {
        _importance = importance;
    }

    public int getImportance() {
        return _importance;
    }
}
