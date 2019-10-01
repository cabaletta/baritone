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

import baritone.api.pathing.goals.GoalXZ;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.command.exception.CommandException;
import baritone.api.utils.command.helpers.arguments.ArgConsumer;
import net.minecraft.util.math.MathHelper;

import java.util.stream.Stream;

public enum RelativeGoalXZ implements IDatatypePost<GoalXZ, BetterBlockPos> {
    INSTANCE;

    @Override
    public GoalXZ apply(IDatatypeContext ctx, BetterBlockPos origin) throws CommandException {
        if (origin == null) {
            origin = BetterBlockPos.ORIGIN;
        }

        final ArgConsumer consumer = ctx.getConsumer();
        return new GoalXZ(
                MathHelper.floor(consumer.getDatatypePost(RelativeCoordinate.INSTANCE, (double) origin.x)),
                MathHelper.floor(consumer.getDatatypePost(RelativeCoordinate.INSTANCE, (double) origin.y))
        );
    }

    @Override
    public Stream<String> tabComplete(IDatatypeContext ctx) {
        final ArgConsumer consumer = ctx.getConsumer();
        if (consumer.hasAtMost(2)) {
            return consumer.tabCompleteDatatype(RelativeCoordinate.INSTANCE);
        }
        return Stream.empty();
    }
}
