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

package baritone.behavior;

import baritone.Baritone;
import baritone.api.event.events.ChunkEvent;
import baritone.api.event.events.TickEvent;
import baritone.api.event.events.type.EventState;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.Helper;
import baritone.api.utils.Rotation;
import baritone.api.utils.RotationUtils;
import baritone.behavior.elytra.NetherPathfinderContext;
import baritone.behavior.elytra.UnpackedSegment;
import baritone.utils.BlockStateInterface;
import com.mojang.realmsclient.util.Pair;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityFireworkRocket;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.*;
import net.minecraft.world.chunk.Chunk;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.UnaryOperator;

public final class ElytraBehavior extends Behavior implements Helper {

    /**
     * 2b2t seed
     */
    private static final long NETHER_SEED = 146008555100680L;

    // Used exclusively for PathRenderer
    public List<Pair<Vec3d, Vec3d>> lines;
    public BlockPos aimPos;
    public List<BetterBlockPos> visiblePath;

    // :sunglasses:
    private final NetherPathfinderContext context;
    private final PathManager pathManager;
    private int sinceFirework;

    public ElytraBehavior(Baritone baritone) {
        super(baritone);
        this.context = new NetherPathfinderContext(NETHER_SEED);
        this.lines = new ArrayList<>();
        this.visiblePath = Collections.emptyList();
        this.pathManager = new PathManager();
    }

    private final class PathManager {

        private BlockPos destination;
        private List<BetterBlockPos> path;
        private boolean completePath;

        private int playerNear;
        private int goingTo;

        private boolean recalculating;

        public PathManager() {
            // lol imagine initializing fields normally
            this.clear();
        }

        public void tick() {
            // Recalculate closest path node
            this.playerNear = this.calculateNear(this.playerNear);

            // Obstacles are more important than an incomplete path, handle those first.
            this.pathfindAroundObstacles();
            this.attemptNextSegment();
        }

        public void pathToDestination(BlockPos destination) {
            this.destination = destination;
            this.path0(ctx.playerFeet(), destination, UnaryOperator.identity())
                    .thenRun(() -> {
                        final int distance = (int) this.pathAt(0).distanceTo(this.pathAt(this.path.size() - 1));
                        if (this.completePath) {
                            logDirect(String.format("Computed path (%d blocks)", distance));
                        } else {
                            logDirect(String.format("Computed segment (Next %d blocks)", distance));
                        }
                    })
                    .whenComplete((result, ex) -> {
                        this.recalculating = false;
                        if (ex != null) {
                            logDirect("Failed to compute path to destination");
                        }
                    });
        }

        public void pathRecalcSegment(final int upToIncl) {
            if (this.recalculating) {
                return;
            }

            this.recalculating = true;
            final List<BetterBlockPos> after = this.path.subList(upToIncl, this.path.size());
            final boolean complete = this.completePath;

            this.path0(ctx.playerFeet(), this.path.get(upToIncl), segment -> segment.append(after.stream(), complete))
                    .thenRun(() -> {
                        final int recompute = this.path.size() - after.size() - 1;
                        final int distance = (int) this.pathAt(0).distanceTo(this.pathAt(recompute));
                        logDirect(String.format("Recomputed segment (Next %d blocks)", distance));
                    })
                    .whenComplete((result, ex) -> {
                        this.recalculating = false;
                        if (ex != null) {
                            logDirect("Failed to recompute segment");
                        }
                    });
        }

        public void pathNextSegment(final int afterIncl) {
            if (this.recalculating) {
                return;
            }

            this.recalculating = true;
            final List<BetterBlockPos> before = this.path.subList(0, afterIncl + 1);

            this.path0(this.path.get(afterIncl), this.destination, segment -> segment.prepend(before.stream()))
                    .thenRun(() -> {
                        final int recompute = this.path.size() - before.size() - 1;
                        final int distance = (int) this.pathAt(0).distanceTo(this.pathAt(recompute));

                        if (this.completePath) {
                            logDirect(String.format("Computed path (%d blocks)", distance));
                        } else {
                            logDirect(String.format("Computed next segment (Next %d blocks)", distance));
                        }
                    })
                    .whenComplete((result, ex) -> {
                        this.recalculating = false;
                        if (ex != null) {
                            logDirect("Failed to compute next segment");
                        }
                    });
        }

