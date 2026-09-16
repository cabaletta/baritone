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

package baritone.utils.accessor;

/**
 * A pathing flags word stored directly on every BlockState (via mixin), so the cost functions don't have to go
 * through the block state registry's identity hash map to find their precomputed data. See PrecomputedData.
 */
public interface IBlockStateFlags {

    int baritone$getPathingFlags();

    void baritone$setPathingFlags(int flags);
}
