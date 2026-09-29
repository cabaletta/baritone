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

import baritone.api.utils.BetterBlockPos;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

// the same floating line the path has always been, redone as a strip of quads facing the camera
// gl lines have hard aliased edges, can't fade across their width, and leave little gaps at every joint once they get thick
// quads can do soft edges and rounded corners, and the joints are just shared vertices
// the width is in screen pixels like the lines were. a width in blocks looks like a cable up close
// except for the frickin thingy's height, which is real blocks, see section
final class PathRibbon implements IRenderer {

    private static final float ALPHA = 0.5F;
    // pixels. the fade to nothing starts this far inside the nominal edge of the line and ends this far outside it
    private static final float FEATHER_IN = 0.5F;
    private static final float FEATHER_OUT = 1.0F;

    // the shimmer, all in blocks (and blocks per second). a bit of extra opacity drifting toward the goal, enough to tell which way the path goes
    // anything stronger than this is really distracting when you're trying to look at something else
    private static final double SHIMMER_SPACING = 14;
    private static final double SHIMMER_LENGTH = 8;
    private static final double SHIMMER_SPEED = 3.5;
    private static final float SHIMMER_STRENGTH = 0.4F;
    // how far behind the player the line takes to fade in
    private static final float TAIL_LENGTH = 2.5F;

    // how far from the middle of the lower block a step's riser goes. the face of the step is at 0.5,
    // and rounding the corner at the top swings the line 0.175 closer to it, so this is what keeps it out of the block
    private static final double RISER = 0.3;

    // the old renderer (renderPathAsLine off) drew every segment as a little rectangle standing up in the world, this tall
    // it's what gives the path a bit of body when you look at it from the side, and it's worth keeping
    private static final float THINGY_HEIGHT = 0.03F;

    // x y z relative to the camera, then the (fractional) index into the path, then arc length from the start of the path
    private static final int STRIDE = 5;
    private static final int X = 0, Y = 1, Z = 2, INDEX = 3, ARC = 4;

    // rendering is single threaded and happens every frame, so no point feeding the garbage collector
    private static float[] points = new float[STRIDE * 1024];
    private static float[] scratch = new float[STRIDE * 1024];
    private static int count;

    // the previous cross section, waiting to be stitched to the next one
    private static final float[] lastCenter = new float[3];
    private static final float[] lastSide = new float[3];
    private static float lastAlpha;
    private static float lastPlateau;
    // unit length, unlike lastSide
    private static final float[] unitSide = new float[3];

    private PathRibbon() {}

    // has to measure the same thing that load draws, or the shimmer hops every time the player walks past a step
    static double length(List<BetterBlockPos> positions, int from, int to, boolean walked) {
        double length = 0;
        for (int i = Math.max(from, 0) + 1; i <= to && i < positions.size(); i++) {
            BetterBlockPos a = positions.get(i - 1);
            BetterBlockPos b = positions.get(i);
            length += walked && isStep(a, b) ? Math.abs(b.y - a.y) + horizontal(a, b) : Math.sqrt(b.distanceSq(a));
        }
        return length;
    }

    // up or down to a neighbouring column: ascend, descend, fall, and the diagonal flavors of those
    // parkour is further than that and crosses open air anyway, pillar and downward don't go sideways at all
    private static boolean isStep(BetterBlockPos a, BetterBlockPos b) {
        int flat = (b.x - a.x) * (b.x - a.x) + (b.z - a.z) * (b.z - a.z);
        return a.y != b.y && flat > 0 && flat <= 2;
    }

    private static double horizontal(BetterBlockPos a, BetterBlockPos b) {
        return Math.sqrt((b.x - a.x) * (b.x - a.x) + (b.z - a.z) * (b.z - a.z));
    }