        private Vec3d pathAt(int i) {
            return new Vec3d(
                    this.path.get(i).x,
                    this.path.get(i).y,
                    this.path.get(i).z
            );
        }

        public void clear() {
            this.path = Collections.emptyList();
            this.playerNear = 0;
            this.goingTo = 0;
            this.completePath = true;
        }

        private void setPath(final UnpackedSegment segment) {
            this.path = segment.collect();
            this.removeBacktracks();
            this.playerNear = 0;
            this.goingTo = 0;
            this.completePath = segment.isFinished();
        }

        public List<BetterBlockPos> getPath() {
            return this.path;
        }

        public int getNear() {
            return this.playerNear;
        }

        public void setGoingTo(int index) {
            this.goingTo = index;
        }

        public BetterBlockPos goingTo() {
            return this.path.get(this.goingTo);
        }

        // mickey resigned
        private CompletableFuture<Void> path0(BlockPos src, BlockPos dst, UnaryOperator<UnpackedSegment> operator) {
            return ElytraBehavior.this.context.pathFindAsync(src, dst)
                    .thenApply(UnpackedSegment::from)
                    .thenApply(operator)
                    .thenAcceptAsync(this::setPath, ctx.minecraft()::addScheduledTask);
        }

        private void pathfindAroundObstacles() {
            if (this.recalculating) {
                return;
            }

            outer:
            while (true) {
                int rangeStartIncl = playerNear;
                int rangeEndExcl = playerNear;
                while (rangeEndExcl < path.size() && ctx.world().isBlockLoaded(path.get(rangeEndExcl), false)) {
                    rangeEndExcl++;
                }
                if (rangeStartIncl >= rangeEndExcl) {
                    // not loaded yet?
                    return;
                }
                if (!passable(ctx.world().getBlockState(path.get(rangeStartIncl)))) {
                    // we're in a wall
                    return; // previous iterations of this function SHOULD have fixed this by now :rage_cat:
                }
                for (int i = rangeStartIncl; i < rangeEndExcl - 1; i++) {
                    if (!clearView(pathAt(i), pathAt(i + 1))) {
                        // obstacle. where do we return to pathing?
                        // find the next valid segment
                        this.pathRecalcSegment(rangeEndExcl - 1);
                        break outer;
                    }
                }
                break;
            }
        }

        private void attemptNextSegment() {
            if (this.recalculating) {
                return;
            }

            final int last = this.path.size() - 1;
            if (!this.completePath && ctx.world().isBlockLoaded(this.path.get(last), false)) {
                this.pathNextSegment(last);
            }
        }

        private int calculateNear(int index) {
            final BetterBlockPos pos = ctx.playerFeet();
            for (int i = index; i >= Math.max(index - 1000, 0); i -= 10) {
                if (path.get(i).distanceSq(pos) < path.get(index).distanceSq(pos)) {
                    index = i; // intentional: this changes the bound of the loop
                }
            }
            for (int i = index; i < Math.min(index + 1000, path.size()); i += 10) {
                if (path.get(i).distanceSq(pos) < path.get(index).distanceSq(pos)) {
                    index = i; // intentional: this changes the bound of the loop
                }
            }
            for (int i = index; i >= Math.max(index - 50, 0); i--) {
                if (path.get(i).distanceSq(pos) < path.get(index).distanceSq(pos)) {
                    index = i; // intentional: this changes the bound of the loop
                }
            }
            for (int i = index; i < Math.min(index + 50, path.size()); i++) {
                if (path.get(i).distanceSq(pos) < path.get(index).distanceSq(pos)) {
                    index = i; // intentional: this changes the bound of the loop
                }
            }
            return index;
        }

        private void removeBacktracks() {
            Map<BetterBlockPos, Integer> positionFirstSeen = new HashMap<>();
            for (int i = 0; i < this.path.size(); i++) {
                BetterBlockPos pos = this.path.get(i);
                if (positionFirstSeen.containsKey(pos)) {
                    int j = positionFirstSeen.get(pos);
                    while (i > j) {
                        this.path.remove(i);
                        i--;
                    }
                } else {
                    positionFirstSeen.put(pos, i);
                }
            }
        }
    }

