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

package baritone;

import baritone.api.IBaritoneProvider;
import baritone.api.behavior.*;
import baritone.api.cache.IWorldProvider;
import baritone.api.cache.IWorldScanner;
import baritone.api.event.listener.IGameEventListener;
import baritone.cache.WorldProvider;
import baritone.cache.WorldScanner;

/**
 * todo fix this cancer
 *
 * @author Brady
 * @since 9/29/2018
 */
public final class BaritoneProvider implements IBaritoneProvider {

    @Override
    public IFollowBehavior getFollowBehavior() {
        return Baritone.INSTANCE.getFollowBehavior();
    }

    @Override
    public ILookBehavior getLookBehavior() {
        return Baritone.INSTANCE.getLookBehavior();
    }

    @Override
    public IMemoryBehavior getMemoryBehavior() {
        return Baritone.INSTANCE.getMemoryBehavior();
    }

    @Override
    public IMineBehavior getMineBehavior() {
        return Baritone.INSTANCE.getMineBehavior();
    }

    @Override
    public IPathingBehavior getPathingBehavior() {
        return Baritone.INSTANCE.getPathingBehavior();
    }

    @Override
    public IWorldProvider getWorldProvider() {
        return WorldProvider.INSTANCE;
    }

    @Override
    public IWorldScanner getWorldScanner() {
        return WorldScanner.INSTANCE;
    }

    @Override
    public void registerEventListener(IGameEventListener listener) {
        Baritone.INSTANCE.registerEventListener(listener);
    }
}
