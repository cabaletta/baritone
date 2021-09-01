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

package baritone.builder;

import baritone.api.utils.BetterBlockPos;
import net.minecraft.util.EnumFacing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * I hate porting things to new versions of Minecraft
 * <p>
 * So just like BetterBlockPos, we now have Face
 */
public enum Face {
    DOWN, UP, NORTH, SOUTH, WEST, EAST;
    public final int index = ordinal();
    public final int oppositeIndex = opposite(index);
    public final int x = toMC().getXOffset();
    public final int y = toMC().getYOffset();
    public final int z = toMC().getZOffset();
    public final long offset = BetterBlockPos.toLong(x, y, z);
    public final int[] vec = new int[]{x, y, z};
    public final boolean vertical = y != 0;
    public final int horizontalIndex = x & 1 | (x | z) & 2;
    public static final int NUM_FACES = 6;
    public static final Face[] VALUES = new Face[NUM_FACES];
    public static final Face[] HORIZONTALS = new Face[4];
    public static final List<Optional<Face>> OPTS;
    public static final long[] OFFSETS = new long[NUM_FACES];

    static {
        List<Optional<Face>> lst = new ArrayList<>();
        for (Face face : values()) {
            VALUES[face.index] = face;
            OFFSETS[face.index] = face.offset;
            lst.add(Optional.of(face));
            HORIZONTALS[face.horizontalIndex] = face;
        }
        lst.add(Optional.empty());
        OPTS = Collections.unmodifiableList(lst);
    }

    public final EnumFacing toMC() {
        return EnumFacing.byIndex(index);
    }

    public static Face fromMC(EnumFacing facing) {
        return VALUES[facing.getIndex()];
    }

    public final Face opposite() {
        return VALUES[oppositeIndex];
    }

    public final long offset(long pos) {
        return (pos + offset) & BetterBlockPos.POST_ADDITION_MASK;
    }

    public static long offset(long pos, int face) {
        return (pos + OFFSETS[face]) & BetterBlockPos.POST_ADDITION_MASK;
    }

    public static int opposite(int face) {
        return face ^ 1;
    }

    public static int oppositeHorizontal(int horizontalIndex) {
        return horizontalIndex ^ 2;
    }
}
