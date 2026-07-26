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

import baritone.api.BaritoneAPI;
import baritone.api.utils.IPlayerContext;
import baritone.utils.accessor.IPlayerControllerMP;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * @author Brady
 * @since 8/25/2018
 */
public final class BlockBreakHelper {
    // base ticks between block breaks caused by tick logic
    private static final int BASE_BREAK_DELAY = 1;

    private final IPlayerContext ctx;
    private boolean wasHitting;
    private int breakDelayTimer = 0;
    private BlockPos activeBlock;
    private Direction activeFace;

    BlockBreakHelper(IPlayerContext ctx) {
        this.ctx = ctx;
    }

    public void stopBreakingBlock() {
        // The player controller will never be null, but the player can be
        if (ctx.player() != null && wasHitting) {
            ctx.playerController().setHittingBlock(false);
            ctx.playerController().resetBlockRemoving();
            wasHitting = false;
        }
        activeBlock = null;
        activeFace = null;
    }

    public void tick(boolean isLeftClick) {
        if (breakDelayTimer > 0) {
            breakDelayTimer--;
            return;
        }
        if (!isLeftClick) {
            wasHitting = false;
            activeBlock = null;
            activeFace = null;
            return;
        }

        if (!canContinueActiveTarget()) {
            if (wasHitting) {
                ctx.playerController().resetBlockRemoving();
            }
            wasHitting = false;
            activeBlock = null;
            activeFace = null;

            HitResult trace = ctx.objectMouseOver();
            if (trace == null || trace.getType() != HitResult.Type.BLOCK) {
                return;
            }
            BlockHitResult blockTrace = (BlockHitResult) trace;
            activeBlock = blockTrace.getBlockPos().immutable();
            activeFace = blockTrace.getDirection();
        }

        if (activeBlock != null) {
            ctx.playerController().setHittingBlock(wasHitting);
            if (ctx.playerController().hasBrokenBlock()) {
                ctx.playerController().syncHeldItem();
                ctx.playerController().clickBlock(activeBlock, activeFace);
                ctx.player().swing(InteractionHand.MAIN_HAND);
            } else {
                if (ctx.playerController().onPlayerDamageBlock(activeBlock, activeFace)) {
                    ctx.player().swing(InteractionHand.MAIN_HAND);
                }
                if (ctx.playerController().hasBrokenBlock()) { // block broken this tick
                    // break delay timer only applies for multi-tick block breaks like vanilla
                    breakDelayTimer = BaritoneAPI.getSettings().blockBreakSpeed.value - BASE_BREAK_DELAY;
                    // must reset controller's destroy delay to prevent the client from delaying itself unnecessarily
                    ((IPlayerControllerMP) ctx.minecraft().gameMode).setDestroyDelay(0);
                }
            }
            // if true, we're breaking a block. if false, we broke the block this tick
            wasHitting = !ctx.playerController().hasBrokenBlock();
            if (!wasHitting) {
                activeBlock = null;
                activeFace = null;
            }
            // this value will be reset by the MC client handling mouse keys
            // since we're not spoofing the click keybind to the client, the client will stop the break if isDestroyingBlock is true
            // we store and restore this value on the next tick to determine if we're breaking a block
            ctx.playerController().setHittingBlock(false);
        }
    }

    private boolean canContinueActiveTarget() {
        if (!wasHitting || activeBlock == null || ctx.world().getBlockState(activeBlock).isAir()) {
            return false;
        }

        double eyeX = ctx.player().getX();
        double eyeY = ctx.player().getY() + ctx.player().getEyeHeight();
        double eyeZ = ctx.player().getZ();
        double dx = Math.max(Math.max(activeBlock.getX() - eyeX, 0.0D), eyeX - activeBlock.getX() - 1.0D);
        double dy = Math.max(Math.max(activeBlock.getY() - eyeY, 0.0D), eyeY - activeBlock.getY() - 1.0D);
        double dz = Math.max(Math.max(activeBlock.getZ() - eyeZ, 0.0D), eyeZ - activeBlock.getZ() - 1.0D);
        double reach = ctx.playerController().getBlockReachDistance();
        return dx * dx + dy * dy + dz * dz <= reach * reach;
    }
}
