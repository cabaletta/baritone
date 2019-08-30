package baritone.api.utils.command.datatypes;

import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.command.helpers.arguments.ArgConsumer;

import java.util.stream.Stream;

public class RelativeGoalBlock implements IDatatypePost<GoalBlock, BetterBlockPos> {
    final RelativeCoordinate[] coords;

    public RelativeGoalBlock() {
        coords = new RelativeCoordinate[0];
    }

    public RelativeGoalBlock(ArgConsumer consumer) {
        coords = new RelativeCoordinate[] {
            consumer.getDatatype(RelativeCoordinate.class),
            consumer.getDatatype(RelativeCoordinate.class),
            consumer.getDatatype(RelativeCoordinate.class)
        };
    }

    @Override
    public GoalBlock apply(BetterBlockPos origin) {
        return new GoalBlock(
            coords[0].applyFloor(origin.x),
            coords[1].applyFloor(origin.y),
            coords[2].applyFloor(origin.z)
        );
    }

    @Override
    public Stream<String> tabComplete(ArgConsumer consumer) {
        if (consumer.hasAtMost(3)) {
            return consumer.tabCompleteDatatype(RelativeCoordinate.class);
        }

        return Stream.empty();
    }
}
