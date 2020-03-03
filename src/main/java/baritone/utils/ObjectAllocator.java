/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.utils;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * Hacky util to allocate objects without needing to invoke their constructor.
 *
 * @author Brady
 * @since 3/3/2020
 */
public final class ObjectAllocator {

    private static final Unsafe theUnsafe;

    static {
        try {
            Class<?> clazz = Class.forName("sun.misc.Unsafe");
            Field field = clazz.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            theUnsafe = (Unsafe) field.get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ObjectAllocator() {}

    public static <T> T allocate(Class<T> clazz) {
        try {
            // noinspection unchecked
            return (T) theUnsafe.allocateInstance(clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
