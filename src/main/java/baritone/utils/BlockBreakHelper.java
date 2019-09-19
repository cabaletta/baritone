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
import baritone.utils.accessor.IPlayerControllerMP;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
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

    private void tryBreakBlock(BlockPos pos, EnumFacing side) {
        if (playerContext.playerController().onPlayerDamageBlock(pos, side)) {
            playerContext.player().swingArm(EnumHand.MAIN_HAND);
        }
    }

    public void stopBreakingBlock() {
        // The player controller will never be null, but the player can be
        if (playerContext.player() != null && didBreakLastTick) {
            if (((IPlayerControllerMP) mc.playerController).getCurrentBlock().getY() != -1) {
                // insane bypass to check breaking succeeded
                ((IPlayerControllerMP) mc.playerController).setIsHittingBlock(true);
            }
            playerContext.playerController().resetBlockRemoving();
            didBreakLastTick = false;
        }
    }


    public void tick(boolean isLeftClick) {
        RayTraceResult trace = playerContext.objectMouseOver();
        boolean isBlockTrace = trace != null && trace.typeOfHit == RayTraceResult.Type.BLOCK;

        if (isLeftClick && isBlockTrace) {
            if (!didBreakLastTick) {
                ((IPlayerControllerMP) Minecraft.getMinecraft().playerController).callSyncCurrentPlayItem();
                Minecraft.getMinecraft().playerController.clickBlock(trace.getBlockPos(), trace.sideHit);
                playerContext.player().swingArm(EnumHand.MAIN_HAND);
            }
            tryBreakBlock(trace.getBlockPos(), trace.sideHit);
            didBreakLastTick = true;
        } else if (didBreakLastTick) {
            stopBreakingBlock();
            didBreakLastTick = false;
        }
        ((IPlayerControllerMP) Minecraft.getMinecraft().playerController).setIsHittingBlock(false);
    }
}
