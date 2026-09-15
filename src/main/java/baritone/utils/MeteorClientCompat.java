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

import baritone.utils.accessor.IFireworkRocketEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.item.component.FireworkExplosion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    /**
     * Client-side "ghost" firework rockets that Baritone spawned itself. A ghost rocket has no
     * server-side counterpart, so the server never broadcasts entity event {@code 17} to it and the
     * vanilla client would otherwise never discard it, causing its boost impulse to apply forever.
     * Each ghost kept here is forcibly discarded once {@code life > lifetime}, giving it exactly the
     * boost duration implied by the module's {@code fireworkLevel} (flight duration) setting.
     */
    private static final Set<Integer> ghostRocketIds = new HashSet<>();

    private MeteorClientCompat() {
    }

    /**
     * Attempts to trigger a Meteor Client Elytra Boost, provided the Meteor Elytra Boost module is installed and
     * currently enabled.
     * <p>
     * Meteor's own {@code ElytraBoost.boost()} refuses to spawn anything while a screen is open
     * ({@code Minecraft.getInstance().gui.screen() != null}), so when a screen is open Baritone replicates the ghost
     * firework spawn directly instead of delegating. This keeps the firework-free boost working even inside GUIs.
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
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !minecraft.player.isFallFlying() || minecraft.level == null) {
            return false;
        }
        try {
            Method isActive = elytraBoostClass.getMethod("isActive");
            if (!((Boolean) isActive.invoke(elytraBoostModule))) {
                return false;
            }
            if (minecraft.gui.screen() == null) {
                boostMethod.invoke(elytraBoostModule);
                trackGhostRocketAttachedToPlayer(minecraft);
            } else {
                replicateGhostBoost(minecraft);
            }
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOGGER.warn("Failed to invoke Meteor Client Elytra Boost", e);
            return false;
        }
    }

    /**
     * Mirrors what {@code ElytraBoost.boost()} does after its guards pass, minus the
     * {@code gui.screen() == null} restriction: spawns a client-side firework rocket attached to the player.
     */
    private static void replicateGhostBoost(Minecraft minecraft) {
        final int fireworkLevel = moduleSetting("fireworkLevel", 0);
        final boolean playSound = moduleSetting("playSound", true);

        ItemStack itemStack = Items.FIREWORK_ROCKET.getDefaultInstance();
        Fireworks defaultFireworks = itemStack.get(DataComponents.FIREWORKS);
        List<FireworkExplosion> explosions = defaultFireworks != null ? defaultFireworks.explosions() : Collections.emptyList();
        itemStack.set(DataComponents.FIREWORKS, new Fireworks(fireworkLevel, explosions));

        FireworkRocketEntity entity = new FireworkRocketEntity(minecraft.level, itemStack, minecraft.player);
        if (playSound) {
            minecraft.level.playSound(minecraft.player, entity, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.AMBIENT, 3.0F, 1.0F);
        }
        minecraft.level.addEntity(entity);
        trackGhostRocket(entity);
    }

    /**
     * Finds a ghost firework rocket that Meteor's {@code boost()} just spawned attached to the local
     * player (the non-GUI path doesn't go through {@link #replicateGhostBoost}, so track it here too).
     */
    private static void trackGhostRocketAttachedToPlayer(Minecraft minecraft) {
        if (minecraft.level == null) {
            return;
        }
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (entity instanceof FireworkRocketEntity
                    && ((IFireworkRocketEntity) entity).getBoostedEntity() == minecraft.player) {
                trackGhostRocket((FireworkRocketEntity) entity);
            }
        }
    }

    /**
     * @return Whether the given entity id belongs to a Baritone-spawned ghost firework rocket.
     */
    public static boolean isGhostRocket(int entityId) {
        return ghostRocketIds.contains(entityId);
    }

    /**
     * @param entity A firework rocket Baritone spawned (or Meteor spawned on Baritone's behalf).
     */
    public static void trackGhostRocket(FireworkRocketEntity entity) {
        ghostRocketIds.add(entity.getId());
    }

    /**
     * Removes a ghost rocket from tracking. Called once the rocket has been discarded.
     */
    public static void untrackGhostRocket(int entityId) {
        ghostRocketIds.remove(entityId);
    }

    private static int moduleSetting(String fieldName, int fallback) {
        Object value = readModuleSetting(fieldName);
        return value instanceof Number ? ((Number) value).intValue() : fallback;
    }

    private static boolean moduleSetting(String fieldName, boolean fallback) {
        Object value = readModuleSetting(fieldName);
        return value instanceof Boolean ? (Boolean) value : fallback;
    }

    private static Object readModuleSetting(String fieldName) {
        try {
            Field field = elytraBoostClass.getDeclaredField(fieldName);
            field.setAccessible(true);
            Object setting = field.get(elytraBoostModule);
            return setting.getClass().getMethod("get").invoke(setting);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
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