    // arcOffset is how much path came before positions[0], so that the shimmer carries on smoothly from current into next
    // opacity and width are multipliers on the alpha and on pathRenderLineWidthPixels, for paths that matter less
    // walked means it's a path made of movements, which get their steps drawn properly, see load
    // fadeTail is for when startIndex is the player. when it's a fork off some other line, fading in would leave a gap right where it's meant to be attached
    static void draw(PoseStack stack, List<BetterBlockPos> positions, int startIndex, Color color, boolean fadeOut, float fadeStart0, float fadeEnd0, double offset, double arcOffset, float opacity, float width, boolean animated, boolean walked, boolean fadeTail) {
        // floats, so that SearchGlow can slide the fade along smoothly. everybody else passes whole numbers
        float fadeStart = fadeStart0 + startIndex;
        float fadeEnd = fadeEnd0 + startIndex;
        int end = fadeOut ? Math.min(positions.size(), (int) Math.ceil(fadeEnd) + 2) : positions.size();
        if (end - startIndex < 2) {
            return;
        }

        // measuring from the real start of the path and not from startIndex, otherwise the shimmer
        // would jump a block backwards whenever the executor moves on to the next movement
        load(positions, startIndex, end, offset, arcOffset + length(positions, 0, startIndex, walked), walked);
        roundCorners();
        roundCorners();

        float[] rgb = color.getColorComponents(null);
        // wrapped to one cycle so that floats don't get crunchy after the game has been open for a week
        long cycleNanos = (long) (SHIMMER_SPACING / SHIMMER_SPEED * 1.0E9);
        double time = animated && settings.renderPathAnimated.value ? (double) (System.nanoTime() % cycleNanos) / cycleNanos : Double.NaN;
        float tailStart = points[ARC];

        // how many blocks wide a pixel is, one block in front of the camera. m11 is 1 / tan(fov / 2), and it already knows about sprinting and speed potions
        float blocksPerPixel = 2 / (Math.abs(RenderSystem.getProjectionMatrix().m11()) * Math.max(Minecraft.getInstance().getWindow().getHeight(), 1));
        float pixels = Math.max(settings.pathRenderLineWidthPixels.value * width, 1) / 2;
        float height = settings.renderPathAsLine.value ? 0 : THINGY_HEIGHT * width;
        unitSide[0] = 1;
        unitSide[1] = 0;
        unitSide[2] = 0;

        boolean ignoreDepth = settings.renderPathIgnoreDepth.value;
        BufferBuilder buffer = IRenderer.startQuads(ignoreDepth);
        PoseStack.Pose pose = stack.last();
        for (int i = 0; i < count; i++) {
            float alpha = 1;
            if (fadeOut) {
                alpha = 1 - (points[i * STRIDE + INDEX] - fadeStart) / (fadeEnd - fadeStart);
            }
            if (fadeTail && startIndex > 0) {
                // the bit we already walked
                alpha = Math.min(alpha, (points[i * STRIDE + ARC] - tailStart) / TAIL_LENGTH);
            }
            section(buffer, pose, i, rgb, blocksPerPixel, pixels, height, Math.max(0, Math.min(1, alpha)) * opacity, time);
        }
        IRenderer.endQuads(buffer, ignoreDepth);
    }

    private static void load(List<BetterBlockPos> positions, int start, int end, double offset, double arc, boolean walked) {
        double vpX = renderManager.renderPosX() - offset;
        double vpY = renderManager.renderPosY() - offset;
        double vpZ = renderManager.renderPosZ() - offset;
        count = 0;
        BetterBlockPos prev = null;
        for (int i = start; i < end; i++) {
            BetterBlockPos pos = positions.get(i);
            if (prev == null) {
                add(pos.x - vpX, pos.y - vpY, pos.z - vpZ, i, arc);
            } else if (walked && isStep(prev, pos)) {
                // a straight line from the bottom of a step to the top goes clean through the corner of the block you're stepping onto
                // (0.2 of a block deep, it's very visible with renderPathIgnoreDepth off)
                // so it goes across, then up or down, then across. and the up and down always happens over the lower block, since that's the one with air above it
                double t = pos.y > prev.y ? RISER : 1 - RISER;
                double riserX = prev.x + (pos.x - prev.x) * t - vpX;
                double riserZ = prev.z + (pos.z - prev.z) * t - vpZ;
                double flat = horizontal(prev, pos);
                arc = lineTo(riserX, prev.y - vpY, riserZ, i - 0.67, arc, flat * t);
                arc = lineTo(riserX, pos.y - vpY, riserZ, i - 0.33, arc, Math.abs(pos.y - prev.y));
                arc = lineTo(pos.x - vpX, pos.y - vpY, pos.z - vpZ, i, arc, flat * (1 - t));
            } else {
                arc = lineTo(pos.x - vpX, pos.y - vpY, pos.z - vpZ, i, arc, Math.sqrt(pos.distanceSq(prev)));
            }
            prev = pos;
        }
    }

