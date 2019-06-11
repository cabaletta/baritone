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

import baritone.api.utils.Helper;
import baritone.api.utils.IPlayerContext;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;

/**
 * @author Brady
 * @since 8/25/2018
 */
public final class BlockBreakHelper implements Helper {

    private boolean didBreakLastTick;

    private final IPlayerContext playerContext;

    public BlockBreakHelper(IPlayerContext playerContext) {
        this.playerContext = playerContext;
    }

    public void tryBreakBlock(BlockPos pos, Direction side) {
        if (playerContext.playerController().onPlayerDamageBlock(pos, side)) {
            playerContext.player().swingArm(Hand.MAIN_HAND);
        }
    }

    public void stopBreakingBlock() {
        // The player controller will never be null, but the player can be
        if (playerContext.player() != null) {
            playerContext.playerController().resetBlockRemoving();
        }
    }


    public void tick(boolean isLeftClick) {
        RayTraceResult trace = playerContext.objectMouseOver();
        boolean isBlockTrace = trace != null && trace.getType() == RayTraceResult.Type.BLOCK;

        if (isLeftClick && isBlockTrace) {
            tryBreakBlock(((BlockRayTraceResult) trace).getPos(), ((BlockRayTraceResult) trace).getFace());
            didBreakLastTick = true;
        } else if (didBreakLastTick) {
            stopBreakingBlock();
            didBreakLastTick = false;
        }
    }
}
