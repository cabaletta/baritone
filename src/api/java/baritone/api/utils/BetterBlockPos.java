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

import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3i;

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

    public static long longHash(int x, int y, int z) {
        // TODO use the same thing as BlockPos.fromLong();
        // invertibility would be incredibly useful
        /*
         *   This is the hashcode implementation of Vec3i (the superclass of the class which I shall not name)
         *
         *   public int hashCode() {
         *       return (this.getY() + this.getZ() * 31) * 31 + this.getX();
         *   }
         *
         *   That is terrible and has tons of collisions and makes the HashMap terribly inefficient.
         *
         *   That's why we grab out the X, Y, Z and calculate our own hashcode
         */
        long hash = 3241;
        hash = 3457689L * hash + x;
        hash = 8734625L * hash + y;
        hash = 2873465L * hash + z;
        return hash;
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
        // which calls this.offset(Direction.UP, 1)
        // which does return n == 0 ? this : new BlockPos(this.getX() + facing.getXOffset() * n, this.getY() + facing.getYOffset() * n, this.getZ() + facing.getZOffset() * n);

        // how many function calls is that? up(), up(int), offset(Direction, int), new BlockPos, getX, getXOffset, getY, getYOffset, getZ, getZOffset
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
    public BetterBlockPos offset(Direction dir) {
        Vector3i vec = dir.getDirectionVec();
        return new BetterBlockPos(x + vec.getX(), y + vec.getY(), z + vec.getZ());
    }

    @Override
    public BetterBlockPos offset(Direction dir, int dist) {
        if (dist == 0) {
            return this;
        }
        Vector3i vec = dir.getDirectionVec();
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
                "BetterBlockPos{x=%s,y=%s,z=%s}",
                SettingsUtil.maybeCensor(x),
                SettingsUtil.maybeCensor(y),
                SettingsUtil.maybeCensor(z)
        );
    }
}
