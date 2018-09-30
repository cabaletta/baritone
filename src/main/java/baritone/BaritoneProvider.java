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
import baritone.behavior.*;
import baritone.cache.WorldProvider;

/**
 * @author Brady
 * @since 9/29/2018
 */
public final class BaritoneProvider implements IBaritoneProvider {

    @Override
    public IFollowBehavior getFollowBehavior() {
        return FollowBehavior.INSTANCE;
    }

    @Override
    public ILookBehavior getLookBehavior() {
        return LookBehavior.INSTANCE;
    }

    @Override
    public IMemoryBehavior getMemoryBehavior() {
        return MemoryBehavior.INSTANCE;
    }

    @Override
    public IMineBehavior getMineBehavior() {
        return MineBehavior.INSTANCE;
    }

    @Override
    public IPathingBehavior getPathingBehavior() {
        return PathingBehavior.INSTANCE;
    }

    @Override
    public IWorldProvider getWorldProvider() {
        return WorldProvider.INSTANCE;
    }
}
