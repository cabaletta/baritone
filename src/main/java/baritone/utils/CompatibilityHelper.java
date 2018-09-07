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

package baritone.utils;

import net.minecraft.client.settings.GameSettings;

/**
 * @author Brady
 * @since 9/7/2018
 */
public final class CompatibilityHelper implements Helper {

    private CompatibilityHelper() {}

    public static boolean isAutoJumpSupported() {
        // noinspection ConstantConditions
        return getAutoJumpOption() != null;
    }

    public static boolean isAutoJump() {
        return isAutoJumpSupported() && mc.gameSettings.getOptionOrdinalValue(getAutoJumpOption());
    }

    public static void setAutoJump(boolean autoJump) {
        if (!isAutoJumpSupported() || isAutoJump() == autoJump)
            return;

        mc.gameSettings.setOptionValue(getAutoJumpOption(), 0);
    }

    private static GameSettings.Options getAutoJumpOption() {
        return GameSettings.Options.byOrdinal(38);
    }
}
