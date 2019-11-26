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

package baritone.pathing.movement.movements.straight;

import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.IPlayerContext;
import baritone.pathing.movement.MovementHelper;
import baritone.utils.BlockStateInterface;
import baritone.utils.math.IntAABB2;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

final class FallHelper {

    public static boolean canWalkOn(BlockStateInterface bsi, int x, int y, int z) {
        IBlockState state = bsi.get0(x, y, z);
        Block block = state.getBlock();

        // be conservative for now
        return block != Blocks.AIR &&
                state.isFullCube() &&
                state.getBlock().slipperiness == 0.6F;
    }

    static WillFallResult willFall(BetterBlockPos floorBlockPos, BlockStateInterface bsi) {
        if (canWalkOn(bsi, floorBlockPos.x, floorBlockPos.y, floorBlockPos.z)) {
            // player is supported by at least one block
            return WillFallResult.NO;
        }

        // We only support either completely full blocks or fully passable
        // blocks for now. We don't support blocks like trapdoors or fences
        // that have a modified hit box.
        if (!MovementHelper.fullyPassable(bsi, floorBlockPos.x, floorBlockPos.y, floorBlockPos.z)) {
            return WillFallResult.UNSUPPORTED_TERRAIN;
        }

        return WillFallResult.YES;
    }

    static WillFallResult willFall(IntAABB2 playerAABB, int floorBlockY, BlockStateInterface bsi) {
        for (int x = playerAABB.minX; x < playerAABB.maxX; x++) {
            for (int z = playerAABB.minY; z < playerAABB.maxY; z++) {
                WillFallResult result = willFall(new BetterBlockPos(x, floorBlockY, z), bsi);
                if (result == WillFallResult.NO || result == WillFallResult.UNSUPPORTED_TERRAIN) {
                    return result;
                }
            }
        }

        return WillFallResult.YES;
    }

    public enum WillFallResult {
        YES,
        NO,
        UNSUPPORTED_TERRAIN,
    }

    static NextFallResult findNextFall(IPlayerContext ctx, BetterBlockPos dest) {
        Vec3d playerFeet = ctx.playerFeetAsVec();

        PathSimulator pathSimulator = new PathSimulator(playerFeet, dest, new BlockStateInterface(ctx));

        if (!pathSimulator.hasNext()) {
            return new NextFallResult(true);
        }

        PathSimulator.PathPart pathPart = pathSimulator.next();

        if (pathPart.isImpossible()) {
            return new NextFallResult(false);
        }

        if (pathPart.getStartY() == pathPart.getEndY()) {
            return new NextFallResult(true);
        }

        return new NextFallResult(pathPart.getFallBox(), pathPart.getEndY() - 1);
    }

    static Optional<Integer> getLandingBlock(BetterBlockPos start, BlockStateInterface bsi) {
        return getLandingBlock(new IntAABB2(start.x, start.z, start.x + 1, start.z + 1), start.y, bsi);
    }

    /**
     * Gets the Y coordinate of the block that the player will land on after a
     * fall. If any unsupported blocks are encountered, returns
     * {@code Optional.empty()}.
     *
     * @param playerAABB - the player's bounding box
     * @param startY - the current player's Y position
     * @return the Y coordinate of the block that will player will land on
     */
    static Optional<Integer> getLandingBlock(IntAABB2 playerAABB, int startY, BlockStateInterface bsi) {
        int currentY = startY - 1;

        while (currentY >= 0) {
            FallHelper.WillFallResult result = FallHelper.willFall(playerAABB, currentY, bsi);
            if (result == FallHelper.WillFallResult.NO) {
                return Optional.of(currentY);
            } else if (result == FallHelper.WillFallResult.UNSUPPORTED_TERRAIN) {
                return Optional.empty();
            }

            currentY--;
        }

        // fall into void?
        return Optional.empty();
    }

    static final class NextFallResult {
        private final boolean isPathStillValid;
        private final IntAABB2 fallBox;
        private final int floorBlockY;

        private NextFallResult(boolean isPathStillValid) {
            this.isPathStillValid = isPathStillValid;
            this.fallBox = null;
            this.floorBlockY = 0;
        }

        private NextFallResult(IntAABB2 fallBox, int floorBlockY) {
            this.isPathStillValid = true;
            this.fallBox = fallBox;
            this.floorBlockY = floorBlockY;
        }

        boolean isPathStillValid() {
            return isPathStillValid;
        }

        Optional<IntAABB2> getFallBox() {
            assertValid();
            return Optional.ofNullable(fallBox);
        }

        int getFloorBlockY() {
            assertValid();
            if (fallBox == null) {
                throw new IllegalStateException("no fall but tried to get the floor block Y");
            }
            return this.floorBlockY;
        }

        private void assertValid() {
            if (!isPathStillValid) {
                throw new IllegalStateException("path is invalid");
            }
        }
    }

}
