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

package baritone.api.utils;

import it.unimi.dsi.fastutil.HashCommon;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;

import javax.annotation.Nonnull;

/**
 * A better BlockPos that has fewer hash collisions (and slightly more performant offsets)
 * <p>
 * Is it really faster to subclass BlockPos and calculate a hash in the constructor like this, taking everything into account?
 * Yes. 20% faster actually. It's called BETTER BlockPos for a reason. Source:
 * <a href="https://docs.google.com/spreadsheets/d/1GWjOjOZINkg_0MkRgKRPH1kUzxjsnEROD9u3UFh_DJc">Benchmark Spreadsheet</a>
 *
 * @author leijurv
 */
public final class BetterBlockPos extends BlockPos {

    public static final BetterBlockPos ORIGIN = new BetterBlockPos(0, 0, 0);

    public final int x;
    public final int y;
    public final int z;

    public BetterBlockPos(int x, int y, int z) {
        super(x, y, z);
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public BetterBlockPos(double x, double y, double z) {
        this(MathHelper.floor(x), MathHelper.floor(y), MathHelper.floor(z));
    }

    public BetterBlockPos(BlockPos pos) {
        this(pos.getX(), pos.getY(), pos.getZ());
    }

    /**
     * Like constructor but returns null if pos is null, good if you just need to possibly censor coordinates
     *
     * @param pos The BlockPos, possibly null, to convert
     * @return A BetterBlockPos or null if pos was null
     */
    public static BetterBlockPos from(BlockPos pos) {
        if (pos == null) {
            return null;
        }

        return new BetterBlockPos(pos);
    }

    @Override
    public int hashCode() {
        return (int) longHash(x, y, z);
    }

    public static long longHash(BetterBlockPos pos) {
        return longHash(pos.x, pos.y, pos.z);
    }

    private static final int NUM_X_BITS = 26;
    private static final int NUM_Z_BITS = NUM_X_BITS;
    private static final int NUM_Y_BITS = 9; // note: even though Y goes from 0 to 255, that doesn't mean 8 bits will "just work" because the deserializer assumes signed. i could change it for just Y to assume unsigned and leave X and Z as signed, however, we know that in 1.17 they plan to add negative Y. for that reason, the better approach is to give the extra bits to Y and leave it as signed.
    // also, if 1.17 sticks with the current plan which is -64 to +320, we could have 9 bits for Y and a constant offset of -64 to change it to -128 to +256.
    // that would result in the packed long representation of any valid coordinate still being a positive integer
    // i like that property, so i will keep num_y_bits at 9 and plan for an offset in 1.17
    // it also gives 1 bit of wiggle room in case anything else happens in the future, so we are only using 63 out of 64 bits at the moment
    private static final int Z_SHIFT = 0;
    private static final int Y_SHIFT = Z_SHIFT + NUM_Z_BITS + 1; // 1 padding bit to make twos complement not overflow
    private static final int X_SHIFT = Y_SHIFT + NUM_Y_BITS + 1; // and also here too
    private static final long X_MASK = (1L << NUM_X_BITS) - 1L;  // X doesn't need padding as the overflow carry bit is just discarded, like a normal long (-1) + (1) = 0
    private static final long Y_MASK = (1L << NUM_Y_BITS) - 1L;
    private static final long Z_MASK = (1L << NUM_Z_BITS) - 1L;
    public static final long POST_ADDITION_MASK = X_MASK << X_SHIFT | Y_MASK << Y_SHIFT | Z_MASK << Z_SHIFT; // required to "manually inline" toLong(-1, -1, -1) here so that javac inserts proper ldc2_w instructions at usage points instead of getstatic

    static {
        if (POST_ADDITION_MASK != toLong(-1, -1, -1)) {
            throw new IllegalStateException(POST_ADDITION_MASK + " " + toLong(-1, -1, -1)); // sanity check
        }
    }

    public long toLong() {
        return toLong(this.x, this.y, this.z);
    }

    public static BetterBlockPos fromLong(long serialized) {
        return new BetterBlockPos(XfromLong(serialized), YfromLong(serialized), ZfromLong(serialized));
    }

    public static int XfromLong(long serialized) {
        return (int) (serialized << (64 - X_SHIFT - NUM_X_BITS) >> (64 - NUM_X_BITS));
    }

    public static int YfromLong(long serialized) {
        return (int) (serialized << (64 - Y_SHIFT - NUM_Y_BITS) >> (64 - NUM_Y_BITS));
    }

    public static int ZfromLong(long serialized) {
        return (int) (serialized << (64 - Z_SHIFT - NUM_Z_BITS) >> (64 - NUM_Z_BITS));
    }

    public static long toLong(final int x, final int y, final int z) {
        return ((long) x & X_MASK) << X_SHIFT | ((long) y & Y_MASK) << Y_SHIFT | ((long) z & Z_MASK) << Z_SHIFT;
    }

    public static final long HASHCODE_MURMUR_MASK = murmur64(-1);
    public static final long ZOBRIST_MURMUR_MASK = murmur64(-2);

    public static long longHash(int x, int y, int z) {
        return longHash(toLong(x, y, z));
    }

    public static long longHash(long packed) {
        return murmur64(HASHCODE_MURMUR_MASK ^ packed);
    }

    public static long zobrist(long packed) {
        return murmur64(ZOBRIST_MURMUR_MASK ^ packed);
    }

    public static long murmur64(long h) {
        return HashCommon.murmurHash3(h);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (o instanceof BetterBlockPos) {
            BetterBlockPos oth = (BetterBlockPos) o;
            return oth.x == x && oth.y == y && oth.z == z;
        }
        // during path execution, like "if (whereShouldIBe.equals(whereAmI)) {"
        // sometimes we compare a BlockPos to a BetterBlockPos
        BlockPos oth = (BlockPos) o;
        return oth.getX() == x && oth.getY() == y && oth.getZ() == z;
    }

    @Override
    public BetterBlockPos up() {
        // this is unimaginably faster than blockpos.up
        // that literally calls
        // this.up(1)
        // which calls this.offset(EnumFacing.UP, 1)
        // which does return n == 0 ? this : new BlockPos(this.getX() + facing.getXOffset() * n, this.getY() + facing.getYOffset() * n, this.getZ() + facing.getZOffset() * n);

        // how many function calls is that? up(), up(int), offset(EnumFacing, int), new BlockPos, getX, getXOffset, getY, getYOffset, getZ, getZOffset
        // that's ten.
        // this is one function call.
        return new BetterBlockPos(x, y + 1, z);
    }

    @Override
    public BetterBlockPos up(int amt) {
        // see comment in up()
        return amt == 0 ? this : new BetterBlockPos(x, y + amt, z);
    }

    @Override
    public BetterBlockPos down() {
        // see comment in up()
        return new BetterBlockPos(x, y - 1, z);
    }

    @Override
    public BetterBlockPos down(int amt) {
        // see comment in up()
        return amt == 0 ? this : new BetterBlockPos(x, y - amt, z);
    }

    @Override
    public BetterBlockPos offset(EnumFacing dir) {
        Vec3i vec = dir.getDirectionVec();
        return new BetterBlockPos(x + vec.getX(), y + vec.getY(), z + vec.getZ());
    }

    @Override
    public BetterBlockPos offset(EnumFacing dir, int dist) {
        if (dist == 0) {
            return this;
        }
        Vec3i vec = dir.getDirectionVec();
        return new BetterBlockPos(x + vec.getX() * dist, y + vec.getY() * dist, z + vec.getZ() * dist);
    }

    @Override
    public BetterBlockPos north() {
        return new BetterBlockPos(x, y, z - 1);
    }

    @Override
    public BetterBlockPos north(int amt) {
        return amt == 0 ? this : new BetterBlockPos(x, y, z - amt);
    }

    @Override
    public BetterBlockPos south() {
        return new BetterBlockPos(x, y, z + 1);
    }

    @Override
    public BetterBlockPos south(int amt) {
        return amt == 0 ? this : new BetterBlockPos(x, y, z + amt);
    }

    @Override
    public BetterBlockPos east() {
        return new BetterBlockPos(x + 1, y, z);
    }

    @Override
    public BetterBlockPos east(int amt) {
        return amt == 0 ? this : new BetterBlockPos(x + amt, y, z);
    }

    @Override
    public BetterBlockPos west() {
        return new BetterBlockPos(x - 1, y, z);
    }

    @Override
    public BetterBlockPos west(int amt) {
        return amt == 0 ? this : new BetterBlockPos(x - amt, y, z);
    }

    @Override
    @Nonnull
    public String toString() {
        return String.format(
                "BetterBlockPos{x=%d,y=%d,z=%d}",
                x,
                y,
                z
        );
    }
}
