package adris.altoclef.util.helpers;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.function.Function;

/**
 * Random functions that extend Java's STL
 */
public interface StlHelper {
    static <T> Comparator<T> compareValues(Function<T, Double> getValue) {
        return (left, right) -> (int) Math.signum(getValue.apply(left) - getValue.apply(right));
    }

    static <T> String toString(Collection<T> thing, Function<T, String> toStringFunc) {
        StringBuilder result = new StringBuilder();
        result.append("[");
        int i = 0;
        for (T item : thing) {
            result.append(toStringFunc.apply(item));
            if (i != thing.size() - 1) {
                result.append(",");
            }
            ++i;
        }
        result.append("]");
        return result.toString();
    }

    static <T> String toString(T[] thing, Function<T, String> toStringFunc) {
        return toString(Arrays.asList(thing), toStringFunc);
    }
}