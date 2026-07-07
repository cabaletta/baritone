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
import baritone.api.utils.IPlayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class BlockPlaceHelper {
    // base ticks between places caused by tick logic
    private static final int BASE_PLACE_DELAY = 1;

    private final IPlayerContext ctx;
    private int rightClickTimer;

    BlockPlaceHelper(IPlayerContext playerContext) {
        this.ctx = playerContext;
    }

    public void tick(boolean rightClickRequested, BlockPos target, Direction side) {
        if (this.isCoolingDown()) {
            return;
        }

        BlockHitResult blockHit = this.targetedBlockHit(rightClickRequested, target, side);
        if (blockHit == null) {
            return;
        }

        rightClickTimer = Baritone.settings().rightClickSpeed.value - BASE_PLACE_DELAY;
        this.tryRightClick(blockHit);
    }

    private boolean isCoolingDown() {
        if (rightClickTimer > 0) {
            rightClickTimer--;
            return true;
        }
        return false;
    }

    private BlockHitResult targetedBlockHit(
            boolean rightClickRequested,
            BlockPos target,
            Direction side) {
        if (!rightClickRequested || ctx.player().isHandsBusy()) {
            return null;
        }

        HitResult mouseOver = ctx.objectMouseOver();
        if (mouseOver == null
                || mouseOver.getType() != HitResult.Type.BLOCK) {
            return null;
        }

        BlockHitResult blockHit = (BlockHitResult) mouseOver;
        if (!this.matchesTarget(blockHit, target, side)) {
            return null;
        }

        return blockHit;
    }

    private boolean matchesTarget(BlockHitResult blockHit, BlockPos target, Direction side) {
        return target == null
                || (blockHit.getBlockPos().equals(target) && blockHit.getDirection() == side);
    }

    private void tryRightClick(BlockHitResult blockHit) {
        for (InteractionHand hand : InteractionHand.values()) {
            if (ctx.playerController().processRightClickBlock(
                    ctx.player(),
                    ctx.world(),
                    hand,
                    blockHit) == InteractionResult.SUCCESS) {
                ctx.player().swing(hand);
                return;
            }
            if (!ctx.player().getItemInHand(hand).isEmpty()
                    && ctx.playerController().processRightClick(
                            ctx.player(),
                            ctx.world(),
                            hand) == InteractionResult.SUCCESS) {
                return;
            }
        }
    }
}