    // from wherever the last point was. returns the arc length at the far end
    private static double lineTo(double x, double y, double z, double index, double arc, double dist) {
        int last = (count - 1) * STRIDE;
        float fromX = points[last + X];
        float fromY = points[last + Y];
        float fromZ = points[last + Z];
        float fromIndex = points[last + INDEX];
        // long falls and parkour (and the entire elytra path) have big gaps. the shimmer can't show up between two vertices,
        // and a long riser would get a long lazy corner that leans into the cliff it's next to
        int pieces = (int) Math.min(Math.ceil(dist / 1.5), 64);
        for (int j = 1; j < pieces; j++) {
            double t = (double) j / pieces;
            add(fromX + (x - fromX) * t, fromY + (y - fromY) * t, fromZ + (z - fromZ) * t, fromIndex + (index - fromIndex) * t, arc + dist * t);
        }
        add(x, y, z, index, arc + dist);
        return arc + dist;
    }

    private static void add(double x, double y, double z, double index, double arc) {
        if ((count + 1) * STRIDE > points.length) {
            points = Arrays.copyOf(points, points.length * 2);
        }
        int base = count++ * STRIDE;
        points[base + X] = (float) x;
        points[base + Y] = (float) y;
        points[base + Z] = (float) z;
        points[base + INDEX] = (float) index;
        points[base + ARC] = (float) arc;
    }

    // chaikin: every segment keeps its middle half, and the gaps that leaves at the corners get bridged
    // on a straight run this only adds vertices that change nothing, which conveniently is what the shimmer needs anyway
    private static void roundCorners() {
        int needed = count * 2 * STRIDE;
        if (scratch.length < needed) {
            scratch = new float[Math.max(needed, scratch.length * 2)];
        }
        int out = 0;
        System.arraycopy(points, 0, scratch, 0, STRIDE);
        out++;
        for (int i = 0; i < count - 1; i++) {
            int a = i * STRIDE;
            int b = a + STRIDE;
            for (int k = 0; k < STRIDE; k++) {
                scratch[out * STRIDE + k] = points[a + k] * 0.75F + points[b + k] * 0.25F;
                scratch[(out + 1) * STRIDE + k] = points[a + k] * 0.25F + points[b + k] * 0.75F;
            }
            out += 2;
        }
        System.arraycopy(points, (count - 1) * STRIDE, scratch, out * STRIDE, STRIDE);
        out++;

        float[] swap = points;
        points = scratch;
        scratch = swap;
        count = out;
    }

