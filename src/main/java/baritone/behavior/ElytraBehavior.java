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
import baritone.api.behavior.IElytraBehavior;
import baritone.api.event.events.BlockChangeEvent;
import baritone.api.event.events.ChunkEvent;
import baritone.api.event.events.PacketEvent;
import baritone.api.event.events.TickEvent;
import baritone.api.utils.*;
import baritone.behavior.elytra.NetherPathfinderContext;
import baritone.behavior.elytra.UnpackedSegment;
import baritone.utils.BlockStateInterface;
import baritone.utils.accessor.IEntityFireworkRocket;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityFireworkRocket;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketPlayerPosLook;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.*;
import net.minecraft.world.chunk.Chunk;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.UnaryOperator;

public final class ElytraBehavior extends Behavior implements IElytraBehavior, Helper {

    /**
     * 2b2t seed
     */
    private static final long NETHER_SEED = 146008555100680L;

    // Used exclusively for PathRenderer
    public List<Pair<Vec3d, Vec3d>> clearLines;
    public List<Pair<Vec3d, Vec3d>> blockedLines;
    public BlockPos aimPos;
    public List<BetterBlockPos> visiblePath;

    // :sunglasses:
    private final NetherPathfinderContext context;
    private final PathManager pathManager;
    private int remainingFireworkTicks;
    private int remainingSetBackTicks;
    private BlockStateInterface bsi;

    public ElytraBehavior(Baritone baritone) {
        super(baritone);
        this.context = new NetherPathfinderContext(NETHER_SEED);
        this.clearLines = new ArrayList<>();
        this.blockedLines = new ArrayList<>();
        this.visiblePath = Collections.emptyList();
        this.pathManager = new PathManager();
    }

    private final class PathManager {

        private BlockPos destination;
        private List<BetterBlockPos> path;
        private boolean completePath;
        private boolean recalculating;

        private int maxPlayerNear;
        private int ticksNearUnchanged;
        private int playerNear;

        public PathManager() {
            // lol imagine initializing fields normally
            this.clear();
        }

        public void tick() {
            // Recalculate closest path node
            final int prevMaxNear = this.maxPlayerNear;
            this.playerNear = this.calculateNear(this.playerNear);
            this.maxPlayerNear = Math.max(this.maxPlayerNear, this.playerNear);

            if (this.maxPlayerNear == prevMaxNear && ctx.player().isElytraFlying()) {
                this.ticksNearUnchanged++;
            } else {
                this.ticksNearUnchanged = 0;
            }

            // Obstacles are more important than an incomplete path, handle those first.
            this.pathfindAroundObstacles();
            this.attemptNextSegment();
        }

        public void pathToDestination(BlockPos destination) {
            this.destination = destination;
            final long start = System.nanoTime();
            this.path0(ctx.playerFeet(), destination, UnaryOperator.identity())
                    .thenRun(() -> {
                        final double distance = this.pathAt(0).distanceTo(this.pathAt(this.path.size() - 1));
                        if (this.completePath) {
                            logDirect(String.format("Computed path (%.1f blocks in %.4f seconds)", distance, (System.nanoTime() - start) / 1e9d));
                        } else {
                            logDirect(String.format("Computed segment (Next %.1f blocks in %.4f seconds)", distance, (System.nanoTime() - start) / 1e9d));
                        }
                    })
                    .whenComplete((result, ex) -> {
                        this.recalculating = false;
                        if (ex != null) {
                            logDirect("Failed to compute path to destination");
                        }
                    });
        }

