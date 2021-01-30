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

package baritone.api.command.datatypes;

import baritone.api.command.argument.IArgConsumer;
import baritone.api.command.exception.CommandException;
import baritone.api.pathing.goals.GoalYLevel;
import baritone.api.utils.BetterBlockPos;
import net.minecraft.util.math.MathHelper;

import java.util.stream.Stream;

public enum RelativeGoalYLevel implements IDatatypePost<GoalYLevel, BetterBlockPos> {
    INSTANCE;

    @Override
    public GoalYLevel apply(IDatatypeContext ctx, BetterBlockPos origin) throws CommandException {
        if (origin == null) {
            origin = BetterBlockPos.ORIGIN;
        }

        return new GoalYLevel(
                MathHelper.floor(ctx.getConsumer().getDatatypePost(RelativeCoordinate.INSTANCE, (double) origin.y))
        );
    }

    @Override
    public Stream<String> tabComplete(IDatatypeContext ctx) {
        final IArgConsumer consumer = ctx.getConsumer();
        if (consumer.hasAtMost(1)) {
            return consumer.tabCompleteDatatype(RelativeCoordinate.INSTANCE);
        }
        return Stream.empty();
    }
}
