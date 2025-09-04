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

package baritone.process.elytra;

import baritone.Baritone;
import baritone.api.event.events.BlockChangeEvent;
import baritone.api.pathing.elytra.IElytraPathfinderContext;
import baritone.api.pathing.elytra.UnpackedSegment;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.IPlayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.levelgen.Heightmap;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class SkyPathfinderContext implements IElytraPathfinderContext {
    NetherPathfinderContext netherCtx;
    IPlayerContext playerCtx;
    final int flightLevel;

    public SkyPathfinderContext(IPlayerContext ctx, Path cacheDir) {
        if (ctx == null) {
            throw new IllegalArgumentException("IPlayerContext cannot be null");
        }

        this.netherCtx = new NetherPathfinderContextFactory().create(ctx, cacheDir);
        this.playerCtx = ctx;
        this.flightLevel = playerCtx.world().getMaxBuildHeight() + 16;

        if(netherCtx.getMaxHeight() + playerCtx.world().getMinBuildHeight() < playerCtx.world().getMaxBuildHeight()) {
            throw new IllegalStateException("Nether pathfinder max height is below world build limit, cannot proceed");
        }
    }

    @Override
    public boolean hasChunk(ChunkPos pos) {
        return netherCtx.hasChunk(pos);
    }

    @Override
    public void queueCacheCulling(int chunkX, int chunkZ, int maxDistanceBlocks) {
        netherCtx.queueCacheCulling(chunkX, chunkZ, maxDistanceBlocks);
    }

    @Override
    public void queueForPacking(LevelChunk chunkIn) {
        netherCtx.queueForPacking(chunkIn);
    }

    @Override
    public void queueBlockUpdate(BlockChangeEvent event) {
        netherCtx.queueBlockUpdate(event);
    }


    /**
     * Generates a direct path from the start to the destination at a fixed y-level above build limit
     * @param start
     * @param destination
     * @param bufferDistance Distance from the destination to halt the direct path
     * @param maxPathSize Maximum number of nodes in the returned path
     * @return A tuple containing the path as a list of BetterBlockPos and a boolean indicating if the path is complete
     */
    public Tuple<List<BetterBlockPos>, Boolean> GenerateDirectPath(BetterBlockPos start, BetterBlockPos destination, int bufferDistance, int maxPathSize) {
        final LinkedList<BetterBlockPos> path = new LinkedList<>();
        final int stepDistance = 32;

        start = start.y == flightLevel ? start : new BetterBlockPos(start.getX(), flightLevel, start.getZ());
        destination = destination.y == flightLevel ? destination : new BetterBlockPos(destination.getX(), flightLevel, destination.getZ());

        BetterBlockPos cur = start;
        path.add(cur);

        while (path.size() < maxPathSize) {
            double deltaX = destination.getX() - cur.getX();
            double deltaZ = destination.getZ() - cur.getZ();
            double remainingDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
            double remainingDistanceSq = deltaX * deltaX + deltaZ * deltaZ;

            if(remainingDistanceSq <= bufferDistance * bufferDistance) {
                // We are within the buffer distance, so we can stop here
                return new Tuple<>(path, true);
            } else if (remainingDistance <= stepDistance) {
                path.add(destination);
                return new Tuple<>(path, true);
            }

            double stepRatio = stepDistance / remainingDistance;
            int nextX = (int) Math.round(cur.getX() + deltaX * stepRatio);
            int nextZ = (int) Math.round(cur.getZ() + deltaZ * stepRatio);

            cur = new BetterBlockPos(nextX, flightLevel, nextZ);
            path.add(cur);
        }

        return new Tuple<>(path, false);
    }

    /**
     * Attempts to find an open spot in the sky to transition up above the build limit so a simple direct path can be followed
     * @param start
     * @param destination
     * @return A tuple containing the path that transitions above build limit and a boolean indicating if a transition was found
     */
    public Tuple<List<BetterBlockPos>,Boolean> GenerateTransitionUp(BetterBlockPos start, BetterBlockPos destination) {
        final double deltaX = destination.getX() - start.getX();
        final double deltaZ = destination.getZ() - start.getZ();
        final double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        final double scale = 8 / distance;
        final double stepX = deltaX * scale;
        final double stepZ = deltaZ * scale;

        final int netherMaxHeight = netherCtx.getMaxHeight() + playerCtx.world().getMinBuildHeight() - 1;

        final ChunkPos startChunk = new ChunkPos(start.x >> 4, start.z >> 4);

        if(!isSkyClear(startChunk, start.y)) {
            return new Tuple<>(new LinkedList<>(), false);
        }

        LinkedList<BetterBlockPos> path = new LinkedList<>();

        // Start with the middle block so the transition doesn't leave the only chunk we can confirm is clear
        final BlockPos middlePos = startChunk.getMiddleBlockPosition(netherMaxHeight+4);

        for(int i = 2; i <= 2; i++) {
            BetterBlockPos next = new BetterBlockPos(
                    (int) (middlePos.getX() + (stepX * i)),
                    netherMaxHeight + (i * 8),
                    (int) (middlePos.getZ() + (stepZ * i))
            );
            path.add(next);
        }

        return new Tuple<>(path, true);
    }

    /**
     * Attempts to find an open spot in the sky to transition down to a flight level the nether pathfinder can navigiate at.
     * @param start
     * @return A tuple containing the path (single point) and a boolean indicating if a transition point was found
     */
    public Tuple<List<BetterBlockPos>,Boolean> GenerateTransitionDown(BetterBlockPos start) {
        final int netherMaxHeight = netherCtx.getMaxHeight() + playerCtx.world().getMinBuildHeight() - 1;
        final ChunkPos startChunk = new ChunkPos(start.x >> 4, start.z >> 4);

        LinkedList<BetterBlockPos> path = new LinkedList<>();

        if(!isSkyClear(new ChunkPos(start.x >> 4, start.z >> 4), netherMaxHeight-16)) {
            return new Tuple<>(new LinkedList<>(), false);
        }

        path.add(new BetterBlockPos(startChunk.getMiddleBlockPosition(netherMaxHeight-8)));
        return new Tuple<>(path, true);
    }

    public boolean isSkyClear(ChunkPos pos, int y) {
        if(!playerCtx.world().getChunkSource().hasChunk(pos.x, pos.z)) {
            return false;
        }

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                BlockPos blockPos = pos.getBlockAt(x, y, z);
                int height = playerCtx.world().getHeight(Heightmap.Types.MOTION_BLOCKING, blockPos.getX(), blockPos.getZ());
                if (height > y) {
                    return false;
                }
            }
        }
        return true;
    }


    @Override
    public CompletableFuture<UnpackedSegment> pathFindAsync(BlockPos src, BlockPos dst) {
        final int netherMaxHeight = netherCtx.getMaxHeight() + playerCtx.world().getMinBuildHeight() - 1;
        final int maxDirectPathSize = 500;

        // There can be some navigation issues around failed transitions if the threshold distance isn't large enough
        final double maxDistance = Baritone.settings().elytraLongDistanceThreshold.value >= 32 ? (double) Baritone.settings().elytraLongDistanceThreshold.value : Double.POSITIVE_INFINITY;

        final double distanceXZ = src.distSqr(new Vec3i(dst.getX(), src.getY(), dst.getZ()));
        final boolean isLongDistance = distanceXZ > maxDistance * maxDistance;
        final boolean srcAboveSupportedHeight = src.getY() >= netherMaxHeight;
        final boolean dstAboveSupportedHeight = dst.getY() >= netherMaxHeight;

        if(srcAboveSupportedHeight && dstAboveSupportedHeight) {
            var path = GenerateDirectPath(new BetterBlockPos(src), new BetterBlockPos(dst), 0, maxDirectPathSize);
            return CompletableFuture.completedFuture(new UnpackedSegment(path.getA().stream(), path.getB()));
        }

        if(isLongDistance) {
            if(srcAboveSupportedHeight) {
                var directPath = GenerateDirectPath(new BetterBlockPos(src), new BetterBlockPos(dst), (int)maxDistance, maxDirectPathSize);
                return CompletableFuture.completedFuture(
                        new UnpackedSegment(
                                directPath.getA().stream(),
                                dstAboveSupportedHeight ? directPath.getB() : false
                        )
                );
            } else {
                var transition = GenerateTransitionUp(new BetterBlockPos(src), new BetterBlockPos(dst));
                var path = transition.getA();
                var success = transition.getB();

                if(success) {
                    var directPath = GenerateDirectPath(path.get(path.size()-1), new BetterBlockPos(dst), (int)maxDistance, maxDirectPathSize);
                    path.addAll(directPath.getA());

                    return CompletableFuture.completedFuture(
                        new UnpackedSegment(
                            path.stream(),
                            dstAboveSupportedHeight? directPath.getB() : false
                        )
                    );
                }
            }

            // Failed to find a transition point so navigate a bit in the right direction and try
            final double deltaX = dst.getX() - src.getX();
            final double deltaZ = dst.getZ() - src.getZ();
            final double scale = (maxDistance/2) / Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
            final double stepX = deltaX * scale;
            final double stepZ = deltaZ * scale;
            final BlockPos midDst = new BlockPos((int)(src.getX() + stepX), netherMaxHeight, (int)(src.getZ() + stepZ));

            return incompletePathfind(src, midDst);
        } else {
            if(srcAboveSupportedHeight) {
                var transition = GenerateTransitionDown(new BetterBlockPos(src));
                List<BetterBlockPos> path = transition.getA();
                boolean success = transition.getB();

                if(!success) {
                    BetterBlockPos newDest = distanceXZ > 32 ? new BetterBlockPos(dst) : new BetterBlockPos(dst.getX(), playerCtx.world().getMaxBuildHeight(), dst.getZ());
                    var directPath = GenerateDirectPath(new BetterBlockPos(src), newDest, 0, 2);
                    return CompletableFuture.completedFuture(new UnpackedSegment(directPath.getA().stream(), directPath.getB()));
                }

                return CompletableFuture.supplyAsync(() -> {
                    try {
                        var np = netherCtx.pathFindAsync(path.get(path.size() - 1), dst).get();
                        path.addAll(np.collect());
                        return new UnpackedSegment(path.stream(), np.isFinished());
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                });
            }


            if(dstAboveSupportedHeight) {
                var transition = GenerateTransitionUp(new BetterBlockPos(src), new BetterBlockPos(dst));
                var path = transition.getA();
                var success = transition.getB();

                if(success) {
                    var directPath = GenerateDirectPath(path.get(path.size() - 1), new BetterBlockPos(dst), 0, maxDirectPathSize);
                    path.addAll(directPath.getA());
                    return CompletableFuture.completedFuture(new UnpackedSegment(path.stream(), directPath.getB()));
                }

                return netherCtx.pathFindAsync(src, new BetterBlockPos(dst.getX(), netherMaxHeight, dst.getZ()));
            }

            return netherCtx.pathFindAsync(src, dst);
        }
    }

    /**
     * A wrapper for a nether pathfinder call but the returned path will always indicate it is incomplete
     * @param src
     * @param dst
     * @return a CompletableFuture containing an UnpackedSegment with isFinished always false
     */
    private CompletableFuture<UnpackedSegment> incompletePathfind(BlockPos src, BlockPos dst) {
        return CompletableFuture.supplyAsync(() -> {
            UnpackedSegment packed;
            try {
                packed = netherCtx.pathFindAsync(src, dst).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
            return new UnpackedSegment(
                    packed.collect().stream(),
                    false
            );
        });
    }

    @Override
    public boolean raytrace(double startX, double startY, double startZ, double endX, double endY, double endZ) {
        final int maxHeight = netherCtx.getMaxHeight() + playerCtx.world().getMinBuildHeight();
        final int minHeight = playerCtx.world().getMinBuildHeight();
        final boolean isOOB = startY >= maxHeight || endY >= maxHeight || startY < minHeight || endY < minHeight;
        if (isOOB) {
            Vec3 start = new Vec3(startX, startY, startZ);
            Vec3 end = new Vec3(endX, endY, endZ);
            return playerCtx.world().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, playerCtx.player())).getType() == HitResult.Type.MISS;
        }

        return netherCtx.raytrace(startX, startY, startZ, endX, endY, endZ);
    }

    @Override
    public boolean raytrace(Vec3 start, Vec3 end) {
        final int maxHeight = netherCtx.getMaxHeight() + playerCtx.world().getMinBuildHeight();
        final int minHeight = playerCtx.world().getMinBuildHeight();
        final boolean isOOB = start.y >= maxHeight || end.y >= maxHeight || start.y < minHeight || end.y < minHeight;
        if (isOOB) {
            return playerCtx.world().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, playerCtx.player())).getType() == HitResult.Type.MISS;
        }
        return netherCtx.raytrace(start.x, start.y, start.z, end.x, end.y, end.z);
    }


    @Override
    public boolean raytrace(int count, double[] src, double[] dst, int visibility) {
        if (src.length != count * 3 || src.length != dst.length) {
            throw new IllegalArgumentException("Expected source and dst to have length of " + (count * 3));
        }
        final int maxHeight = netherCtx.getMaxHeight() + playerCtx.world().getMinBuildHeight();

        boolean isOOB = false;
        for(int i = 1; i < src.length; i += 3) {
            if (src[i] >= maxHeight || src[i] < playerCtx.world().getMinBuildHeight() ||
                    dst[i] >= maxHeight || dst[i] < playerCtx.world().getMinBuildHeight()) {
                isOOB = true;
                break;
            }
        }

        if(isOOB) {
            for (int i = 0; i < count; i++) {
                Vec3 start = new Vec3(src[i * 3], src[i * 3 + 1], src[i * 3 + 2]);
                Vec3 end = new Vec3(dst[i * 3], dst[i * 3 + 1], dst[i * 3 + 2]);
                if (playerCtx.world().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, playerCtx.player())).getType() != HitResult.Type.MISS) {
                    return false;
                }
            }
            return true;
        }

        return netherCtx.raytrace(count, src, dst, visibility);
    }

    @Override
    public void cancel() {
        netherCtx.cancel();
    }

    @Override
    public void destroy() {
        netherCtx.destroy();
    }

    @Override
    public long getSeed() {
        return netherCtx.getSeed();
    }

    @Override
    public void RLock() {
        netherCtx.RLock();
    }

    @Override
    public void RUnlock() {
        netherCtx.RUnlock();
    }

    @Override
    public int getMaxHeight() {
        return netherCtx.getMaxHeight();
    }

    @Override
    public boolean passable(int x, int y, int z) {
        if(y >= playerCtx.world().getMaxBuildHeight() || y < playerCtx.world().getMinBuildHeight()) {
            return true;
        }
        return netherCtx.passable(x, y, z);
    }
}
