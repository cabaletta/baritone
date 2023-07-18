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

package baritone.api.process;

import net.minecraft.util.math.BlockPos;

public interface IElytraProcess extends IBaritoneProcess {

    void repackChunks();

    /**
     * @return Where it is currently flying to, null if not active
     */
    BlockPos currentDestination();

    void pathTo(BlockPos destination);

    /**
     * Resets the state of the process but will maintain the same destination and will try to keep flying
     */
    void resetState();

    /**
     * @return {@code true} if the native library loaded and elytra is actually usable
     */
    boolean isLoaded();

    /*
     * FOR INTERNAL USE ONLY. MAY BE REMOVED AT ANY TIME.
     */
    boolean isSafeToCancel();
}
