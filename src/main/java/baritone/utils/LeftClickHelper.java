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
import baritone.utils.accessor.IPlayerControllerMP;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public final class LeftClickHelper {
    // base ticks between block breaks caused by tick logic
    private static final int BASE_BREAK_DELAY = 1;

    private final IPlayerContext ctx;
    private boolean isBreaking = false;
    private int breakDelayTimer = 0;
    private int leftClickTimer = 0;

    LeftClickHelper(IPlayerContext ctx) {
        this.ctx = ctx;
    }

    public void stopBreakingBlock() {
        // The player controller will never be null, but the player can be
        if (ctx.player() != null && isBreaking) {
            ctx.playerController().setHittingBlock(false);
            ctx.playerController().resetBlockRemoving();
            isBreaking = false;
        }
    }

    public void tick(boolean isLeftClick) {
        if (leftClickTimer > 0) {
            leftClickTimer--;
        }
        if (breakDelayTimer > 0) {
            breakDelayTimer--;
        }
        HitResult trace = ctx.minecraft().hitResult;
        if (isLeftClick && trace != null) {
            switch (trace.getType()) {
                case ENTITY:
                    if (leftClickTimer <= 0 &&
                            (!Baritone.settings().timedAttacks.value || ctx.player().getAttackStrengthScale(0f) == 1f)) {
                        EntityHitResult entityHitResult = (EntityHitResult) trace;
                        stopBreakingBlock();
                        leftClickTimer = Baritone.settings().leftClickSpeed.value;
                        ctx.minecraft().gameMode.attack(ctx.player(), entityHitResult.getEntity());
                        ctx.player().swing(InteractionHand.MAIN_HAND);
                    }
                    break;
                case BLOCK:
                    if (breakDelayTimer <= 0) {
                        BlockHitResult blockHitResult = (BlockHitResult) trace;
                        ctx.playerController().setHittingBlock(isBreaking);
                        if (ctx.playerController().hasBrokenBlock()) {
                            ctx.playerController().syncHeldItem();
                            ctx.playerController().clickBlock(blockHitResult.getBlockPos(), blockHitResult.getDirection());
                            ctx.player().swing(InteractionHand.MAIN_HAND);
                        } else {
                            if (ctx.playerController().onPlayerDamageBlock(blockHitResult.getBlockPos(), blockHitResult.getDirection())) {
                                ctx.player().swing(InteractionHand.MAIN_HAND);
                            }
                        }
                        if (ctx.playerController().hasBrokenBlock()) { // block broken this tick
                            // break delay timer only applies for multi-tick block breaks like vanilla
                            breakDelayTimer = Baritone.settings().blockBreakSpeed.value - BASE_BREAK_DELAY;
                            // must reset controller's destroy delay to prevent the client from delaying itself unnecessarily
                            ((IPlayerControllerMP) ctx.minecraft().gameMode).setDestroyDelay(0);
                        }
                    }
                    // if true, we're breaking a block. if false, we broke the block this tick
                    isBreaking = !ctx.playerController().hasBrokenBlock();
                    // this value will be reset by the MC client handling mouse keys
                    // since we're not spoofing the click keybind to the client, the client will stop the break if isDestroyingBlock is true
                    // we store and restore this value on the next tick to determine if we're breaking a block
                    ctx.playerController().setHittingBlock(false);
                    break;
                default:
                    isBreaking = false;
                    break;
            }
        } else {
            isBreaking = false;
        }
    }
}