    @Override
    public void onChunkEvent(ChunkEvent event) {
        if (event.getState() == EventState.POST && event.getType().isPopulate()) {
            final Chunk chunk = ctx.world().getChunk(event.getX(), event.getZ());
            this.context.queueForPacking(chunk);
        }
    }

    public void path(BlockPos destination) {
        this.pathManager.pathToDestination(destination);
    }

    public void cancel() {
        this.visiblePath = Collections.emptyList();
        this.pathManager.clear();
        this.aimPos = null;
        this.sinceFirework = 0;
    }

    @Override
    public void onTick(TickEvent event) {
        if (event.getType() == TickEvent.Type.OUT) {
            return;
        }
        this.lines.clear();

        final List<BetterBlockPos> path = this.pathManager.getPath();
        if (path.isEmpty()) {
            return;
        }

        this.pathManager.tick();

        final int playerNear = this.pathManager.getNear();
        this.visiblePath = path.subList(
                Math.max(playerNear - 30, 0),
                Math.min(playerNear + 100, path.size())
        );

        if (!ctx.player().isElytraFlying()) {
            return;
        }

        baritone.getInputOverrideHandler().clearAllKeys();

        if (ctx.player().collidedHorizontally) {
            logDirect("hbonk");
        }
        if (ctx.player().collidedVertically) {
            logDirect("vbonk");
        }

        Vec3d start = ctx.playerFeetAsVec();
        boolean firework = isFireworkActive();
        sinceFirework++;
        final long t = System.currentTimeMillis();
        outermost:
        for (int relaxation = 0; relaxation < 3; relaxation++) { // try for a strict solution first, then relax more and more (if we're in a corner or near some blocks, it will have to relax its constraints a bit)
            int[] heights = firework ? new int[]{20, 10, 5, 0} : new int[]{0}; // attempt to gain height, if we can, so as not to waste the boost
            int steps = relaxation < 2 ? firework ? 5 : Baritone.settings().elytraSimulationTicks.value : 3;
            int lookahead = relaxation == 0 ? 2 : 3; // ideally this would be expressed as a distance in blocks, rather than a number of voxel steps
            //int minStep = Math.max(0, playerNear - relaxation);
            int minStep = playerNear;
            for (int i = Math.min(playerNear + 20, path.size() - 1); i >= minStep; i--) {
                for (int dy : heights) {
                    Vec3d dest = this.pathManager.pathAt(i).add(0, dy, 0);
                    if (dy != 0) {
                        if (i + lookahead >= path.size()) {
                            continue;
                        }
                        if (start.distanceTo(dest) < 40) {
                            if (!clearView(dest, this.pathManager.pathAt(i + lookahead).add(0, dy, 0)) || !clearView(dest, this.pathManager.pathAt(i + lookahead))) {
                                // aka: don't go upwards if doing so would prevent us from being able to see the next position **OR** the modified next position
                                continue;
                            }
                        } else {
                            // but if it's far away, allow gaining altitude if we could lose it again by the time we get there
                            if (!clearView(dest, this.pathManager.pathAt(i))) {
                                continue;
                            }
                        }
                    }

                    // 1.0 -> 0.25 -> none
                    final Double grow = relaxation == 2 ? null
                            : relaxation == 0 ? 1.0d : 0.25d;

                    if (isClear(start, dest, grow)) {
                        Rotation rot = RotationUtils.calcRotationFromVec3d(start, dest, ctx.playerRotations());
                        long a = System.currentTimeMillis();
                        Float pitch = solvePitch(dest.subtract(start), steps, relaxation == 2);
                        if (pitch == null) {
                            baritone.getLookBehavior().updateTarget(new Rotation(rot.getYaw(), ctx.playerRotations().getPitch()), false);
                            continue;
                        }
                        long b = System.currentTimeMillis();
                        //System.out.println("Solved pitch in " + (b - a) + " total time " + (b - t));
                        this.pathManager.setGoingTo(i);
                        this.aimPos = path.get(i).add(0, dy, 0);
                        baritone.getLookBehavior().updateTarget(new Rotation(rot.getYaw(), pitch), false);
                        break outermost;
                    }
                }
            }
            if (relaxation == 2) {
                logDirect("no pitch solution, probably gonna crash in a few ticks LOL!!!");
                return;
            }
        }

        final BetterBlockPos goingTo = this.pathManager.goingTo();

        if (!firework
                && sinceFirework > 10
                && (Baritone.settings().wasteFireworks.value || ctx.player().posY < goingTo.y + 5) // don't firework if trying to descend
                && (ctx.player().posY < goingTo.y - 5 || start.distanceTo(new Vec3d(goingTo.x + 0.5, ctx.player().posY, goingTo.z + 0.5)) > 5) // UGH!!!!!!!
                && new Vec3d(ctx.player().motionX, ctx.player().posY < goingTo.y ? Math.max(0, ctx.player().motionY) : ctx.player().motionY, ctx.player().motionZ).length() < Baritone.settings().elytraFireworkSpeed.value // ignore y component if we are BOTH below where we want to be AND descending
        ) {
            logDirect("firework");
            ctx.playerController().processRightClick(ctx.player(), ctx.world(), EnumHand.MAIN_HAND);
            sinceFirework = 0;
        }
    }

