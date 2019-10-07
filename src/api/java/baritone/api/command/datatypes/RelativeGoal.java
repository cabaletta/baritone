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
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.pathing.goals.GoalXZ;
import baritone.api.pathing.goals.GoalYLevel;
import baritone.api.utils.BetterBlockPos;
import baritone.api.command.exception.CommandException;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public enum RelativeGoal implements IDatatypePost<Goal, BetterBlockPos> {
    INSTANCE;

    @Override
    public Goal apply(IDatatypeContext ctx, BetterBlockPos origin) throws CommandException {
        if (origin == null) {
            origin = BetterBlockPos.ORIGIN;
        }
        final IArgConsumer consumer = ctx.getConsumer();

        List<IDatatypePostFunction<Double, Double>> coords = new ArrayList<>();
        final IArgConsumer copy = consumer.copy(); // This is a hack and should be fixed in the future probably
        for (int i = 0; i < 3; i++) {
            if (copy.peekDatatypeOrNull(RelativeCoordinate.INSTANCE) != null) {
                coords.add(o -> consumer.getDatatypePost(RelativeCoordinate.INSTANCE, o));
                copy.get(); // Consume so we actually decrement the remaining arguments
            }
        }

        switch (coords.size()) {
            case 0:
                return new GoalBlock(origin);
            case 1:
                return new GoalYLevel(
                        MathHelper.floor(coords.get(0).apply((double) origin.y))
                );
            case 2:
                return new GoalXZ(
                        MathHelper.floor(coords.get(0).apply((double) origin.x)),
                        MathHelper.floor(coords.get(1).apply((double) origin.z))
                );
            case 3:
                return new GoalBlock(
                        MathHelper.floor(coords.get(0).apply((double) origin.x)),
                        MathHelper.floor(coords.get(1).apply((double) origin.y)),
                        MathHelper.floor(coords.get(2).apply((double) origin.z))
                );
            default:
                throw new IllegalStateException("Unexpected coords size: " + coords.size());
        }
    }

    @Override
    public Stream<String> tabComplete(IDatatypeContext ctx) {
        return ctx.getConsumer().tabCompleteDatatype(RelativeCoordinate.INSTANCE);
    }
}
