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
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.RayTraceResult;

public class BlockPlaceHelper {

    private final IPlayerContext ctx;
    private int rightClickTimer;

    BlockPlaceHelper(IPlayerContext playerContext) {
        this.ctx = playerContext;
    }

    public void tick(boolean rightClickRequested) {
        if (rightClickTimer > 0) {
            rightClickTimer--;
            return;
        }
        RayTraceResult mouseOver = ctx.objectMouseOver();
        if (!rightClickRequested || ctx.player().isRowingBoat() || mouseOver == null || mouseOver.getBlockPos() == null || mouseOver.type != RayTraceResult.Type.BLOCK) {
            return;
        }
        rightClickTimer = Baritone.settings().rightClickSpeed.value;
        for (EnumHand hand : EnumHand.values()) {
            if (ctx.playerController().processRightClickBlock(ctx.player(), ctx.world(), mouseOver.getBlockPos(), mouseOver.sideHit, mouseOver.hitVec, hand) == EnumActionResult.SUCCESS) {
                ctx.player().swingArm(hand);
                return;
            }
            if (!ctx.player().getHeldItem(hand).isEmpty() && ctx.playerController().processRightClick(ctx.player(), ctx.world(), hand) == EnumActionResult.SUCCESS) {
                return;
            }
        }
    }
}