    private static void section(BufferBuilder buffer, PoseStack.Pose pose, int i, float[] rgb, float blocksPerPixel, float pixels, float height, float fade, double time) {
        int cur = i * STRIDE;
        int before = Math.max(i - 1, 0) * STRIDE;
        int after = Math.min(i + 1, count - 1) * STRIDE;
        float px = points[cur + X];
        float py = points[cur + Y] + height / 2;
        float pz = points[cur + Z];
        float tx = points[after + X] - points[before + X];
        float ty = points[after + Y] - points[before + Y];
        float tz = points[after + Z] - points[before + Z];

        // the camera is at the origin, so the position doubles as the view direction
        // sideways is whatever is perpendicular to both that and the way the path is heading
        float sx = ty * pz - tz * py;
        float sy = tz * px - tx * pz;
        float sz = tx * py - ty * px;
        float len = (float) Math.sqrt(sx * sx + sy * sy + sz * sz);
        if (len > 1.0E-5F) {
            unitSide[0] = sx / len;
            unitSide[1] = sy / len;
            unitSide[2] = sz / len;
        }
        // otherwise we're staring straight down the barrel of the path. no such thing as sideways, so keep the old one

        // depth and not distance, that's what perspective divides by. with distance the line gets fatter toward the edges of the screen
        Matrix4f view = pose.pose();
        float depth = Math.max(-(view.m02() * px + view.m12() * py + view.m22() * pz + view.m32()), 0.05F);
        float pixelSize = blocksPerPixel * depth;
        // the thingy's rectangle stands in the vertical plane of the path, so on screen it's as wide as however much of "up" points sideways from here
        // side on that's the whole height on top of the line width, from straight above (or on a riser) it's nothing and we're back to the plain line
        float wide = pixels + height / 2 * Math.abs(unitSide[1]) / pixelSize;
        float plateau = Math.max(wide - FEATHER_IN, 0) / (wide + FEATHER_OUT);
        float halfWidth = (wide + FEATHER_OUT) * pixelSize;
        sx = unitSide[0] * halfWidth;
        sy = unitSide[1] * halfWidth;
        sz = unitSide[2] * halfWidth;

        float a = fade * ALPHA * (1 + SHIMMER_STRENGTH * shimmer(points[cur + ARC], time));

        if (i > 0) {
            strip(buffer, pose, px, py, pz, sx, sy, sz, rgb, -1, -1, 0, 0, -lastPlateau, -plateau, lastAlpha, a);
            strip(buffer, pose, px, py, pz, sx, sy, sz, rgb, -lastPlateau, -plateau, lastAlpha, a, lastPlateau, plateau, lastAlpha, a);
            strip(buffer, pose, px, py, pz, sx, sy, sz, rgb, lastPlateau, plateau, lastAlpha, a, 1, 1, 0, 0);
        }

        lastCenter[0] = px;
        lastCenter[1] = py;
        lastCenter[2] = pz;
        lastSide[0] = sx;
        lastSide[1] = sy;
        lastSide[2] = sz;
        lastAlpha = a;
        lastPlateau = plateau;
    }

    // one quad between the previous cross section and this one, spanning from m1 to m2 half widths off center
    // the l ones are the previous section's, since the thingy makes every section a different shape. same for alpha
    private static void strip(BufferBuilder buffer, PoseStack.Pose pose, float px, float py, float pz, float sx, float sy, float sz, float[] rgb, float lm1, float m1, float la1, float a1, float lm2, float m2, float la2, float a2) {
        buffer.addVertex(pose, lastCenter[0] + lastSide[0] * lm1, lastCenter[1] + lastSide[1] * lm1, lastCenter[2] + lastSide[2] * lm1).setColor(rgb[0], rgb[1], rgb[2], la1);
        buffer.addVertex(pose, lastCenter[0] + lastSide[0] * lm2, lastCenter[1] + lastSide[1] * lm2, lastCenter[2] + lastSide[2] * lm2).setColor(rgb[0], rgb[1], rgb[2], la2);
        buffer.addVertex(pose, px + sx * m2, py + sy * m2, pz + sz * m2).setColor(rgb[0], rgb[1], rgb[2], a2);
        buffer.addVertex(pose, px + sx * m1, py + sy * m1, pz + sz * m1).setColor(rgb[0], rgb[1], rgb[2], a1);
    }

    // 0 most of the time, a smooth bump up to 1 where a wave is passing over this bit of path
    private static float shimmer(float arc, double time) {
        if (Double.isNaN(time)) {
            return 0;
        }
        double phase = arc / SHIMMER_SPACING - time;
        phase -= Math.floor(phase);
        double fromCenter = Math.abs(phase - 0.5) * SHIMMER_SPACING / (SHIMMER_LENGTH / 2);
        if (fromCenter >= 1) {
            return 0;
        }
        double x = 1 - fromCenter;
        return (float) (x * x * (3 - 2 * x));
    }
}
