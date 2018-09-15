/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.utils.pathing;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;

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
    public final int x;
    public final int y;
    public final int z;
    public final long hashCode;

    public BetterBlockPos(int x, int y, int z) {
        super(x, y, z);
        this.x = x;
        this.y = y;
        this.z = z;
        /*
         *   This is the hashcode implementation of Vec3i, the superclass of BlockPos
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
        this.hashCode = hash;
    }

    public BetterBlockPos(double x, double y, double z) {
        this(MathHelper.floor(x), MathHelper.floor(y), MathHelper.floor(z));
    }

    public BetterBlockPos(BlockPos pos) {
        this(pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public int hashCode() {
        return (int) hashCode;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (o instanceof BetterBlockPos) {
            BetterBlockPos oth = (BetterBlockPos) o;
            if (oth.hashCode != hashCode) {
                return false;
            }
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
        return new BetterBlockPos(x, y - amt, z);
    }

    @Override
    public BetterBlockPos offset(EnumFacing dir) {
        Vec3i vec = dir.getDirectionVec();
        return new BetterBlockPos(x + vec.getX(), y + vec.getY(), z + vec.getZ());
    }

    @Override
    public BetterBlockPos offset(EnumFacing dir, int dist) {
        Vec3i vec = dir.getDirectionVec();
        return new BetterBlockPos(x + vec.getX() * dist, y + vec.getY() * dist, z + vec.getZ() * dist);
    }
}
