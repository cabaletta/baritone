/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.bot;

import net.minecraft.util.math.BlockPos;

/**
 * @author Brady
 * @since 7/31/2018 10:50 PM
 */
public final class Memory {

    public final void scanBlock(BlockPos pos) {
        checkActive(() -> {
            // We might want to always run this method even if Baritone
            // isn't active, this is just an example of the implementation
            // of checkActive(Runnable).
        });
    }

    private void checkActive(Runnable runnable) {
        if (Baritone.INSTANCE.isActive()) {
            runnable.run();
        }
    }
}
