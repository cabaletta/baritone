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

import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * Provides optional, dependency-free integration with Meteor Client's Elytra Boost module.
 * <p>
 * Meteor Client is detected and invoked entirely through reflection so that Baritone has no
 * compile-time or runtime dependency on it. If Meteor Client (or its Elytra Boost module) is
 * not present or not active, this class simply reports the boost as unavailable.
 */
public final class MeteorClientCompat {

    private static final Logger LOGGER = LoggerFactory.getLogger("Baritone");

    private static final String MODULES_CLASS = "meteordevelopment.meteorclient.systems.modules.Modules";
    private static final String ELYTRA_BOOST_CLASS = "meteordevelopment.meteorclient.systems.modules.movement.ElytraBoost";

    private static Class<?> modulesClass;
    private static Class<?> elytraBoostClass;
    private static Object modules;
    private static Object elytraBoostModule;
    private static Method boostMethod;
    private static boolean resolved;
    private static boolean available;
    private static boolean warnedUnavailable;

    private MeteorClientCompat() {
    }

    /**
     * Attempts to trigger a Meteor Client Elytra Boost, provided the Meteor Elytra Boost module is installed and
     * currently enabled.
     *
     * @return {@code true} if the boost was triggered, {@code false} if Meteor Client is not present or the Elytra
     *         Boost module is not active.
     */
    public static synchronized boolean tryBoost() {
        if (!resolve()) {
            if (!warnedUnavailable) {
                warnedUnavailable = true;
                LOGGER.warn("elytraBoostModule is enabled but Meteor Client's Elytra Boost module is not available");
            }
            return false;
        }
        try {
            // Meteor's ElytraBoost.boost() silently refuses to act while a screen is open or the player isn't
            // fall flying (mc.player.isFallFlying() && mc.gui.screen() == null). Mirror that guard here; otherwise
            // we'd report a successful boost to Baritone, which then skips using a real firework and never boosts.
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.gui.screen() != null
                    || minecraft.player == null
                    || !minecraft.player.isFallFlying()) {
                return false;
            }
            Method isActive = elytraBoostClass.getMethod("isActive");
            if (!((Boolean) isActive.invoke(elytraBoostModule))) {
                return false;
            }
            boostMethod.invoke(elytraBoostModule);
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOGGER.warn("Failed to invoke Meteor Client Elytra Boost", e);
            return false;
        }
    }

    private static boolean resolve() {
        if (resolved) {
            return available;
        }
        resolved = true;
        try {
            modulesClass = Class.forName(MODULES_CLASS);
            elytraBoostClass = Class.forName(ELYTRA_BOOST_CLASS);

            Method getMethod = modulesClass.getMethod("get");
            modules = getMethod.invoke(null);

            Method getModuleMethod = modulesClass.getMethod("get", Class.class);
            elytraBoostModule = getModuleMethod.invoke(modules, elytraBoostClass);

            boostMethod = elytraBoostClass.getDeclaredMethod("boost");
            boostMethod.setAccessible(true);

            available = elytraBoostModule != null && boostMethod != null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            available = false;
        }
        return available;
    }
}