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

package baritone;

import baritone.api.event.events.TickEvent;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.Helper;
import baritone.api.utils.Rotation;
import baritone.api.utils.RotationUtils;
import baritone.behavior.Behavior;
import baritone.utils.BlockStateInterface;
import net.minecraft.block.material.Material;
import net.minecraft.entity.item.EntityFireworkRocket;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Elytra extends Behavior implements Helper {

    public static List<BetterBlockPos> path = new ArrayList<>();

    static {

        try {
            DataInputStream in = new DataInputStream(new FileInputStream(new File("/Users/leijurv/Dropbox/nether-pathfinder/build/test")));
            int count = in.readInt();
            System.out.println("Count: " + count);
            for (int i = 0; i < count; i++) {
                path.add(new BetterBlockPos((int) in.readDouble(), (int) in.readDouble(), (int) in.readDouble()));
            }
            removeBacktracks();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public int playerNear;
    public int goingTo;
    public int sinceFirework;
    public BlockPos goal;

    protected Elytra(Baritone baritone) {
        super(baritone);
    }


    @Override
    public void onTick(TickEvent event) {
        if (event.getType() == TickEvent.Type.OUT) {
            return;
        }
        fixNearPlayer();
        baritone.getInputOverrideHandler().clearAllKeys();

        if (ctx.player().isElytraFlying()) {
            Vec3d start = ctx.playerFeetAsVec();
            boolean firework = firework();
            sinceFirework++;
            if (!firework
                    && sinceFirework > 10
                    && ctx.player().posY < path.get(goingTo).y + 5 // don't firework if trying to descend
                    && (ctx.player().posY < path.get(goingTo).y - 5 || ctx.playerFeetAsVec().distanceTo(new Vec3d(path.get(goingTo).x, ctx.player().posY, path.get(goingTo).z)) > 5) // UGH!!!!!!!
                    && new Vec3d(ctx.player().motionX, ctx.player().posY < path.get(goingTo).y ? Math.max(0, ctx.player().motionY) : ctx.player().motionY, ctx.player().motionZ).length() < Baritone.settings().elytraFireworkSpeed.value // ignore y component if we are BOTH below where we want to be AND descending
            ) {
                logDirect("firework");
                ctx.playerController().processRightClick(ctx.player(), ctx.world(), EnumHand.MAIN_HAND);
                sinceFirework = 0;
            }
            long t = System.currentTimeMillis();
            for (int relaxation = 0; relaxation < 3; relaxation++) { // try for a strict solution first, then relax more and more (if we're in a corner or near some blocks, it will have to relax its constraints a bit)
                int[] heights = firework ? new int[]{20, 10, 5, 0} : new int[]{0}; // attempt to gain height, if we can, so as not to waste the boost
                boolean requireClear = relaxation == 0;
                int steps = relaxation < 2 ? Baritone.settings().elytraSimulationTicks.value : 3;
                int lookahead = relaxation == 0 ? 2 : 3; // ideally this would be expressed as a distance in blocks, rather than a number of voxel steps
                for (int dy : heights) {
                    for (int i = Math.min(playerNear + 20, path.size()); i >= playerNear; i--) {
                        Vec3d dest = new Vec3d(path.get(i)).add(0, dy, 0);
                        if (dy != 0 && (i + lookahead >= path.size() || (!clearView(dest, new Vec3d(path.get(i + lookahead)).add(0, dy, 0)) || !clearView(dest, new Vec3d(path.get(i + lookahead)))))) {
                            // aka: don't go upwards if doing so would prevent us from being able to see the next position **OR** the modified next position
                            continue;
                        }
                        if (requireClear ? isClear(start, dest) : clearView(start, dest)) {
                            Rotation rot = RotationUtils.calcRotationFromVec3d(start, dest, ctx.playerRotations());
                            ctx.player().rotationYaw = rot.getYaw();
                            long a = System.currentTimeMillis();
                            Float pitch = solvePitch(dest.subtract(start), steps);
                            if (pitch == null) {
                                continue;
                            }
                            long b = System.currentTimeMillis();
                            ctx.player().rotationPitch = pitch;
                            System.out.println("Solved pitch in " + (b - a) + " total time " + (b - t));
                            goingTo = i;
                            goal = path.get(i).add(0, dy, 0);
                            return;
                        }
                    }
                }
            }
            logDirect("no pitch solution, probably gonna crash in a few ticks LOL!!!");
        }
    }

    private boolean firework() {
        return ctx.world().loadedEntityList.stream().anyMatch(x -> (x instanceof EntityFireworkRocket) && ((EntityFireworkRocket) x).isAttachedToEntity());
    }

    private boolean isClear(Vec3d start, Vec3d dest) {
        Vec3d perpendicular = dest.subtract(start).crossProduct(new Vec3d(0, 1, 0)).normalize();
        return clearView(start, dest)
                && clearView(start.add(0, 2, 0), dest.add(0, 2, 0))
                && clearView(start.add(0, -2, 0), dest.add(0, -2, 0))
                && clearView(start.add(perpendicular), dest.add(perpendicular))
                && clearView(start.subtract(perpendicular), dest.subtract(perpendicular));
    }

    private boolean clearView(Vec3d start, Vec3d dest) {
        RayTraceResult result = ctx.world().rayTraceBlocks(start, dest, true, false, true);
        return result == null || result.typeOfHit == RayTraceResult.Type.MISS;
    }

    private Float solvePitch(Vec3d goalDirection, int steps) {
        // we are at a certain velocity, but we have a target velocity
        // what pitch would get us closest to our target velocity?
        // yaw is easy so we only care about pitch

        goalDirection = goalDirection.normalize();
        Rotation good = RotationUtils.calcRotationFromVec3d(new Vec3d(0, 0, 0), goalDirection, ctx.playerRotations()); // lazy lol

        boolean firework = firework();
        Float bestPitch = null;
        double bestDot = Double.NEGATIVE_INFINITY;
        Vec3d motion = new Vec3d(ctx.player().motionX, ctx.player().motionY, ctx.player().motionZ);
        BlockStateInterface bsi = new BlockStateInterface(ctx);
        outer:
        for (float pitch = Math.max(good.getPitch() - Baritone.settings().elytraPitchRange.value, -89); pitch < Math.min(good.getPitch() + Baritone.settings().elytraPitchRange.value, 89); pitch++) {
            Vec3d stepped = motion;
            Vec3d totalMotion = new Vec3d(0, 0, 0);
            for (int i = 0; i < steps; i++) {
                stepped = step(stepped, pitch, good.getYaw(), firework);
                totalMotion = totalMotion.add(stepped);

                Vec3d actualPosition = ctx.playerFeetAsVec().add(totalMotion);
                if (bsi.get0(MathHelper.floor(actualPosition.x), MathHelper.floor(actualPosition.y), MathHelper.floor(actualPosition.z)).getMaterial() != Material.AIR) {
                    continue outer;
                }
                if (bsi.get0(MathHelper.floor(actualPosition.x), MathHelper.floor(actualPosition.y) + 1, MathHelper.floor(actualPosition.z)).getMaterial() != Material.AIR) {
                    continue outer;
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

    public static Vec3d step(Vec3d motion, float rotationPitch, float rotationYaw, boolean firework) {
        double motionX = motion.x;
        double motionY = motion.y;
        double motionZ = motion.z;
        float flatZ = MathHelper.cos(-rotationYaw * 0.017453292F - (float) Math.PI); // 0.174... is Math.PI / 180
        float flatX = MathHelper.sin(-rotationYaw * 0.017453292F - (float) Math.PI);
        float pitchBase = -MathHelper.cos(-rotationPitch * 0.017453292F);
        float pitchHeight = MathHelper.sin(-rotationPitch * 0.017453292F);
        Vec3d lookDirection = new Vec3d(flatX * pitchBase, pitchHeight, flatZ * pitchBase);
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
        if (firework) {
            motionX += lookDirection.x * 0.1 + (lookDirection.x * 1.5 - motionX) * 0.5;
            motionY += lookDirection.y * 0.1 + (lookDirection.y * 1.5 - motionY) * 0.5;
            motionZ += lookDirection.z * 0.1 + (lookDirection.z * 1.5 - motionZ) * 0.5;
        }
        return new Vec3d(motionX, motionY, motionZ);
    }

    public void fixNearPlayer() {
        BetterBlockPos pos = ctx.playerFeet();
        for (int i = playerNear; i >= Math.max(playerNear - 1000, 0); i -= 10) {
            if (path.get(i).distanceSq(pos) < path.get(playerNear).distanceSq(pos)) {
                playerNear = i; // intentional: this changes the bound of the loop
            }
        }
        for (int i = playerNear; i < Math.min(playerNear + 1000, path.size()); i += 10) {
            if (path.get(i).distanceSq(pos) < path.get(playerNear).distanceSq(pos)) {
                playerNear = i; // intentional: this changes the bound of the loop
            }
        }
        for (int i = playerNear; i >= Math.max(playerNear - 50, 0); i--) {
            if (path.get(i).distanceSq(pos) < path.get(playerNear).distanceSq(pos)) {
                playerNear = i; // intentional: this changes the bound of the loop
            }
        }
        for (int i = playerNear; i < Math.min(playerNear + 50, path.size()); i++) {
            if (path.get(i).distanceSq(pos) < path.get(playerNear).distanceSq(pos)) {
                playerNear = i; // intentional: this changes the bound of the loop
            }
        }
        //System.out.println(playerNear);
    }

    public static void removeBacktracks() {
        Map<BetterBlockPos, Integer> positionFirstSeen = new HashMap<>();
        for (int i = 0; i < path.size(); i++) {
            BetterBlockPos pos = path.get(i);
            if (positionFirstSeen.containsKey(pos)) {
                int j = positionFirstSeen.get(pos);
                while (i > j) {
                    path.remove(i);
                    i--;
                }
            } else {
                positionFirstSeen.put(pos, i);
            }
        }
    }
}
