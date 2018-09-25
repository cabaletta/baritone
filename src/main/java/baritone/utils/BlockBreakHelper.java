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

import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;

/**
 * @author Brady
 * @since 8/25/2018
 */
public final class BlockBreakHelper implements Helper {

    /**
     * The last block that we tried to break, if this value changes
     * between attempts, then we re-initialize the breaking process.
     */
    private static BlockPos lastBlock;

    private BlockBreakHelper() {}

    public static void tryBreakBlock(BlockPos pos, EnumFacing side) {
        if (!pos.equals(lastBlock)) {
            mc.playerController.clickBlock(pos, side);
        }
        if (mc.playerController.onPlayerDamageBlock(pos, side)) {
            mc.player.swingArm(EnumHand.MAIN_HAND);
        }
        lastBlock = pos;
    }

    public static void stopBreakingBlock() {
        if (mc.playerController != null) {
            mc.playerController.resetBlockRemoving();
        } 
        lastBlock = null;
    }
}
