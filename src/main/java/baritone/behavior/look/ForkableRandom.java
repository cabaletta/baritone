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

package baritone.behavior.look;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

/**
 * Implementation of Xoroshiro256++
 * <p>
 * Extended to produce random double-precision floating point numbers, and allow copies to be spawned via {@link #fork},
 * which share the same internal state as the source object.
 *
 * @author Brady
 */
public final class ForkableRandom {

    private static final double DOUBLE_UNIT = 0x1.0p-53;

    private final long[] s;

    public ForkableRandom() {
        this(System.nanoTime() ^ System.currentTimeMillis());
    }

    public ForkableRandom(long seedIn) {
        final AtomicLong seed = new AtomicLong(seedIn);
        final LongSupplier splitmix64 = () -> {
            long z = seed.addAndGet(0x9e3779b97f4a7c15L);
            z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
            z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
            return z ^ (z >>> 31);
        };
        this.s = new long[] {
                splitmix64.getAsLong(),
                splitmix64.getAsLong(),
                splitmix64.getAsLong(),
                splitmix64.getAsLong()
        };
    }

    private ForkableRandom(long[] s) {
        this.s = s;
    }

    public double nextDouble() {
        return (this.next() >>> 11) * DOUBLE_UNIT;
    }

    public long next() {
        final long result = rotl(this.s[0] + this.s[3], 23) + this.s[0];
        final long t = this.s[1] << 17;
        this.s[2] ^= this.s[0];
        this.s[3] ^= this.s[1];
        this.s[1] ^= this.s[2];
        this.s[0] ^= this.s[3];
        this.s[2] ^= t;
        this.s[3] = rotl(this.s[3], 45);
        return result;
    }

    public ForkableRandom fork() {
        return new ForkableRandom(Arrays.copyOf(this.s, 4));
    }

    private static long rotl(long x, int k) {
        return (x << k) | (x >>> (64 - k));
    }
}
