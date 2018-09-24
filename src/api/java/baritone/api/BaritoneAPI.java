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

/**
 * API exposure for various things implemented in Baritone.
 * <p>
 * W.I.P
 *
 * @author Brady
 * @since 9/23/2018
 */
public class BaritoneAPI {

    // General
    private static final Settings settings = new Settings();
    private static IWorldProvider worldProvider;

    // Behaviors
    private static IFollowBehavior followBehavior;
    private static ILookBehavior lookBehavior;
    private static IMemoryBehavior memoryBehavior;
    private static IMineBehavior mineBehavior;
    private static IPathingBehavior pathingBehavior;

    public static IFollowBehavior getFollowBehavior() {
        return followBehavior;
    }

    public static ILookBehavior getLookBehavior() {
        return lookBehavior;
    }

    public static IMemoryBehavior getMemoryBehavior() {
        return memoryBehavior;
    }

    public static IMineBehavior getMineBehavior() {
        return mineBehavior;
    }

    public static IPathingBehavior getPathingBehavior() {
        return pathingBehavior;
    }

    public static Settings getSettings() {
        return settings;
    }

    /**
     * FOR INTERNAL USE ONLY
     */
    public static void registerProviders(
            IWorldProvider worldProvider
    ) {
        BaritoneAPI.worldProvider = worldProvider;
    }

    /**
     * FOR INTERNAL USE ONLY
     */
    // @formatter:off
    public static void registerDefaultBehaviors(
            IFollowBehavior  followBehavior,
            ILookBehavior    lookBehavior,
            IMemoryBehavior  memoryBehavior,
            IMineBehavior    mineBehavior,
            IPathingBehavior pathingBehavior
    ) {
        BaritoneAPI.followBehavior  = followBehavior;
        BaritoneAPI.lookBehavior    = lookBehavior;
        BaritoneAPI.memoryBehavior  = memoryBehavior;
        BaritoneAPI.mineBehavior    = mineBehavior;
        BaritoneAPI.pathingBehavior = pathingBehavior;
    }
    // @formatter:on
}
