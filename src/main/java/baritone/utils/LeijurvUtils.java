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

import baritone.Baritone;
import org.lwjgl.opengl.GL14;

import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Is your name Leijurv and you have a severe handicap for X? Well, no worries! This class has YOU covered!
 *
 * @author Brady
 * @since 1/30/2019
 */
public class LeijurvUtils {

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    private static Runnable glBlendColorChunkOwnage = null;
    private static float existingAlpha;

    public static Runnable updateAndGet() {
        float currentAlpha = Baritone.settings().cachedChunksOpacity.get();
        if (glBlendColorChunkOwnage == null || existingAlpha != currentAlpha) {
            glBlendColorChunkOwnage = createBlendColorRunnable(1, 1, 1, existingAlpha = currentAlpha);
        }
        return glBlendColorChunkOwnage;
    }

    /**
     * Hahhahaha
     * WHY???? you may ask. well:
     *
     * basically proguard sucks (actually the gradle automated setup for it does) and I'm too
     * lazy to actually make it work. (It is unable to recognize the GL14 class). This is an
     * AWESOME workaround by creating a runnable for the {@code glBlendColor} method without having
     * to directly reference the method itself with the added bonus of pre-filling the invocation arguments.
     *
     * @param r red
     * @param g green
     * @param b blue
     * @param a alpha
     * @return Epic runnable that calls {@link GL14#glBlendColor(float, float, float, float)}
     */
    public static Runnable createBlendColorRunnable(float r, float g, float b, float a) {
        try {
            MethodHandle glBlendColor = LOOKUP.findStatic(
                    Class.forName("org.lwjgl.opengl.GL14"),
                    "glBlendColor",
                    MethodType.methodType(Void.TYPE, Float.TYPE, Float.TYPE, Float.TYPE, Float.TYPE)
            );

            // noinspection unchecked
            return (Runnable) LambdaMetafactory.metafactory(
                    LOOKUP,
                    "run",
                    glBlendColor.type().changeReturnType(Runnable.class),
                    MethodType.methodType(Void.TYPE),
                    glBlendColor,
                    MethodType.methodType(Void.TYPE)
            ).dynamicInvoker().invoke(r, g, b, a);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