        public CompletableFuture<Void> pathRecalcSegment(final int upToIncl) {
            if (this.recalculating) {
                throw new IllegalStateException("already recalculating");
            }

            this.recalculating = true;
            final List<BetterBlockPos> after = this.path.subList(upToIncl, this.path.size());
            final boolean complete = this.completePath;

            return this.path0(ctx.playerFeet(), this.path.get(upToIncl), segment -> segment.append(after.stream(), complete))
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
            final long start = System.nanoTime();

            this.path0(this.path.get(afterIncl), this.destination, segment -> segment.prepend(before.stream()))
                    .thenRun(() -> {
                        final int recompute = this.path.size() - before.size() - 1;
                        final double distance = this.pathAt(0).distanceTo(this.pathAt(recompute));

                        if (this.completePath) {
                            logDirect(String.format("Computed path (%.1f blocks in %.4f seconds)", distance, (System.nanoTime() - start) / 1e9d));
                        } else {
                            logDirect(String.format("Computed segment (Next %.1f blocks in %.4f seconds)", distance, (System.nanoTime() - start) / 1e9d));
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
            this.destination = null;
            this.path = Collections.emptyList();
            this.completePath = true;
            this.recalculating = false;
            this.playerNear = 0;
            this.ticksNearUnchanged = 0;
            this.maxPlayerNear = 0;
        }

        private void setPath(final UnpackedSegment segment) {
            this.path = segment.collect();
            this.removeBacktracks();
            this.completePath = segment.isFinished();
            this.playerNear = 0;
            this.ticksNearUnchanged = 0;
            this.maxPlayerNear = 0;
        }

        public List<BetterBlockPos> getPath() {
            return this.path;
        }

        public int getNear() {
            return this.playerNear;
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

            int rangeStartIncl = playerNear;
            int rangeEndExcl = playerNear;
            while (rangeEndExcl < path.size() && ctx.world().isBlockLoaded(path.get(rangeEndExcl), false)) {
                rangeEndExcl++;
            }
            if (rangeStartIncl >= rangeEndExcl) {
                // not loaded yet?
                return;
            }
            if (!passable(ctx.world().getBlockState(path.get(rangeStartIncl)), false)) {
                // we're in a wall
                return; // previous iterations of this function SHOULD have fixed this by now :rage_cat:
            }

            if (this.ticksNearUnchanged > 100) {
                this.pathRecalcSegment(rangeEndExcl - 1)
                        .thenRun(() -> {
                            logDirect("Recalculating segment, no progress in last 100 ticks");
                        });
                this.ticksNearUnchanged = 0;
                return;
            }

            for (int i = rangeStartIncl; i < rangeEndExcl - 1; i++) {
                if (!clearView(pathAt(i), pathAt(i + 1), false)) {
                    // obstacle. where do we return to pathing?
                    // find the next valid segment
                    final BetterBlockPos blockage = this.path.get(i);
                    final double distance = Math.sqrt(ctx.playerFeet().distanceSq(this.path.get(rangeEndExcl - 1)));

                    final long start = System.nanoTime();
                    this.pathRecalcSegment(rangeEndExcl - 1)
                            .thenRun(() -> {
                                logDirect(String.format("Recalculated segment around path blockage near %s %s %s (next %.1f blocks in %.4f seconds)",
                                        SettingsUtil.maybeCensor(blockage.x),
                                        SettingsUtil.maybeCensor(blockage.y),
                                        SettingsUtil.maybeCensor(blockage.z),
                                        distance,
                                        (System.nanoTime() - start) / 1e9d
                                ));
                            });
                    return;
                }
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
        if (event.isPostPopulate()) {
            final Chunk chunk = ctx.world().getChunk(event.getX(), event.getZ());
            this.context.queueForPacking(chunk);
        }
    }

    @Override
    public void onBlockChange(BlockChangeEvent event) {
        event.getAffectedChunks().stream()
                .map(pos -> ctx.world().getChunk(pos.x, pos.z))
                .forEach(this.context::queueForPacking);
    }

    @Override
    public void onReceivePacket(PacketEvent event) {
        if (event.getPacket() instanceof SPacketPlayerPosLook) {
            ctx.minecraft().addScheduledTask(() -> {
                this.remainingSetBackTicks = Baritone.settings().elytraFireworkSetbackUseDelay.value;
            });
        }
    }

    @Override
    public void pathTo(BlockPos destination) {
        this.pathManager.pathToDestination(destination);
    }

    @Override
    public void cancel() {
        this.visiblePath = Collections.emptyList();
        this.pathManager.clear();
        this.aimPos = null;
        this.remainingFireworkTicks = 0;
        this.remainingSetBackTicks = 0;
    }

    @Override
    public boolean isActive() {
        return !this.pathManager.getPath().isEmpty();
    }

    @Override
    public void onTick(TickEvent event) {
        if (event.getType() == TickEvent.Type.OUT) {
            return;
        }

        // Certified mojang employee incident
        if (this.remainingFireworkTicks > 0) {
            this.remainingFireworkTicks--;
        }
        if (this.remainingSetBackTicks > 0) {
            this.remainingSetBackTicks--;
        }

        this.clearLines.clear();
        this.blockedLines.clear();

        final List<BetterBlockPos> path = this.pathManager.getPath();
        if (path.isEmpty()) {
            return;
        }

        this.bsi = new BlockStateInterface(ctx);
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

        final Vec3d start = ctx.playerFeetAsVec();
        final boolean firework = isFireworkActive();
        BetterBlockPos goingTo = null;
        final boolean isInLava = ctx.player().isInLava();
        Solution solution = null;

        outermost:
        for (int relaxation = 0; relaxation < 3; relaxation++) { // try for a strict solution first, then relax more and more (if we're in a corner or near some blocks, it will have to relax its constraints a bit)
            int[] heights = firework ? new int[]{20, 10, 5, 0} : new int[]{0}; // attempt to gain height, if we can, so as not to waste the boost
            float[] interps = new float[] {1.0f, 0.75f, 0.5f, 0.25f};
            int steps = relaxation < 2 ? firework ? 5 : Baritone.settings().elytraSimulationTicks.value : 3;
            int lookahead = relaxation == 0 ? 2 : 3; // ideally this would be expressed as a distance in blocks, rather than a number of voxel steps
            //int minStep = Math.max(0, playerNear - relaxation);
            int minStep = playerNear;
            for (int i = Math.min(playerNear + 20, path.size() - 1); i >= minStep; i--) {
                for (int dy : heights) {
                    for (float interp : interps) {
                        Vec3d dest;
                        if (interp == 1 || i == minStep) {
                            dest = this.pathManager.pathAt(i);
                        } else {
                            dest = this.pathManager.pathAt(i).scale(interp)
                                    .add(this.pathManager.pathAt(i - 1).scale(1.0d - interp));
                        }

                        dest = dest.add(0, dy, 0);
                        if (dy != 0) {
                            if (i + lookahead >= path.size()) {
                                continue;
                            }
                            if (start.distanceTo(dest) < 40) {
                                if (!clearView(dest, this.pathManager.pathAt(i + lookahead).add(0, dy, 0), false)
                                        || !clearView(dest, this.pathManager.pathAt(i + lookahead), false)) {
                                    // aka: don't go upwards if doing so would prevent us from being able to see the next position **OR** the modified next position
                                    continue;
                                }
                            } else {
                                // but if it's far away, allow gaining altitude if we could lose it again by the time we get there
                                if (!clearView(dest, this.pathManager.pathAt(i), false)) {
                                    continue;
                                }
                            }
                        }

                        // 1.0 -> 0.25 -> none
                        final Double grow = relaxation == 2 ? null
                                : relaxation == 0 ? 1.0d : 0.25d;

                        if (isClear(start, dest, grow, isInLava)) {
                            final float yaw = RotationUtils.calcRotationFromVec3d(start, dest, ctx.playerRotations()).getYaw();

                            final Pair<Float, Boolean> pitch = this.solvePitch(dest.subtract(start), steps, relaxation, firework, isInLava);
                            if (pitch.first() == null) {
                                solution = new Solution(new Rotation(yaw, ctx.playerRotations().getPitch()), false, false);
                                continue;
                            }
                            goingTo = new BetterBlockPos(dest.x, dest.y, dest.z);
                            this.aimPos = goingTo;

                            // A solution was found with yaw AND pitch, break out of the loop to use it
                            solution = new Solution(new Rotation(yaw, pitch.first()), true, pitch.second());
                            break outermost;
                        }
                    }
                }
            }
        }

        if (solution == null) {
            logDirect("no solution");
            return;
        }

        baritone.getLookBehavior().updateTarget(solution.rotation, false);

        if (!solution.solvedPitch) {
            logDirect("no pitch solution, probably gonna crash in a few ticks LOL!!!");
            return;
        }

        this.tickUseFireworks(start, firework, goingTo, solution.forceUseFirework);
    }

    private void tickUseFireworks(final Vec3d start, final boolean firework, final BetterBlockPos goingTo, final boolean forceUseFirework) {
        if (this.remainingSetBackTicks > 0) {
            logDebug("waiting for elytraFireworkSetbackUseDelay: " + this.remainingSetBackTicks);
            return;
        }
        final boolean useOnDescend = !Baritone.settings().conserveFireworks.value || ctx.player().posY < goingTo.y + 5;
        final double currentSpeed = new Vec3d(
                ctx.player().motionX,
                // ignore y component if we are BOTH below where we want to be AND descending
                ctx.player().posY < goingTo.y ? Math.max(0, ctx.player().motionY) : ctx.player().motionY,
                ctx.player().motionZ
        ).lengthSquared();

        final double elytraFireworkSpeed = Baritone.settings().elytraFireworkSpeed.value;
        if (this.remainingFireworkTicks <= 0 && (forceUseFirework || (!firework
                && useOnDescend
                && (ctx.player().posY < goingTo.y - 5 || start.distanceTo(new Vec3d(goingTo.x + 0.5, ctx.player().posY, goingTo.z + 0.5)) > 5) // UGH!!!!!!!
                && currentSpeed < elytraFireworkSpeed * elytraFireworkSpeed))
        ) {
            // Prioritize boosting fireworks over regular ones
            // TODO: Take the minimum boost time into account?
            if (!baritone.getInventoryBehavior().throwaway(true, ElytraBehavior::isBoostingFireworks) &&
                    !baritone.getInventoryBehavior().throwaway(true, ElytraBehavior::isFireworks)) {
                logDirect("no fireworks");
                return;
            }
            logDirect("attempting to use firework" + (forceUseFirework ? " takeoff" : ""));
            ctx.playerController().processRightClick(ctx.player(), ctx.world(), EnumHand.MAIN_HAND);
            this.remainingFireworkTicks = 10;
        }
    }

    private static final class Solution {

        public final Rotation rotation;
        public final boolean solvedPitch;
        public final boolean forceUseFirework;

        public Solution(Rotation rotation, boolean solvedPitch, boolean forceUseFirework) {
            this.rotation = rotation;
            this.solvedPitch = solvedPitch;
            this.forceUseFirework = forceUseFirework;
        }
    }

    private static boolean isFireworks(final ItemStack itemStack) {
        if (itemStack.getItem() != Items.FIREWORKS) {
            return false;
        }
        // If it has NBT data, make sure it won't cause us to explode.
        final NBTTagCompound subCompound = itemStack.getSubCompound("Fireworks");
        return subCompound == null || !subCompound.hasKey("Explosions");
    }

    private static boolean isBoostingFireworks(final ItemStack itemStack) {
        final NBTTagCompound subCompound = itemStack.getSubCompound("Fireworks");
        return isFireworks(itemStack) && subCompound != null && subCompound.hasKey("Flight");
    }

    private boolean isFireworkActive() {
        return ctx.world().loadedEntityList.stream()
                .filter(x -> x instanceof EntityFireworkRocket)
                .anyMatch(x -> Objects.equals(((IEntityFireworkRocket) x).getBoostedEntity(), ctx.player()));
    }

    private boolean isClear(final Vec3d start, final Vec3d dest, final Double growAmount, boolean ignoreLava) {
        if (!clearView(start, dest, ignoreLava)) {
            return false;
        }
        if (growAmount == null) {
            return true;
        }

        final AxisAlignedBB bb = ctx.player().getEntityBoundingBox().grow(growAmount);

        final double ox = dest.x - start.x;
        final double oy = dest.y - start.y;
        final double oz = dest.z - start.z;

        final double[] src = new double[]{
                bb.minX, bb.minY, bb.minZ,
                bb.minX, bb.minY, bb.maxZ,
                bb.minX, bb.maxY, bb.minZ,
                bb.minX, bb.maxY, bb.maxZ,
                bb.maxX, bb.minY, bb.minZ,
                bb.maxX, bb.minY, bb.maxZ,
                bb.maxX, bb.maxY, bb.minZ,
                bb.maxX, bb.maxY, bb.maxZ,
        };
        final double[] dst = new double[]{
                bb.minX + ox, bb.minY + oy, bb.minZ + oz,
                bb.minX + ox, bb.minY + oy, bb.maxZ + oz,
                bb.minX + ox, bb.maxY + oy, bb.minZ + oz,
                bb.minX + ox, bb.maxY + oy, bb.maxZ + oz,
                bb.maxX + ox, bb.minY + oy, bb.minZ + oz,
                bb.maxX + ox, bb.minY + oy, bb.maxZ + oz,
                bb.maxX + ox, bb.maxY + oy, bb.minZ + oz,
                bb.maxX + ox, bb.maxY + oy, bb.maxZ + oz,
        };
        return this.context.raytrace(8, src, dst, NetherPathfinderContext.Visibility.ALL);
    }

    private boolean clearView(Vec3d start, Vec3d dest, boolean ignoreLava) {
        final boolean clear;
        if (!ignoreLava) {
            // if start == dest then the cpp raytracer dies
            clear = start.equals(dest) || this.context.raytrace(start.x, start.y, start.z, dest.x, dest.y, dest.z);
        } else {
            clear = ctx.world().rayTraceBlocks(start, dest, false, false, false) == null;
        }

        if (clear) {
            clearLines.add(new Pair<>(start, dest));
            return true;
        } else {
            blockedLines.add(new Pair<>(start, dest));
            return false;
        }
    }

    private Pair<Float, Boolean> solvePitch(Vec3d goalDirection, int steps, int relaxation, boolean currentlyBoosted, boolean ignoreLava) {
        final Float pitch = this.solvePitch(goalDirection, steps, relaxation == 2, currentlyBoosted, ignoreLava);
        if (pitch != null) {
            return new Pair<>(pitch, false);
        }

        if (Baritone.settings().experimentalTakeoff.value && relaxation > 0) {
            final Float usingFirework = this.solvePitch(goalDirection, steps, relaxation == 2, true, ignoreLava);
            if (usingFirework != null) {
                return new Pair<>(usingFirework, true);
            }
        }

        return new Pair<>(null, false);
    }

    private Float solvePitch(Vec3d goalDirection, int steps, boolean desperate, boolean firework, boolean ignoreLava) {
        // we are at a certain velocity, but we have a target velocity
        // what pitch would get us closest to our target velocity?
        // yaw is easy so we only care about pitch

        goalDirection = goalDirection.normalize();
        Rotation good = RotationUtils.calcRotationFromVec3d(new Vec3d(0, 0, 0), goalDirection, ctx.playerRotations()); // lazy lol

        Float bestPitch = null;
        double bestDot = Double.NEGATIVE_INFINITY;
        Vec3d motion = new Vec3d(ctx.player().motionX, ctx.player().motionY, ctx.player().motionZ);
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
                            if (!this.passable(x, y, z, ignoreLava)) {
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

    public boolean passable(int x, int y, int z, boolean ignoreLava) {
        return passable(this.bsi.get0(x, y, z), ignoreLava);
    }

    public static boolean passable(IBlockState state, boolean ignoreLava) {
        Material mat = state.getMaterial();
        return mat == Material.AIR || (ignoreLava && mat == Material.LAVA);
    }

    private static Vec3d step(Vec3d motion, float rotationPitch, float rotationYaw, boolean firework) {
        double motionX = motion.x;
        double motionY = motion.y;
        double motionZ = motion.z;
        float flatZ = MathHelper.cos((-rotationYaw * RotationUtils.DEG_TO_RAD_F) - (float) Math.PI);
        float flatX = MathHelper.sin((-rotationYaw * RotationUtils.DEG_TO_RAD_F) - (float) Math.PI);
        float pitchBase = -MathHelper.cos(-rotationPitch * RotationUtils.DEG_TO_RAD_F);
        float pitchHeight = MathHelper.sin(-rotationPitch * RotationUtils.DEG_TO_RAD_F);
        Vec3d lookDirection = new Vec3d(flatX * pitchBase, pitchHeight, flatZ * pitchBase);

        if (firework) {
            // See EntityFireworkRocket
            motionX += lookDirection.x * 0.1 + (lookDirection.x * 1.5 - motionX) * 0.5;
            motionY += lookDirection.y * 0.1 + (lookDirection.y * 1.5 - motionY) * 0.5;
            motionZ += lookDirection.z * 0.1 + (lookDirection.z * 1.5 - motionZ) * 0.5;
        }

        float pitchRadians = rotationPitch * RotationUtils.DEG_TO_RAD_F;
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
}

