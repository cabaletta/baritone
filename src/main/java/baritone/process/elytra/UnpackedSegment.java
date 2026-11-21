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

package baritone.process.elytra;

import baritone.api.utils.BetterBlockPos;
import baritone.process.elytra.pathfinder.PathSegment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Brady
 */
public final class UnpackedSegment {

    private final Stream<BetterBlockPos> path;
    private final boolean finished;

    public UnpackedSegment(Stream<BetterBlockPos> path, boolean finished) {
        this.path = path;
        this.finished = finished;
    }

    public UnpackedSegment append(Stream<BetterBlockPos> other, boolean otherFinished) {
        // The new segment is only finished if the one getting added on is
        return new UnpackedSegment(Stream.concat(this.path, other), otherFinished);
    }

    public UnpackedSegment prepend(Stream<BetterBlockPos> other) {
        return new UnpackedSegment(Stream.concat(other, this.path), this.finished);
    }

    public List<BetterBlockPos> collect() {
        final List<BetterBlockPos> path = this.path.collect(Collectors.toList());

        // Remove backtracks: coming back to a spot we've been means everything since then was a loop, cut it out
        // this used to remove from the list in place but leave the removed spots in the map with their old index,
        // so A B C B D C came out as A B D. the stale C -> 2 ate the real end of the path. also O(n^2) from remove(i)
        final List<BetterBlockPos> out = new ArrayList<>(path.size());
        final Map<BetterBlockPos, Integer> index = new HashMap<>();
        for (BetterBlockPos pos : path) {
            Integer j = index.get(pos);
            if (j == null) {
                index.put(pos, out.size());
                out.add(pos);
                continue;
            }
            while (out.size() > j + 1) {
                index.remove(out.remove(out.size() - 1));
            }
        }
        return out;
    }

    public boolean isFinished() {
        return this.finished;
    }

    public static UnpackedSegment from(final PathSegment segment) {
        return new UnpackedSegment(
                segment.blocks.stream().map(BetterBlockPos::new),
                segment.finished
        );
    }
}
