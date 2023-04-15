package com.github.btrekkie.connectivity.test;

/**
 * Wraps a value using reference equality. In other words, two references are equal only if their values are the same
 * object instance, as in ==.
 * @param <T> The type of value.
 */
class Reference<T> {
    /** The value this wraps. */
    private final T value;

    public Reference(T value) {
        this.value = value;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof Reference)) {
            return false;
        }
        Reference<?> reference = (Reference<?>)obj;
        return value == reference.value;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(value);
    }
}