    private boolean isFireworkActive() {
        // TODO: Validate that the EntityFireworkRocket is attached to ctx.player()
        return ctx.world().loadedEntityList.stream()
                .anyMatch(x -> (x instanceof EntityFireworkRocket) && ((EntityFireworkRocket) x).isAttachedToEntity());
    }

    private boolean isClear(final Vec3d start, final Vec3d dest, final Double growAmount) {
        if (!clearView(start, dest)) {
            return false;
        }
        if (growAmount == null) {
            return true;
        }

        final AxisAlignedBB bb = ctx.player().getEntityBoundingBox().grow(growAmount);
        final Vec3d[] corners = new Vec3d[]{
                new Vec3d(bb.minX, bb.minY, bb.minZ),
                new Vec3d(bb.minX, bb.minY, bb.maxZ),
                new Vec3d(bb.minX, bb.maxY, bb.minZ),
                new Vec3d(bb.minX, bb.maxY, bb.maxZ),
                new Vec3d(bb.maxX, bb.minY, bb.minZ),
                new Vec3d(bb.maxX, bb.minY, bb.maxZ),
                new Vec3d(bb.maxX, bb.maxY, bb.minZ),
                new Vec3d(bb.maxX, bb.maxY, bb.maxZ),
        };

        for (final Vec3d corner : corners) {
            if (!clearView(corner, dest.add(corner.subtract(start)))) {
                return false;
            }
        }
        return true;
    }

    private boolean clearView(Vec3d start, Vec3d dest) {
        lines.add(Pair.of(start, dest));
        RayTraceResult result = rayTraceBlocks(start, dest);
        return result == null || result.typeOfHit == RayTraceResult.Type.MISS;
    }

    private Float solvePitch(Vec3d goalDirection, int steps, boolean desperate) {
        // we are at a certain velocity, but we have a target velocity
        // what pitch would get us closest to our target velocity?
        // yaw is easy so we only care about pitch

        goalDirection = goalDirection.normalize();
        Rotation good = RotationUtils.calcRotationFromVec3d(new Vec3d(0, 0, 0), goalDirection, ctx.playerRotations()); // lazy lol

        boolean firework = isFireworkActive();
        Float bestPitch = null;
        double bestDot = Double.NEGATIVE_INFINITY;
        Vec3d motion = new Vec3d(ctx.player().motionX, ctx.player().motionY, ctx.player().motionZ);
        BlockStateInterface bsi = new BlockStateInterface(ctx);
        float minPitch = desperate ? -90 : Math.max(good.getPitch() - Baritone.settings().elytraPitchRange.value, -89);
        float maxPitch = desperate ? 90 : Math.min(good.getPitch() + Baritone.settings().elytraPitchRange.value, 89);
        outer:
        for (float pitch = minPitch; pitch <= maxPitch; pitch++) {
            Vec3d stepped = motion;
            Vec3d totalMotion = new Vec3d(0, 0, 0);
            for (int i = 0; i < steps; i++) {
                stepped = step(stepped, pitch, good.getYaw(), firework);
                Vec3d actualPositionPrevTick = ctx.playerFeetAsVec().add(totalMotion);
                totalMotion = totalMotion.add(stepped);
                Vec3d actualPosition = ctx.playerFeetAsVec().add(totalMotion);
                for (int x = MathHelper.floor(Math.min(actualPosition.x, actualPositionPrevTick.x) - 0.31); x <= Math.max(actualPosition.x, actualPositionPrevTick.x) + 0.31; x++) {
                    for (int y = MathHelper.floor(Math.min(actualPosition.y, actualPositionPrevTick.y) - 0.2); y <= Math.max(actualPosition.y, actualPositionPrevTick.y) + 1; y++) {
                        for (int z = MathHelper.floor(Math.min(actualPosition.z, actualPositionPrevTick.z) - 0.31); z <= Math.max(actualPosition.z, actualPositionPrevTick.z) + 0.31; z++) {
                            if (!passable(bsi.get0(x, y, z))) {
                                continue outer;
                            }
                        }
                    }
                }
            }
            double directionalGoodness = goalDirection.dotProduct(totalMotion.normalize());
            // tried to incorporate a "speedGoodness" but it kept making it do stupid stuff (aka always losing altitude)
            double goodness = directionalGoodness;
            if (goodness > bestDot) {
                bestDot = goodness;
                bestPitch = pitch;
            }
        }
        return bestPitch;
    }

