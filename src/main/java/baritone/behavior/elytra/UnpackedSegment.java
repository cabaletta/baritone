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

package baritone.behavior.elytra;

import baritone.api.utils.BetterBlockPos;
import dev.babbaj.pathfinder.PathSegment;

import java.util.Arrays;
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

    public NetherPath collect() {
        final List<BetterBlockPos> path = this.path.collect(Collectors.toList());

        // Remove backtracks
        final Map<BetterBlockPos, Integer> positionFirstSeen = new HashMap<>();
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

        return new NetherPath(path);
    }

    public boolean isFinished() {
        return this.finished;
    }

    public static UnpackedSegment from(final PathSegment segment) {
        return new UnpackedSegment(
                Arrays.stream(segment.packed).mapToObj(BetterBlockPos::deserializeFromLong),
                segment.finished
        );
    }
}
