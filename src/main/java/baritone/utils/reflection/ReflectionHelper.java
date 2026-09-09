package baritone.utils.reflection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

/**
 * @author ZacSharp
 * @since 3/26/2025
 */
public class ReflectionHelper {

    private ReflectionHelper() {}

    // Note: This approach works with public/protected/package-private methods only.
    // If we need fields or private methods that should be possible with some mixin
    // trickery like a custom injection point with the name we need as its target.
    /**
     * Get a map with all declared in {@code class} with the {@code Marker} annotation
     * using the annotation value as the map key.
     */
    public static Map<String, Method> getMarkedMethods(Class<? extends Object> cls) {
        Map<String, Method> methods = new HashMap<>();
        for (Method method : cls.getDeclaredMethods()) {
            Marker marker = method.getAnnotation(Marker.class);
            if (marker == null) {
                continue;
            }
            methods.put(marker.value(), method);
        }
        return methods;
    }

    /**
     * Gets a public method of {@code cls} with the same name and parameter
     * types as {@code sig}, or null if such a method does not exist.
     *
     * @param the class to search in
     * @param a method with the same signature as the sought method
     * @return the found method or null
     */
    public static Method getBySignature(Class<? extends Object> cls, Method sig) {
        try {
            return cls.getMethod(sig.getName(), sig.getParameterTypes());
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public static @interface Marker {
        String value();
    }
}
