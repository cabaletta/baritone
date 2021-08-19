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
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

/**
 * An area.
 * <p>
 * More likely than not a cuboid, but who knows :)
 */
public interface Bounds {

    @FunctionalInterface
    interface BoundsIntsConsumer {

        void consume(int x, int y, int z);
    }

    @FunctionalInterface
    interface BoundsLongConsumer {

        void consume(long pos);
    }

    @FunctionalInterface
    interface BoundsIntAndLongConsumer {

        void consume(int x, int y, int z, long pos);
    }

    void forEach(BoundsIntsConsumer consumer);

    void forEach(BoundsLongConsumer consumer);

    void forEach(BoundsIntAndLongConsumer consumer);

    // there is no "forEach" for the "index" lookup, because it is always going to just be a loop from 0 to bounds.volume()-1

    boolean inRange(int x, int y, int z);

    default boolean inRangePos(long pos) {
        return inRange(BetterBlockPos.XfromLong(pos), BetterBlockPos.YfromLong(pos), BetterBlockPos.ZfromLong(pos));
    }

    int volume();

    int toIndex(int x, int y, int z); // easy to implement for cuboid, harder for more complicated shapes

    default int toIndex(long pos) {
        return toIndex(BetterBlockPos.XfromLong(pos), BetterBlockPos.YfromLong(pos), BetterBlockPos.ZfromLong(pos));
    }

    static void sanityCheckConnectedness(Bounds bounds) {
        LongOpenHashSet all = new LongOpenHashSet();
        bounds.forEach(all::add);
        long any = all.iterator().nextLong();
        LongOpenHashSet reachable = new LongOpenHashSet();
        LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
        queue.enqueue(any);
        while (!queue.isEmpty()) {
            long pos = queue.dequeueLong();
            if (bounds.inRangePos(pos) && reachable.add(pos)) {
                for (Face face : Face.VALUES) {
                    queue.enqueueFirst(face.offset(pos));
                }
            }
        }
        LongIterator it = all.iterator();
        while (it.hasNext()) {
            if (!reachable.contains(it.nextLong())) {
                throw new IllegalStateException();
            }
        }
    }
}