    public static boolean passable(IBlockState state) {
        return state.getMaterial() == Material.AIR;
    }

    private static Vec3d step(Vec3d motion, float rotationPitch, float rotationYaw, boolean firework) {
        double motionX = motion.x;
        double motionY = motion.y;
        double motionZ = motion.z;
        float flatZ = MathHelper.cos(-rotationYaw * 0.017453292F - (float) Math.PI); // 0.174... is Math.PI / 180
        float flatX = MathHelper.sin(-rotationYaw * 0.017453292F - (float) Math.PI);
        float pitchBase = -MathHelper.cos(-rotationPitch * 0.017453292F);
        float pitchHeight = MathHelper.sin(-rotationPitch * 0.017453292F);
        Vec3d lookDirection = new Vec3d(flatX * pitchBase, pitchHeight, flatZ * pitchBase);

        if (firework) {
            // See EntityFireworkRocket
            motionX += lookDirection.x * 0.1 + (lookDirection.x * 1.5 - motionX) * 0.5;
            motionY += lookDirection.y * 0.1 + (lookDirection.y * 1.5 - motionY) * 0.5;
            motionZ += lookDirection.z * 0.1 + (lookDirection.z * 1.5 - motionZ) * 0.5;
        }

        float pitchRadians = rotationPitch * 0.017453292F;
        double pitchBase2 = Math.sqrt(lookDirection.x * lookDirection.x + lookDirection.z * lookDirection.z);
        double flatMotion = Math.sqrt(motionX * motionX + motionZ * motionZ);
        double thisIsAlwaysOne = lookDirection.length();
        float pitchBase3 = MathHelper.cos(pitchRadians);
        //System.out.println("always the same lol " + -pitchBase + " " + pitchBase3);
        //System.out.println("always the same lol " + Math.abs(pitchBase3) + " " + pitchBase2);
        //System.out.println("always 1 lol " + thisIsAlwaysOne);
        pitchBase3 = (float) ((double) pitchBase3 * (double) pitchBase3 * Math.min(1, thisIsAlwaysOne / 0.4));
        motionY += -0.08 + (double) pitchBase3 * 0.06;
        if (motionY < 0 && pitchBase2 > 0) {
            double speedModifier = motionY * -0.1 * (double) pitchBase3;
            motionY += speedModifier;
            motionX += lookDirection.x * speedModifier / pitchBase2;
            motionZ += lookDirection.z * speedModifier / pitchBase2;
        }
        if (pitchRadians < 0) { // if you are looking down (below level)
            double anotherSpeedModifier = flatMotion * (double) (-MathHelper.sin(pitchRadians)) * 0.04;
            motionY += anotherSpeedModifier * 3.2;
            motionX -= lookDirection.x * anotherSpeedModifier / pitchBase2;
            motionZ -= lookDirection.z * anotherSpeedModifier / pitchBase2;
        }
        if (pitchBase2 > 0) { // this is always true unless you are looking literally straight up (let's just say the bot will never do that)
            motionX += (lookDirection.x / pitchBase2 * flatMotion - motionX) * 0.1;
            motionZ += (lookDirection.z / pitchBase2 * flatMotion - motionZ) * 0.1;
        }
        motionX *= 0.99;
        motionY *= 0.98;
        motionZ *= 0.99;
        //System.out.println(motionX + " " + motionY + " " + motionZ);

        return new Vec3d(motionX, motionY, motionZ);
    }

