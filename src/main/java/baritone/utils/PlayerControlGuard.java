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

import baritone.api.utils.IPlayerContext;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;

public final class PlayerControlGuard {

    private PlayerControlGuard() {
    }

    public static boolean canControl(IPlayerContext ctx) {
        return canControl(ctx.player(), ctx.minecraft().screen)
                && ctx.world() != null
                && !ctx.minecraft().isPaused();
    }

    public static boolean canControl(LocalPlayer player, Screen screen) {
        if (player == null || !player.isAlive()) {
            return false;
        }
        return screen == null || (!screen.isPauseScreen() && !(screen instanceof DeathScreen));
    }
}
