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

        int floorBlockY = (int) Math.floor(playerFeet.y - 1.0);

        // predict the next few positions
        GridCollisionIterator collisionIterator = new GridCollisionIterator(player.width, playerPosXZ, destXZ);
        for (int i = 0; i < 10 && collisionIterator.hasNext(); i++) {
            IntAABB2 playerAABB = collisionIterator.next();

            WillFallResult willFallResult = willFall(playerAABB, floorBlockY, bsi);
            if (willFallResult == WillFallResult.NO) {
                // continue looping until we find a fall
                continue;
            } else if (willFallResult == WillFallResult.UNSUPPORTED_TERRAIN) {
                return new NextFallResult(false);
            }

            // Loop until we find something that the player can land on.
            while (willFallResult == WillFallResult.YES) {
                floorBlockY--;

                if (floorBlockY < 0) {
                    // fall into void?
                    return new NextFallResult(false);
                }

                willFallResult = willFall(playerAABB, floorBlockY, bsi);
                if (willFallResult == WillFallResult.UNSUPPORTED_TERRAIN) {
                    return new NextFallResult(false);
                }
            }

            // We know that there are all blocks on this AABB are
            // fully passable because we just did the simulation,
            // so if the player stays there, then they will be able
            // to fall without hitting any block during the path.
            // TODO: try to extend this box as much as possible to
            //  limit constraints by checking for blocks
            //  around too
            return new NextFallResult(playerAABB, floorBlockY);
        }

        return new NextFallResult(true);
    }

    /**
     * Gets the block that the player will land on after a fall.
     * If any unsupported blocks are encountered, returns
     * {@code Optional.empty()}.
     *
     * @param start - the starting fall position
     * @param bsi - the BSI
     * @return the block that will player will land on
     */
    static Optional<BetterBlockPos> getLandingBlock(BetterBlockPos start, BlockStateInterface bsi) {
        BetterBlockPos current = start.down();

        while (true) {
            FallHelper.WillFallResult result = FallHelper.willFall(current, bsi);
            if (result == FallHelper.WillFallResult.NO) {
                return Optional.of(current);
            } else if (result == FallHelper.WillFallResult.UNSUPPORTED_TERRAIN) {
                return Optional.empty();
            }

            current = current.down();

            if (current.y < 0) {
                // fall into void?
                return Optional.empty();
            }
        }
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