    // TODO: Use the optimized version from builder-2
    private RayTraceResult rayTraceBlocks(Vec3d start, Vec3d end) {
        int x1 = MathHelper.floor(end.x);
        int y1 = MathHelper.floor(end.y);
        int z1 = MathHelper.floor(end.z);
        int x2 = MathHelper.floor(start.x);
        int y2 = MathHelper.floor(start.y);
        int z2 = MathHelper.floor(start.z);
        BlockPos blockpos = new BlockPos(x2, y2, z2);
        IBlockState iblockstate = ctx.world().getBlockState(blockpos);
        if (!passable(iblockstate)) {
            return Blocks.SPONGE.getDefaultState().collisionRayTrace(ctx.world(), blockpos, start, end);
        }
        int steps = 200;
        while (steps-- >= 0) {
            if (Double.isNaN(start.x) || Double.isNaN(start.y) || Double.isNaN(start.z)) {
                return null;
            }
            if (x2 == x1 && y2 == y1 && z2 == z1) {
                return null;
            }
            boolean hitX = true;
            boolean hitY = true;
            boolean hitZ = true;
            double nextX = 999.0D;
            double nextY = 999.0D;
            double nextZ = 999.0D;
            if (x1 > x2) {
                nextX = (double) x2 + 1.0D;
            } else if (x1 < x2) {
                nextX = (double) x2 + 0.0D;
            } else {
                hitX = false;
            }
            if (y1 > y2) {
                nextY = (double) y2 + 1.0D;
            } else if (y1 < y2) {
                nextY = (double) y2 + 0.0D;
            } else {
                hitY = false;
            }
            if (z1 > z2) {
                nextZ = (double) z2 + 1.0D;
            } else if (z1 < z2) {
                nextZ = (double) z2 + 0.0D;
            } else {
                hitZ = false;
            }
            double stepX = 999.0D;
            double stepY = 999.0D;
            double stepZ = 999.0D;
            double dirX = end.x - start.x;
            double dirY = end.y - start.y;
            double dirZ = end.z - start.z;
            if (hitX) {
                stepX = (nextX - start.x) / dirX;
            }
            if (hitY) {
                stepY = (nextY - start.y) / dirY;
            }
            if (hitZ) {
                stepZ = (nextZ - start.z) / dirZ;
            }
            if (stepX == -0.0D) {
                stepX = -1.0E-4D;
            }
            if (stepY == -0.0D) {
                stepY = -1.0E-4D;
            }
            if (stepZ == -0.0D) {
                stepZ = -1.0E-4D;
            }
            EnumFacing dir;
            if (stepX < stepY && stepX < stepZ) {
                dir = x1 > x2 ? EnumFacing.WEST : EnumFacing.EAST;
                start = new Vec3d(nextX, start.y + dirY * stepX, start.z + dirZ * stepX);
            } else if (stepY < stepZ) {
                dir = y1 > y2 ? EnumFacing.DOWN : EnumFacing.UP;
                start = new Vec3d(start.x + dirX * stepY, nextY, start.z + dirZ * stepY);
            } else {
                dir = z1 > z2 ? EnumFacing.NORTH : EnumFacing.SOUTH;
                start = new Vec3d(start.x + dirX * stepZ, start.y + dirY * stepZ, nextZ);
            }
            x2 = MathHelper.floor(start.x) - (dir == EnumFacing.EAST ? 1 : 0);
            y2 = MathHelper.floor(start.y) - (dir == EnumFacing.UP ? 1 : 0);
            z2 = MathHelper.floor(start.z) - (dir == EnumFacing.SOUTH ? 1 : 0);
            blockpos = new BlockPos(x2, y2, z2);
            IBlockState iblockstate1 = ctx.world().getBlockState(blockpos);
            if (!passable(iblockstate1)) {
                return Blocks.NETHERRACK.getDefaultState().collisionRayTrace(ctx.world(), blockpos, start, end);
            }
        }
        return null;
    }
}