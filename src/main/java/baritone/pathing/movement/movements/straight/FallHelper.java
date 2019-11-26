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
import baritone.utils.BlockStateInterface;
import baritone.utils.math.IntAABB2;
import baritone.utils.math.Vector2;
import baritone.utils.pathing.GridCollisionIterator;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

public final class FallHelper {

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

        // We don't handle weird blocks yet.
        if (bsi.get0(floorBlockPos.x, floorBlockPos.y, floorBlockPos.z).getBlock() != Blocks.AIR) {
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
        EntityPlayerSP player = ctx.player();
        Vec3d playerFeet = ctx.playerFeetAsVec();

        Vector2 destXZ = new Vector2((double) dest.x + 0.5, (double) dest.z + 0.5);
        Vector2 playerPosXZ = new Vector2(player.posX, player.posZ);

        BlockStateInterface bsi = new BlockStateInterface(ctx);

        int feetBlockY = (int) Math.floor(playerFeet.y);

        // predict the next few positions
        GridCollisionIterator collisionIterator = new GridCollisionIterator(player.width, playerPosXZ, destXZ);
        for (int i = 0; i < 10 && collisionIterator.hasNext(); i++) {
            IntAABB2 playerAABB = collisionIterator.next();

            WillFallResult willFallResult = willFall(playerAABB, feetBlockY - 1, bsi);
            if (willFallResult == WillFallResult.NO) {
                // continue looping until we find a fall
                continue;
            } else if (willFallResult == WillFallResult.UNSUPPORTED_TERRAIN) {
                return new NextFallResult(false);
            }

            return getLandingBlock(playerAABB, feetBlockY, bsi)
                    .map(b -> new NextFallResult(playerAABB, b))
                    .orElseGet(() -> {
                        // void or unsupported blocks
                        return new NextFallResult(false);
                    });
        }

        return new NextFallResult(true);
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
        int currentY = startY;

        while (currentY >= 0) {
            FallHelper.WillFallResult result = FallHelper.willFall(playerAABB, currentY - 1, bsi);
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
            this.isPathStillValid = false;
            this.fallBox = fallBox;
            this.floorBlockY = floorBlockY;
        }

        boolean isPathStillValid() {
            return isPathStillValid;
        }

        Optional<IntAABB2> getFallBox() {
            return Optional.ofNullable(fallBox);
        }

        int getFloorBlockY() {
            if (fallBox == null) {
                throw new IllegalStateException("no fall but tried to get the floor block Y");
            }

            return this.floorBlockY;
        }
    }

}
