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

package baritone.api;

import baritone.api.behavior.*;
import baritone.api.cache.IWorldProvider;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * API exposure for various things implemented in Baritone.
 * <p>
 * W.I.P
 *
 * @author Brady
 * @since 9/23/2018
 */
public final class BaritoneAPI {

    private final static IBaritoneProvider baritone;
    private final static Settings settings;

    static {
        ServiceLoader<IBaritoneProvider> baritoneLoader = ServiceLoader.load(IBaritoneProvider.class);
        Iterator<IBaritoneProvider> instances = baritoneLoader.iterator();
        baritone = instances.next();

        settings = new Settings();
    }

    public static IFollowBehavior getFollowBehavior() {
        return baritone.getFollowBehavior();
    }

    public static ILookBehavior getLookBehavior() {
        return baritone.getLookBehavior();
    }

    public static IMemoryBehavior getMemoryBehavior() {
        return baritone.getMemoryBehavior();
    }

    public static IMineBehavior getMineBehavior() {
        return baritone.getMineBehavior();
    }

    public static IPathingBehavior getPathingBehavior() {
        return baritone.getPathingBehavior();
    }

    public static Settings getSettings() {
        return settings;
    }

    public static IWorldProvider getWorldProvider() {
        return baritone.getWorldProvider();
    }
}
