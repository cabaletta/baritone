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

import baritone.api.behavior.ILookBehavior;
import baritone.api.behavior.IMemoryBehavior;
import baritone.api.behavior.IPathingBehavior;
import baritone.api.cache.IWorldProvider;
import baritone.api.cache.IWorldScanner;
import baritone.api.event.listener.IGameEventListener;
import baritone.api.process.ICustomGoalProcess;
import baritone.api.process.IFollowProcess;
import baritone.api.process.IGetToBlockProcess;
import baritone.api.process.IMineProcess;
import baritone.api.utils.SettingsUtil;

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

    private static final IBaritone baritone;
    private static final Settings settings;

    static {
        ServiceLoader<IBaritoneProvider> baritoneLoader = ServiceLoader.load(IBaritoneProvider.class);
        Iterator<IBaritoneProvider> instances = baritoneLoader.iterator();
        baritone = instances.next().getBaritoneForPlayer(null); // PWNAGE

        settings = new Settings();
        SettingsUtil.readAndApply(settings);
    }

    public static IFollowProcess getFollowProcess() {
        return baritone.getFollowProcess();
    }

    public static ILookBehavior getLookBehavior() {
        return baritone.getLookBehavior();
    }

    public static IMemoryBehavior getMemoryBehavior() {
        return baritone.getMemoryBehavior();
    }

    public static IMineProcess getMineProcess() {
        return baritone.getMineProcess();
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

    public static IWorldScanner getWorldScanner() {
        return baritone.getWorldScanner();
    }

    public static ICustomGoalProcess getCustomGoalProcess() {
        return baritone.getCustomGoalProcess();
    }

    public static IGetToBlockProcess getGetToBlockProcess() {
        return baritone.getGetToBlockProcess();
    }

    public static void registerEventListener(IGameEventListener listener) {
        baritone.registerEventListener(listener);
    }
}
