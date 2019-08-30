package baritone.api.utils.command.datatypes;

import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalXZ;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.command.helpers.arguments.ArgConsumer;

import java.util.stream.Stream;

public class RelativeGoalXZ implements IDatatypePost<GoalXZ, BetterBlockPos> {
    final RelativeCoordinate[] coords;

    public RelativeGoalXZ() {
        coords = new RelativeCoordinate[0];
    }

    public RelativeGoalXZ(ArgConsumer consumer) {
        coords = new RelativeCoordinate[] {
            consumer.getDatatype(RelativeCoordinate.class),
            consumer.getDatatype(RelativeCoordinate.class)
        };
    }

    @Override
    public GoalXZ apply(BetterBlockPos origin) {
        return new GoalXZ(
            coords[0].applyFloor(origin.x),
            coords[1].applyFloor(origin.z)
        );
    }

    @Override
    public Stream<String> tabComplete(ArgConsumer consumer) {
        if (consumer.hasAtMost(2)) {
            return consumer.tabCompleteDatatype(RelativeCoordinate.class);
        }

        return Stream.empty();
    }
}
