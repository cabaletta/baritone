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

package baritone.api.utils.command.datatypes;

import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.command.helpers.arguments.ArgConsumer;

import java.util.stream.Stream;

public class RelativeBlockPos implements IDatatypePost<BetterBlockPos, BetterBlockPos> {

    final RelativeCoordinate x;
    final RelativeCoordinate y;
    final RelativeCoordinate z;

    public RelativeBlockPos() {
        x = null;
        y = null;
        z = null;
    }

    public RelativeBlockPos(ArgConsumer consumer) {
        x = consumer.getDatatype(RelativeCoordinate.class);
        y = consumer.getDatatype(RelativeCoordinate.class);
        z = consumer.getDatatype(RelativeCoordinate.class);
    }

    @Override
    public BetterBlockPos apply(BetterBlockPos origin) {
        return new BetterBlockPos(
                x.apply((double) origin.x),
                y.apply((double) origin.y),
                z.apply((double) origin.z)
        );
    }

    @Override
    public Stream<String> tabComplete(ArgConsumer consumer) {
        if (consumer.has() && !consumer.has(4)) {
            while (consumer.has(2)) {
                if (consumer.peekDatatypeOrNull(RelativeCoordinate.class) == null) {
                    break;
                }
                consumer.get();
            }
            return consumer.tabCompleteDatatype(RelativeCoordinate.class);
        }
        return Stream.empty();
    }
}
