package baritone.api.utils.command.datatypes;

import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalYLevel;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.command.helpers.arguments.ArgConsumer;

import java.util.stream.Stream;

public class RelativeGoalYLevel implements IDatatypePost<GoalYLevel, BetterBlockPos> {
    final RelativeCoordinate coord;

    public RelativeGoalYLevel() {
        coord = null;
    }

    public RelativeGoalYLevel(ArgConsumer consumer) {
        coord = consumer.getDatatype(RelativeCoordinate.class);
    }

    @Override
    public GoalYLevel apply(BetterBlockPos origin) {
        return new GoalYLevel(coord.applyFloor(origin.y));
    }

    @Override
    public Stream<String> tabComplete(ArgConsumer consumer) {
        if (consumer.hasAtMost(1)) {
            return consumer.tabCompleteDatatype(RelativeCoordinate.class);
        }

        return Stream.empty();
    }
}
