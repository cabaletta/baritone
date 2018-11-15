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

import baritone.Baritone;
import baritone.api.BaritoneAPI;
import baritone.api.utils.IPlayerContext;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;

/**
 * @author Brady
 * @since 8/25/2018
 */
public final class BlockBreakHelper implements Helper {

    /**
     * The last block that we tried to break, if this value changes
     * between attempts, then we re-initialize the breaking process.
     */
    private BlockPos lastBlock;
    private boolean didBreakLastTick;

    private IPlayerContext playerContext;

    public BlockBreakHelper(IPlayerContext playerContext) {
        this.playerContext = playerContext;
    }

    public void tryBreakBlock(BlockPos pos, EnumFacing side) {
        if (!pos.equals(lastBlock)) {
            playerContext.playerController().clickBlock(pos, side);
        }
        if (playerContext.playerController().onPlayerDamageBlock(pos, side)) {
            playerContext.player().swingArm(EnumHand.MAIN_HAND);
        }
        lastBlock = pos;
    }

    public void stopBreakingBlock() {
        if (playerContext.playerController() != null) {
            playerContext.playerController().resetBlockRemoving();
        }
        lastBlock = null;
    }

    private boolean fakeBreak() {
        if (playerContext != BaritoneAPI.getProvider().getPrimaryBaritone().getPlayerContext()) {
            // for a non primary player, we need to fake break always, CLICK_LEFT has no effect
            return true;
        }
        if (!Baritone.settings().leftClickWorkaround.get()) {
            // if this setting is false, we CLICK_LEFT regardless of gui status
            return false;
        }
        return mc.currentScreen != null;
    }

    public boolean tick(boolean isLeftClick) {
        if (!fakeBreak()) {
            if (didBreakLastTick) {
                stopBreakingBlock();
            }
            return isLeftClick;
        }

        RayTraceResult trace = playerContext.objectMouseOver();
        boolean isBlockTrace = trace != null && trace.typeOfHit == RayTraceResult.Type.BLOCK;

        if (isLeftClick && isBlockTrace) {
            tryBreakBlock(trace.getBlockPos(), trace.sideHit);
            didBreakLastTick = true;
        } else if (didBreakLastTick) {
            stopBreakingBlock();
            didBreakLastTick = false;
        }
        return false; // fakeBreak is true so no matter what we aren't forcing CLICK_LEFT
    }
}
